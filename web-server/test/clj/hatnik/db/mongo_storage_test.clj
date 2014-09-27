(ns hatnik.db.mongo-storage-test
  (:require [clojure.test :refer :all]
            [hatnik.db.storage-test :as st-test]
            [hatnik.db.mongo-storage :as ms]))

(def config
  {:host "localhost"
   :port 27017
   :db "test-db"
   :drop? true})

(deftest mongo-storage-implements-user-storage
  (st-test/test-user-storage (ms/create-mongo-storage config)))

(deftest mongo-storage-implements-project-storage
  (st-test/test-project-storage (ms/create-mongo-storage config)))
