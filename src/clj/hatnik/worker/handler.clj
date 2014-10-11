(ns hatnik.worker.handler
  (:require [compojure.core :refer :all]
            [taoensso.timbre :as timbre]
            [com.stuartsierra.component :as component]

            [hatnik.db.storage :as stg]
            [hatnik.utils :as utils]

            [ring.util.response :as resp]
            [ring.middleware.json :as json]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.stacktrace :as stacktrace]))

(defn test-action [db action perform-action utils]
  (timbre/info "Running test action for user" (:user action))
  (let [project (stg/get-project db (:project-id action))
        user (stg/get-user-by-id db (:user-id project))
        error (perform-action action user
                              {:library (:library action)
                               :version (:version action)
                               :previous-version (:previous-version action)
                               :project (:name project)}
                              utils)]
    (if error
      (resp/response
       {:result :error
        :message error})
      (resp/response
       {:result :ok}))))

(defn app
  "Creates routes for worker web server."
  [db perform-action utils]
  (-> (POST "/test-action" req (test-action db (:body req)
                                            perform-action utils))
      utils/wrap-exceptions
      (json/wrap-json-body {:keywords? true})
      json/wrap-json-response))


;;; Worker web server component. It provides REST API for testing actions.
;;; It should not be visible to the outside world, only inside. Used
;;; by regular web server.
(defrecord WorkerWebServer [config db perform-action utils server]

  component/Lifecycle
  (start [component]
    (let [port (-> config :worker-server :port)
          host (-> config :worker-server :host)]
      (timbre/info "Starting WorkerWebServer server on port" port
                   "host" host)
      (assoc component
        :server (run-jetty (app db perform-action utils)
                           {:port port
                            :host host
                            :join? false}))))

  (stop [component]
    (timbre/info "Stopping WorkerWebServer.")
    (when-let [server (:server component)]
      (.stop server))
    (assoc component
      :server nil)))
