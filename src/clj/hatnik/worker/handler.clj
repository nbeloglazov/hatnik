(ns hatnik.worker.handler
  (:require [compojure.core :refer :all]
            [taoensso.timbre :as timbre]

            [hatnik.config :refer [config]]
            [hatnik.db.storage :as stg]
            [hatnik.worker.action :refer [perform-action]]

            [ring.util.response :as resp]
            [ring.middleware.json :as json]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.stacktrace :as stacktrace]))


(defn test-action [action]
  (timbre/info "Running test action for user" (:user action))
  (let [project (stg/get-project @stg/storage (:project-id action))
        user (stg/get-user-by-id @stg/storage (:user-id project))]
    (perform-action action user {:library (:library action)
                                 :version (:version action)
                                 :previous-version (:previous-version action)
                                 :project (:name project)})
   (resp/response
    {:result :ok})))

(defroutes app-routes
  (POST "/test-action" req (test-action (:body req))))

(def app
  (-> #'app-routes
      (json/wrap-json-body {:keywords? true})
      json/wrap-json-response))

(defn start []
  (timbre/info "Starting worker HTTP server on port"
               (-> config :worker-server :port))
  (run-jetty #(app %) {:port (-> config :worker-server :port)
                       :join? false}))

(comment

  (do
     (def server (run-jetty #(app %) {:port (-> config :worker-server :port) :join? false})))

   (.stop server))
