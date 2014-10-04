(ns hatnik.worker.handler
  (:require [compojure.core :refer :all]
            [taoensso.timbre :as timbre]
            [com.stuartsierra.component :as component]

            [hatnik.config :refer [config]]
            [hatnik.db.storage :as stg]
            [hatnik.worker.worker :as w]

            [ring.util.response :as resp]
            [ring.middleware.json :as json]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.stacktrace :as stacktrace]))

(defn test-action [db action]
  (timbre/info "Running test action for user" (:user action))
  (let [project (stg/get-project db (:project-id action))
        user (stg/get-user-by-id db (:user-id project))
        error (w/perform-action action user
                                {:library (:library action)
                                 :version (:version action)
                                 :previous-version (:previous-version action)
                                 :project (:name project)})]
    (if error
      (resp/response
       {:result :error
        :message error})
      (resp/response
       {:result :ok}))))

(defn app [db]
  (-> (POST "/test-action" req (test-action db (:body req)))
      (json/wrap-json-body {:keywords? true})
      json/wrap-json-response))

(defrecord WorkerWebServer [config db server]

  component/Lifecycle
  (start [component]
    (timbre/info "Starting WorkerWebServer server on port"
                 (:port config))
    (assoc component
      :server (run-jetty (app db) {:port (:port config)
                                   :join? false})))

  (stop [component]
    (timbre/info "Stopping WorkerWebServer.")
    (.stop server)
    (assoc component
      :server nil)))
