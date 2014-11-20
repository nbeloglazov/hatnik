(ns hatnik.web.server.api-test
  (:require [hatnik.web.server.handler :refer [map->WebServer]]
            [com.stuartsierra.component :as component]
            [clojure.test :refer :all]
            [clj-http.client :as c]
            [taoensso.timbre :as timbre]
            [hatnik.versions :as ver]
            [hatnik.test-utils :refer :all]
            [clojure.data :refer [diff]]))

(def config
  {:web {:port test-web-port}
   :enable-force-login true
   :db :memory})

(defn system-fixture [f]
  (let [system (-> (component/system-map
                    :config config
                    :db (get-db)
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

(use-fixtures :each system-fixture cookie-store-fixture)

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
        ; Create 4 actions in Default proj and 1 action in Foo.
        ;

        ; Create first action in Default. Email action.
        act-dflt-one {:project-id proj-dflt-id
                      :type "email"
                      :subject "Subject dflt one"
                      :body "Template dflt one"
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

        ; Create third action in Default. GithubIssue action.
        act-dflt-three {:project-id proj-dflt-id
                        :type "github-issue"
                        :repo "quil/quil"
                        :body "Template body"
                        :title "Template title"
                        :library "ring"}
        resp (ok? (http :post "/actions" act-dflt-three))
        _ (is (= (:last-processed-version resp) ring-ver))
        act-dflt-three (merge act-dflt-three (dissoc resp :result))

        ; Create fourth action in Default. GithubPullRequest action.
        act-dflt-four {:project-id proj-dflt-id
                       :type "github-pull-request"
                       :repo "quil/quil"
                       :body "Template body PR"
                       :title "Template title PR"
                       :operations [{:file "project.clj"
                                     :regex "hello"
                                     :replacement "world"}]
                       :library "ring"}
        resp (ok? (http :post "/actions" act-dflt-four))
        _ (is (= (:last-processed-version resp) ring-ver))
        act-dflt-four (merge act-dflt-four (dissoc resp :result))

        ; Create single action in Foo.
        act-foo-one {:project-id proj-foo-id
                     :type "email"
                     :subject "Subject foo single"
                     :body "Template foo single"
                     :library "quil"}
        resp (ok? (http :post "/actions" act-foo-one))
        _ (is (= (:last-processed-version resp) quil-ver))
        act-foo-one (merge act-foo-one (dissoc resp :result))

        ; Check that actions created correctly
        expected [{:name "Default" :id proj-dflt-id
                   :actions (map-by-id [act-dflt-one
                                        act-dflt-two
                                        act-dflt-three
                                        act-dflt-four])}
                  {:name "Foo" :id proj-foo-id
                   :actions (map-by-id [act-foo-one])}]
        _ (data-equal (-> (http :get "/projects") ok? :projects)
                      (map-by-id expected))

        ; Rename project "Default" to "First"
        _ (ok? (http :put (str "/projects/" proj-dflt-id) {:name "First"}))

        ; Update action dflt-one. Change library and body.
        act-dflt-one (assoc act-dflt-one
                       :library "ring"
                       :body "Oh my new body")
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
        ; actions one and three.
        expected [{:name "First" :id proj-dflt-id
                   :actions (map-by-id [act-dflt-one
                                        act-dflt-three
                                        act-dflt-four])}]
        _ (data-equal (-> (http :get "/projects") ok? :projects)
                      (map-by-id expected))

        ; Logout and check that we don't have access to projects.
        _ (c/get (str api-url "/logout"))
        resp (error? (http :get "/projects"))
        ; Error response should have only :result and :message keys.
        _ (is (= #{:result :message} (set (keys resp))))

        ; Login as new user and check that we can't see previous user's
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
        _ (ok? (http :get (str "/force-login?skip-dummy-data=true&email="
                               email)))
        expected [{:name "First" :id proj-dflt-id
                   :actions (map-by-id [act-dflt-one
                                        act-dflt-three
                                        act-dflt-four])}]
        _ (data-equal (-> (http :get "/projects") ok? :projects)
                      (map-by-id expected))
        ]))

(def long-string (apply str (repeat 1000 "1111")))

(defn without-each-key [action]
  (map #(dissoc action %) (keys action)))

(defn make-invalid-email-actions [proj-id email]
  (let [valid {:project-id proj-id
               :type "email"
               :subject "Subject dflt one"
               :body "Template dflt one"
               :library "quil"}]
    (concat (without-each-key valid)
            (map #(merge valid %)
                 [{:library "iDontExist"}
                  {:type "unknown"}
                  {:body long-string}
                  {:subject long-string}
                  {:id "1234"}]))))

(defn make-invalid-github-issue-actions [proj-id]
  (let [valid {:project-id proj-id
               :library "quil"
               :type "github-issue"
               :repo "nbeloglazov/hatnik"
               :title "Hello {{library}}"
               :body "Hello"}]
    (concat (without-each-key valid)
            (map #(merge valid %)
                 [{:repo "invalid$repo%1"}
                  {:title long-string}
                  {:body long-string}]))))

(defn make-invalid-github-pull-request-actions [proj-id]
  (let [valid {:project-id proj-id
               :library "quil"
               :type "github-pull-request"
               :repo "nbeloglazov/hatnik"
               :title "Hello {{library}}"
               :body "Hello"
               :operations [{:file "some/file"
                             :regex "regex"
                             :replacement "replacement"}]}]
    (concat (without-each-key valid)
            (map #(merge valid %)
                 [{:repo "invalid$repo%1"}
                  {:title long-string}
                  {:body long-string}
                  {:operations [{:regex "regex"
                                 :replacement "repl"}]}
                  {:operations [{:file "some/file"
                                 :replacement "repl"}]}
                  {:operations [{:file long-string
                                 :regex long-string
                                 :replacement long-string}]}]))))

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
                      :subject "Subject dflt one"
                      :body "Template dflt one"
                      :library "quil"}
        resp (ok? (http :post "/actions" act-dflt-one))
        _ (is (= (:last-processed-version resp) quil-ver))
        act-dflt-one (merge act-dflt-one (dissoc resp :result))

        ; Try to create project without name, empty name, long name,
        ; invalid keys. Also try to update existing project.
        _ (doseq [proj [{}
                        {:name ""}
                        {:name "Valid" :another-key "Hey"}
                        {:name long-string}]]
            (error? (http :post "/projects" proj))
            (error? (http :put (str "/projects/" proj-dflt-id) proj)))

        ; Try to create invalid actions.
        _ (doseq [action (concat
                          (make-invalid-email-actions proj-dflt-id email)
                          (make-invalid-github-issue-actions proj-dflt-id)
                          (make-invalid-github-pull-request-actions proj-dflt-id))]
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

        ; Check that default project was created
        proj-id (-> (http :get "/projects") ok? :projects first first name)

        ; Create action in Default.
        act-base {:project-id proj-id
                  :type "email"
                  :subject "Subject dflt one"
                  :body "Template dflt one"
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
                     (assoc act-base :body "I changed your action!")))
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

