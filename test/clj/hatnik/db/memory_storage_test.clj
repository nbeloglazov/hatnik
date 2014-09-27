(ns hatnik.db.memory-storage-test
  (:require [clojure.test :refer :all]
            [hatnik.db.storage-test :as st-test]
            [hatnik.db.memory-storage :as ms]))

(deftest memory-storage-implements-user-storage
  (st-test/test-user-storage (ms/create-memory-storage)))

(deftest memory-storage-implements-project-storage
  (st-test/test-project-storage (ms/create-memory-storage)))

(deftest memory-storage-implements-action-storage
  (st-test/test-action-storage (ms/create-memory-storage)))
