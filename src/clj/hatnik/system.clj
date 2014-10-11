(ns hatnik.system
  (:gen-class)
  (:require [com.stuartsierra.component :as component]
            [taoensso.timbre :as timbre]
            [hatnik.config :as conf]
            [hatnik.db.memory-storage :as mem-stg]
            [hatnik.db.mongo-storage :as mon-stg]
            [hatnik.utils :as utils]
            [hatnik.worker.worker :as worker]
            [hatnik.web.server.handler :refer [map->WebServer]]
            [hatnik.worker.handler :refer [map->WorkerWebServer]]))



(defn make-system [config]
  (let [db (case (:db config)
             :mongo (mon-stg/map->MongoStorage {:config (:mongo config)})
             :memory (mem-stg/map->MemoryStorage {}))
        send-email (partial utils/send-email (:email config))
        utils {:send-email send-email}]
    (timbre/set-level! (:log-level config))
    (when (:send-errors-to config)
      (utils/notify-about-errors-via-email config))
    (component/system-map
     :config config
     :db db
     :utils utils
     :perform-action (if (:enable-actions config)
                       worker/perform-action
                       worker/perform-action-disabled)
     :worker-web-server (component/using
                         (map->WorkerWebServer {})
                         [:db :utils :config :perform-action])
     :worker (component/using
              (worker/map->Worker {})
              [:db :utils :perform-action :config])
     :web-server (component/using
                  (map->WebServer {})
                  [:db :utils :config]))))


(def system nil)

(defn init []
    (alter-var-root #'system
                    (constantly (make-system (conf/get-config)))))

(defn start []
  (alter-var-root #'system component/start))

(defn stop []
  (try
    (alter-var-root #'system
                    (fn [s] (when s (component/stop s))))
    (catch clojure.lang.ExceptionInfo e
      (timbre/error (.getCause e)
                    "Error while stopping component "
                    (-> e ex-data :component)))))

(defn go []
  (try
    (init)
    (start)
    (catch clojure.lang.ExceptionInfo e
      (timbre/error (.getCause e)
                    "Error while starting component "
                    (-> e ex-data :component)))))

(defn restart []
  (stop)
  (go))

(defn -main [& args]
  (let [system (component/start
                (make-system (conf/get-config)))]
    (.addShutdownHook (Runtime/getRuntime)
                      (Thread. (fn []
                                 (timbre/info "Shutting down.")
                                 (component/stop system))))))

(comment

  (go)

  (stop)

  (restart)

  (init)

  )
