(ns hatnik.web.server.handler
  (:require clojure.pprint
            [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [taoensso.timbre :as timbre]

            [hatnik.web.server.renderer :as renderer]
            [hatnik.web.server.projects :refer [projects-api]]
            [hatnik.web.server.actions :refer [actions-api]]
            [hatnik.web.server.login :refer [login-api]]
            [hatnik.config :refer [config]]
            [hatnik.db.storage :as stg]
            [hatnik.db.memory-storage :refer [create-memory-storage]]
            [hatnik.db.mongo-storage :refer [create-mongo-storage]]
            [hatnik.versions :as ver]
            [hatnik.worker.handler :as worker-handler]
            [hatnik.worker.worker :as worker]

            [ring.util.request :refer [body-string]]
            [ring.util.response :as resp]
            [ring.middleware.json :as json]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.stacktrace :as stacktrace]))

(defn initialise []
  (timbre/info "Initialisation started.")
  (worker-handler/start)
  (worker/initialise-and-start-job)
  (timbre/set-level! (:log-level config))
  (reset! stg/storage
          (case (:db config)
            :memory (do
                      (timbre/info "Using memory db")
                      (create-memory-storage))
            :mongo (do
                     (timbre/info "Using mongo db"
                                  (select-keys (:mongo config)
                                               [:host :port :db :drop?]))
                     (create-mongo-storage (:mongo config)))))
  (timbre/info "Initialisation finished."))

(defn library-version [library]
  (resp/response
   {:result :ok
    :version (ver/latest-release library)}))

(defn wrap-authenticated-only [handler]
  (fn [req]
    (if (-> req :session :user)
      (handler req)
      (do (timbre/debug "Unauthorized request")
          {:status 401
           :body {:result "error"
                  :message "Not authenticated"}}))))

(defroutes app-routes
  (GET "/" [] renderer/core-page)
  (context "/api" []
           (context "/projects" []
                    (wrap-authenticated-only projects-api))
           (context "/actions" []
                    (wrap-authenticated-only actions-api))
           login-api
           (GET "/library-version" [library] (library-version library)))
  (route/resources "/")
  (route/not-found "Not Found"))

(defn dump-request [handler]
  (fn [req]
    (timbre/debug "Request:" req)
    (let [resp (handler req)]
      (timbre/debug "Response:" resp)
      resp)))

(def app
  (-> #'app-routes
      dump-request
      (handler/site {:session {:cookie-attrs {:max-age 3600
                                              :http-only true}}})
      (json/wrap-json-body {:keywords? true})
      json/wrap-json-response
      stacktrace/wrap-stacktrace))

(comment

   (do
     (def server (run-jetty #(app %) {:port 8080 :join? false}))
     (initialise))

   (.stop server)
)
