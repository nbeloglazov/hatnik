(ns hatnik.test-utils
  (:require [clj-http.client :as c]
            [clojure.test :refer :all]
            [hatnik.db.memory-storage :refer [map->MemoryStorage]]
            [hatnik.db.mongo-storage :refer [map->MongoStorage]]
            [com.stuartsierra.component :as component]
            [hatnik.web.server.handler :refer [map->WebServer]]
            [taoensso.timbre :as timbre]
            [clojure.data :refer [diff]]))

(def test-web-port 6780)

(def config
  {:web {:port test-web-port}
   :enable-force-login true
   :db :memory})

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
  (assert (= (:result resp) "ok")
          (str "Expected ok response. Got: " resp))
  resp)

(defn error? [resp]
  (assert (= (:result resp) "error")
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

(defn start-test-system []
  (-> (component/system-map
       :config config
       :db (get-db)
       :web-server (component/using
                    (map->WebServer {})
                    [:db :config]))
      (component/start)))

(defn system-fixture [f]
  (let [system (start-test-system)]
    (timbre/set-level! :info)
    (try (f)
         (finally
           (component/stop system)))))

(defn cookie-store-fixture [f]
  (binding [clj-http.core/*cookie-store* (clj-http.cookies/cookie-store)]
    (f)))

(defn map-by-id [coll]
  (into {} (map #(vector (keyword (:id %)) %) coll)))

(defn assert-default-project [projects]
  (assert (= 1 (count projects)))
  (let [[id project] (first projects)]
    (assert (= (name id) (:id project)))
    (assert (= (:name project) "Default"))
    (assert (empty? (:actions project)))))

(defn data-equal [expected actual]
  (let [[left right both] (diff expected actual)]
    (assert (nil? left))
    (assert (nil? right))))

(defn login-and-check-default-project-created [email]
  (let [resp (ok? (http :get (str "/force-login?skip-dummy-data=true&email="
                                  email)))

        ; Check that default project is create for user
        projects (-> (http :get "/projects") ok? :projects)
        proj-dflt-id (-> projects first first name)
        _ (data-equal (map-by-id [{:name "Default" :id proj-dflt-id
                                   :type "regular" :actions {}}])
                      projects)]
    proj-dflt-id))
