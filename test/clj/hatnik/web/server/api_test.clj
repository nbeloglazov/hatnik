(ns hatnik.web.server.api-test
  (:require [hatnik.web.server.handler :refer [map->WebServer]]
            [hatnik.db.memory-storage :refer [map->MemoryStorage]]
            [com.stuartsierra.component :as component]
            [clojure.test :refer :all]
            [clj-http.client :as c]
            [taoensso.timbre :as timbre]
            [hatnik.versions :as ver]
            [clojure.data :refer [diff]]
            [clj-http core cookies]))

(def config
  {:web {:port 6780}
   :enable-force-login true
   :db :memory})

(def url (str "http://localhost:" (-> config :web :port) "/api"))

(defn system-fixture [f]
  (let [system (-> (component/system-map
                    :config config
                    :db (map->MemoryStorage {})
                    :web-server (component/using
                                 (map->WebServer {})
                                 [:db :config]))
                   (component/start))]
    (timbre/set-level! :info)
    (try (f)
         (finally
           (component/stop system)))))

(defn cookie-store-fixture [f]
  (binding [clj-http.core/*cookie-store* (clj-http.cookies/cookie-store)]
    (f)))

(defn http [method path & [body]]
  (let [resp (->> {:form-params body
                   :content-type :json
                   :accept :json
                   :as :json
                   :method method
                   :url (str url path)
                   :throw-exceptions false
                   :coerce :always}
                  c/request
                  :body)]
    (timbre/spy resp)))

(use-fixtures :each system-fixture cookie-store-fixture)

(defn ok? [resp]
  (is (= (:result resp) "ok")
      "Expected ok response")
  resp)

(defn error? [resp]
  (is (= (:result resp) "error")
      "Expected error response")
  resp)

(defn map-by-id [coll]
  (into {} (map #(vector (keyword (:id %)) %) coll)))

(defn assert-default-project [projects]
  (is (= 1 (count projects)))
  (let [[id project] (first projects)]
    (is (= (name id) (:id project)))
    (is (= (:name project) "Default"))
    (is (empty? (:actions project)))))

(defn data-equal [expected actual]
  (let [[left right both] (diff expected actual)]
    (is (nil? left))
    (is (nil? right))))

(deftest test-api-valid-usage
  (let [quil-ver (-> (http :get "/library-version?library=quil")
                     ok? :version)
        ring-ver (-> (http :get "/library-version?library=ring")
                     ok? :version)
        email "me@email.com"

        resp (ok? (http :get (str "/force-login?skip-dummy-data=true&email="
                                  email)))

        ; Check that default project is create for user
        projects (-> (http :get "/projects") ok? :projects)
        proj-dflt-id (-> projects first first name)
        _ (data-equal projects
                      (map-by-id [{:name "Default" :id proj-dflt-id
                                   :actions {}}]))

        ; Create project foo
        proj-foo-id (-> (http :post "/projects" {:name "Foo"}) ok? :id)
        expected [{:name "Default" :id proj-dflt-id :actions {}}
                  {:name "Foo" :id proj-foo-id :actions {}}]
        _ (data-equal (-> (http :get "/projects") ok? :projects)
                      (map-by-id expected))

        ;
        ; Create 2 actions in Default proj and 1 action in Foo.
        ;

        ; Create first action in Default. Email action.
        act-dflt-one {:project-id proj-dflt-id
                      :type "email"
                      :address email
                      :template "Template dflt one"
                      :library "quil"}
        resp (ok? (http :post "/actions" act-dflt-one))
        _ (is (= (:last-processed-version resp) quil-ver))
        act-dflt-one (merge act-dflt-one (dissoc resp :result))

        ; Create second action in Default. Noop action.
        act-dflt-two {:project-id proj-dflt-id
                      :type "noop"
                      :library "ring"}
        resp (ok? (http :post "/actions" act-dflt-two))
        _ (is (= (:last-processed-version resp) ring-ver))
        act-dflt-two (merge act-dflt-two (dissoc resp :result))

        ; Create single action in Foo.
        act-foo-one {:project-id proj-foo-id
                     :type "email"
                     :address email
                     :template "Template foo single"
                     :library "quil"}
        resp (ok? (http :post "/actions" act-foo-one))
        _ (is (= (:last-processed-version resp) quil-ver))
        act-foo-one (merge act-foo-one (dissoc resp :result))

        ; Check that actions created correctly
        expected [{:name "Default" :id proj-dflt-id
                   :actions (map-by-id [act-dflt-one act-dflt-two])}
                  {:name "Foo" :id proj-foo-id
                   :actions (map-by-id [act-foo-one])}]
        _ (data-equal (-> (http :get "/projects") ok? :projects)
                      (map-by-id expected))

        ; Rename project "Default" to "First"
        _ (ok? (http :put (str "/projects/" proj-dflt-id) {:name "First"}))

        ; Update action dflt-one. Change library and template.
        act-dflt-one (assoc act-dflt-one
                       :library "ring"
                       :template "Oh my new template")
        resp (->> (dissoc act-dflt-one :id :last-processed-version)
                  (http :put (str "/actions/" (:id act-dflt-one)))
                  ok?)
        _ (data-equal {:result "ok" :last-processed-version ring-ver}
                      resp)
        act-dflt-one (merge act-dflt-one (dissoc resp :result))

        ; Delete action dflt-two
        _ (ok? (http :delete (str "/actions/" (:id act-dflt-two))))

        ; Delete project "Foo"
        _ (ok? (http :delete (str "/projects/" proj-foo-id)))

        ; Check that only project "First" is present and it contains
        ; single action.
        expected [{:name "First" :id proj-dflt-id
                   :actions (map-by-id [act-dflt-one])}]
        _ (data-equal (-> (http :get "/projects") ok? :projects)
                      (map-by-id expected))

        ; Logout and check that we don't have access to projects.
        _ (c/get (str url "/logout"))
        resp (error? (http :get "/projects"))
        ; Error response should have only :result and :message keys.
        _ (is (= #{:result :message} (set (keys resp))))

        ; Login as new user and check that we can't see previous user
        ; project
        resp (ok? (http :get (str "/force-login?skip-dummy-data=true&"
                                  "email=new@email.com")))
        projects (-> (http :get "/projects") ok? :projects)
        proj-new-dflt-id (-> projects first first name)
        _ (data-equal projects
                      (map-by-id [{:name "Default" :id proj-new-dflt-id
                                   :actions {}}]))

        ; Login as old user again and verify that the project is
        ; retrieved properly.
        _ (ok? (http :get (str "/force-login?skip-dummy-data=true&email="                                  email)))
        expected [{:name "First" :id proj-dflt-id
                   :actions (map-by-id [act-dflt-one])}]
        _ (data-equal (-> (http :get "/projects") ok? :projects)
                      (map-by-id expected))
        ]))

(deftest test-api-invalid-requests
  (let [quil-ver (-> (http :get "/library-version?library=quil")
                     ok? :version)
        ring-ver (-> (http :get "/library-version?library=ring")
                     ok? :version)
        email "me@email.com"

        resp (ok? (http :get (str "/force-login?skip-dummy-data=true&email="
                                  email)))

        ; Check that default project is create for user
        projects (-> (http :get "/projects") ok? :projects)
        proj-dflt-id (-> projects first first name)
        _ (data-equal projects
                      (map-by-id [{:name "Default" :id proj-dflt-id
                                   :actions {}}]))

        ; Create action in Default.
        act-dflt-one {:project-id proj-dflt-id
                      :type "email"
                      :address email
                      :template "Template dflt one"
                      :library "quil"}
        resp (ok? (http :post "/actions" act-dflt-one))
        _ (is (= (:last-processed-version resp) quil-ver))
        act-dflt-one (merge act-dflt-one (dissoc resp :result))

        long-string (apply str (repeat 1000 "1111"))
        ; Try to create project without name, empty name, long name,
        ; invalid keys. Also try to update existing project.
        _ (doseq [proj [{}
                        {:name ""}
                        {:name "Valid" :another-key "Hey"}
                        {:name long-string}]]
            (error? (http :post "/projects" proj))
            (error? (http :put (str "/projects/" proj-dflt-id) proj)))

        ; Try to create invalid actions or update existing action to be
        ; invalid.
        valid-act (dissoc act-dflt-one
                          :id :last-processed-version)
        _ (doseq [action [(dissoc valid-act :template)
                          (assoc valid-act :library "iDontExist")
                          (assoc valid-act :type "unknown")
                          (assoc valid-act :template long-string)
                          (assoc valid-act :id "1233")]]
            (error? (http :post "/actions" action))
            (error? (http :put (str "/actions/" (:id act-dflt-one)) action)))

        ; Check that the project and the action weren't modified.
        expected [{:name "Default" :id proj-dflt-id
                   :actions (map-by-id [act-dflt-one])}]
        _ (data-equal (-> (http :get "/projects") ok? :projects)
                      (map-by-id expected))]))

(deftest test-api-access-to-other-users
  (let [quil-ver (-> (http :get "/library-version?library=quil")
                     ok? :version)
        ring-ver (-> (http :get "/library-version?library=ring")
                     ok? :version)
        email "me@email.com"

        _ (ok? (http :get (str "/force-login?skip-dummy-data=true&email="
                               email)))

        ; Check that default project is create for user
        proj-id (-> (http :get "/projects") ok? :projects first first name)

        ; Create action in Default.
        act-base {:project-id proj-id
                           :type "email"
                           :address email
                           :template "Template dflt one"
                           :library "quil"}
        resp (ok? (http :post "/actions" act-base))
        act-full (merge act-base (dissoc resp :result))

        ; Currently we return "ok" result even if you tried to access
        ; others projects or actions. But these actions should not be
        ; modified in fact. Now we'll try to change projects/actions of
        ; the first user while we're logged in as another user.

        ; Login as new user
        _ (ok? (http :get (str "/force-login?skip-dummy-data=true&"
                               "email=another@email.com")))

        ; Modify project
        _ (ok? (http :put (str "/projects/" proj-id) {:name "I changed your project!"}))
        ; Modify action
        _ (ok? (http :put (str "/actions/" (:id act-full))
                     (assoc act-base :template "I changed your action!")))
        ; Create action. API is not consistent actually, in this case
        ; it return error if action was not created.
        _ (error? (http :post "/actions" act-base))
        ; Delete action
        _ (ok? (http :delete (str "/actions/" (:id act-full))))
        ; Delete project
        _ (ok? (http :delete (str "/projects/" proj-id)))

        ; Now relogin as first user and verify that nothing has changed.
        _ (ok? (http :get (str "/force-login?skip-dummy-data=true&email="
                               email)))
        projects (-> (http :get "/projects") ok? :projects)
        _ (data-equal projects
                      (map-by-id [{:name "Default" :id proj-id
                                   :actions (map-by-id [act-full])}]))
        ]))

