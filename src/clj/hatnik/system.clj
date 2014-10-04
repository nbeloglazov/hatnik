(ns hatnik.system
  (:require [com.stuartsierra.component :as component]
            [hatnik.config :as conf]
            [hatnik.db.memory-storage :as mem-stg]
            [hatnik.db.mongo-storage :as mon-stg]
            [hatnik.worker.handler :refer [map->WorkerWebServer]]))

(defn make-system [config]
  (let [db (case (:db config)
             :mongo (mon-stg/map->MongoStorage {:config (:mongo config)})
             :memory (mem-stg/create-memory-storage))]
   (component/system-map
    :config config
    :db db
    :worker-web-server (component/using
                        (map->WorkerWebServer {:config (:worker-server config)})
                        [:db]))))

(def system nil)

(defn init []
    (alter-var-root #'system
                    (constantly (make-system (conf/get-config)))))

(defn start []
  (alter-var-root #'system component/start))

(defn stop []
  (alter-var-root #'system
    (fn [s] (when s (component/stop s)))))

(defn go []
  (init)
  (start))

(defn restart []
  (stop)
  (go))

(comment

  (go)

  (restart)

  (init)

  )
