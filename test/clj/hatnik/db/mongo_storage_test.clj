(ns hatnik.db.mongo-storage-test
  (:require [clojure.test :refer :all]
            [com.stuartsierra.component :as component]
            [hatnik.db.storage-test :as st-test]
            [hatnik.db.mongo-storage :refer [map->MongoStorage]]))

(def config
  {:host "localhost"
   :port 27017
   :db "test-db"
   :drop? true})

(deftest mongo-storage-implements-user-storage
  (let [storage (-> {:config config}
                    map->MongoStorage
                    component/start)]
    (st-test/test-user-storage storage)
    (component/stop storage)))

(deftest mongo-storage-implements-project-storage
  (let [storage (-> {:config config}
                    map->MongoStorage
                    component/start)]
    (st-test/test-project-storage storage)
    (component/stop storage))
)
(deftest mongo-storage-implements-action-storage
  (let [storage (-> {:config config}
                    map->MongoStorage
                    component/start)]
    (st-test/test-action-storage storage)
    (component/stop storage)))
