(ns hatnik.test-utils
  (:require [clj-http.client :as c]
            [clojure.test :refer :all]
            [hatnik.db.memory-storage :refer [map->MemoryStorage]]
            [hatnik.db.mongo-storage :refer [map->MongoStorage]]))

(def test-web-port 6780)

(def api-url (str "http://localhost:" test-web-port "/api"))

(defn http [method path & [body]]
  (let [resp (->> {:form-params body
                   :content-type :json
                   :accept :json
                   :as :json
                   :method method
                   :url (str api-url path)
                   :throw-exceptions false
                   :coerce :always}
                  c/request
                  :body)]
    resp))

(defn ok? [resp]
  (is (= (:result resp) "ok")
      (str "Expected ok response. Got: " resp))
  resp)

(defn error? [resp]
  (is (= (:result resp) "error")
      (str "Expected error response. Got: " resp))
  resp)

(defn get-db
  "Returns db for tests. TEST_DB environment variable is used.
  If it is set to 'mongo' - mongo will be used. Memory otherwise.
  This needed to run all tests on travis on mongo and locally on memory
  storage."
  []
  (if (= (System/getenv "TEST_DB") "mongo")
    (map->MongoStorage {:config
                        {:host "localhost"
                         :port 27017
                         :db "test-db"
                         :drop? true}})
    (map->MemoryStorage {})))
