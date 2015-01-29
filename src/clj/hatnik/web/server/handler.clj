(ns hatnik.web.server.handler
  (:require clojure.pprint
            [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [taoensso.timbre :as timbre]
            [monger.ring.session-store :refer [monger-store]]
            [monger.core :as mg]
            [com.stuartsierra.component :as component]

            [hatnik.web.server.renderer :as renderer]
            [hatnik.web.server.projects :refer [projects-api-routes]]
            [hatnik.web.server.actions :refer [actions-api-routes]]
            [hatnik.web.server.login :refer [login-api-routes]]
            [hatnik.db.storage :as stg]
            [hatnik.versions :as ver]
            [hatnik.utils :as utils]

            [ring.util.request :refer [body-string]]
            [ring.util.response :as resp]
            [ring.middleware.json :as json]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.session.memory :refer [memory-store]]
            [ring.middleware.stacktrace :as stacktrace]))

(defn library-version [library]
  (if-let [version (ver/latest-release library)]
    (resp/response
     {:result :ok
      :version version})
    (resp/response
     {:result :error
      :message (str "Library " library " not found")})))

(defn wrap-authenticated-only [handler]
  (fn [req]
    (if (-> req :session :user)
      (handler req)
      (do (timbre/debug "Unauthorized request")
          {:status 401
           :body {:result "error"
                  :message "Not authenticated"}}))))

(defn index-page [config req]
  (if (.startsWith (-> req :headers (get "host" "")) "hatnik.clojurecup.com")
    ; Redirect all requests coming for hatnik.clojurecup.com to hatnik.com
    {:status 301
     :headers {"Location" "http://hatnik.com"}
     :body ""}
    (renderer/core-page config (-> req :session :user))))

(defn app-routes [db config]
  (routes
   (GET "/" req (index-page config req))
   (context "/api" []
            (context "/projects" []
                     (wrap-authenticated-only
                      (projects-api-routes db)))
            (context "/actions" []
                     (wrap-authenticated-only
                      (actions-api-routes db config)))
            (login-api-routes db config)
            (GET "/library-version" [library] (library-version library)))
   (route/resources "/")
   ; for debugging serve files from "out" folder
   (route/files "/out" {:root "out"})
   (route/not-found "Not Found")))

(defn dump-request [handler]
  (fn [req]
    (timbre/debug "Request:" req)
    (let [resp (handler req)]
      (timbre/debug "Response:" resp)
      resp)))

(defn get-session-store [config]
  (if (= (:db config) :mongo)
    (let [{:keys [host port db drop?]} (:mongo config)
          conn (mg/connect {:host host
                            :port port})]
      (timbre/info "Using mongo session store.")
      (when drop? (mg/drop-db conn db))
      (monger-store (mg/get-db conn db) "sessions"))
    (do (timbre/info "Using memory session store.")
        (memory-store))))

(defn wrap [config routes]
  (-> routes
      dump-request
      (handler/site {:session {:cookie-attrs {; 2 weeks
                                              :max-age (* 60 60 24 14)
                                              :http-only true}
                               :store (get-session-store config)}})
      utils/wrap-exceptions
      (json/wrap-json-body {:keywords? true})
      json/wrap-json-response))

(defrecord WebServer [config db server events]

  component/Lifecycle
  (start [component]
    (let [port (-> config :web :port)]
     (timbre/info "Starting WebServer on port" port)
     (let [started (promise)
           stopped (promise)
           listener (reify org.eclipse.jetty.util.component.LifeCycle$Listener
                      (lifeCycleFailure [this event cause])
                      (lifeCycleStarted [this event]
                        (deliver started true))
                      (lifeCycleStarting [this event])
                      (lifeCycleStopped [this event]
                        (deliver stopped true))
                      (lifeCycleStopping [this event]))
           server (run-jetty (wrap config (app-routes db config))
                             {:port port :join? false
                              :configurator #(.addLifeCycleListener % listener)})]
       @started
       (assoc component
         :server server
         :events {:started started
                  :stopped stopped}))))

  (stop [component]
    (timbre/info "Stopping WebServer")
    (when-let [server (:server component)]
      (.stop server)
      @(:stopped events))
    (assoc component :server nil)))

(comment

  (def server (-> {:config {:web {:port 1333}}
                   :db nil}
                  (map->WebServer)
                  (component/start)))

  (component/stop server)

  )
