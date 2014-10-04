(ns hatnik.db.memory-storage-test
  (:require [clojure.test :refer :all]
            [com.stuartsierra.component :as component]
            [hatnik.db.storage-test :as st-test]
            [hatnik.db.memory-storage :refer [map->MemoryStorage]]))

(deftest memory-storage-implements-user-storage
  (let [storage (component/start (map->MemoryStorage {}))]
    (st-test/test-user-storage storage)
    (component/stop storage)))

(deftest memory-storage-implements-project-storage
  (let [storage (component/start (map->MemoryStorage {}))]
    (st-test/test-project-storage storage)
    (component/stop storage))
)
(deftest memory-storage-implements-action-storage
  (let [storage (component/start (map->MemoryStorage {}))]
    (st-test/test-action-storage storage)
    (component/stop storage)))
