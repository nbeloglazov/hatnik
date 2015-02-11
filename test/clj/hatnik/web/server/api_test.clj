(ns hatnik.web.server.api-test
  (:require [clojure.test :refer :all]
            [clj-http.client :as c]
            [hatnik.versions :as ver]
            [hatnik.test-utils :refer :all]))

(use-fixtures :each system-fixture cookie-store-fixture)

(deftest test-api-valid-usage
  (let [quil-ver (-> (http :get "/library-version?name=quil&type=jvm")
                     ok? :version)
        ring-ver (-> (http :get "/library-version?name=ring&type=jvm")
                     ok? :version)
        email "me@email.com"
        proj-dflt-id (login-and-check-default-project-created email)

        ; Create project foo
        proj-foo-id (-> (http :post "/projects" {:name "Foo"
                                                 :type "regular"})
                        ok? :id)
        expected [{:name "Default" :id proj-dflt-id :actions {} :type "regular"}
                  {:name "Foo" :id proj-foo-id :actions {} :type "regular"}]
        _ (data-equal (map-by-id expected)
                      (-> (http :get "/projects") ok? :projects))

        ;
        ; Create 4 actions in Default proj and 1 action in Foo.
        ;

        ; Create first action in Default. Email action.
        act-dflt-one {:project-id proj-dflt-id
                      :type "email"
                      :subject "Subject dflt one"
                      :body "Template dflt one"
                      :library {:name "quil"
                                :type "jvm"}}
        resp (ok? (http :post "/actions" act-dflt-one))
        _ (assert (= (:last-processed-version resp) quil-ver))
        act-dflt-one (merge act-dflt-one (dissoc resp :result))

        ; Create second action in Default. Noop action.
        act-dflt-two {:project-id proj-dflt-id
                      :type "noop"
                      :library {:name "ring"
                                :type "jvm"}}
        resp (ok? (http :post "/actions" act-dflt-two))
        _ (assert (= (:last-processed-version resp) ring-ver))
        act-dflt-two (merge act-dflt-two (dissoc resp :result))

        ; Create third action in Default. GithubIssue action.
        act-dflt-three {:project-id proj-dflt-id
                        :type "github-issue"
                        :repo "quil/quil-with.dot"
                        :body "Template body"
                        :title "Template title"
                        :library {:name "ring"
                                  :type "jvm"}}
        resp (ok? (http :post "/actions" act-dflt-three))
        _ (assert (= (:last-processed-version resp) ring-ver))
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
                       :library {:name "ring"
                                 :type "jvm"}}
        resp (ok? (http :post "/actions" act-dflt-four))
        _ (assert (= (:last-processed-version resp) ring-ver))
        act-dflt-four (merge act-dflt-four (dissoc resp :result))

        ; Create single action in Foo.
        act-foo-one {:project-id proj-foo-id
                     :type "email"
                     :subject "Subject foo single"
                     :body "Template foo single"
                     :library {:name "quil"
                               :type "jvm"}}
        resp (ok? (http :post "/actions" act-foo-one))
        _ (assert (= (:last-processed-version resp) quil-ver))
        act-foo-one (merge act-foo-one (dissoc resp :result))

        ; Check that actions created correctly
        expected [{:name "Default" :id proj-dflt-id
                   :type "regular"
                   :actions (map-by-id [act-dflt-one
                                        act-dflt-two
                                        act-dflt-three
                                        act-dflt-four])}
                  {:name "Foo" :id proj-foo-id
                   :type "regular"
                   :actions (map-by-id [act-foo-one])}]
        _ (data-equal (map-by-id expected)
                      (-> (http :get "/projects") ok? :projects))

        ; Rename project "Default" to "First"
        _ (ok? (http :put (str "/projects/" proj-dflt-id) {:name "First"
                                                           :type "regular"}))

        ; Update action dflt-one. Change library and body.
        act-dflt-one (assoc act-dflt-one
                            :library {:name "ring"
                                      :type "jvm"}
                            :body "Oh my new body")
        resp (->> (dissoc act-dflt-one :id :last-processed-version)
                  (http :put (str "/actions/" (:id act-dflt-one)))
                  ok?)
        _ (data-equal resp
                      {:result "ok" :last-processed-version ring-ver})
        act-dflt-one (merge act-dflt-one (dissoc resp :result))

        ; Delete action dflt-two
        _ (ok? (http :delete (str "/actions/" (:id act-dflt-two))))

        ; Delete project "Foo"
        _ (ok? (http :delete (str "/projects/" proj-foo-id)))

        ; Check that only project "First" is present and it contains
        ; actions one and three.
        expected [{:name "First" :id proj-dflt-id
                   :type "regular"
                   :actions (map-by-id [act-dflt-one
                                        act-dflt-three
                                        act-dflt-four])}]
        _ (data-equal (map-by-id expected)
                      (-> (http :get "/projects") ok? :projects))

        ; Logout and check that we don't have access to projects.
        _ (c/get (str api-url "/logout"))
        resp (error? (http :get "/projects"))
        ; Error response should have only :result and :message keys.
        _ (assert (= #{:result :message} (set (keys resp))))

        ; Login as new user and check that we can't see previous user's
        ; project
        resp (ok? (http :get (str "/force-login?skip-dummy-data=true&"
                                  "email=new@email.com")))
        projects (-> (http :get "/projects") ok? :projects)
        proj-new-dflt-id (-> projects first first name)
        _ (data-equal (map-by-id [{:name "Default" :id proj-new-dflt-id
                                   :type "regular" :actions {}}])
                      projects)

        ; Login as old user again and verify that the project is
        ; retrieved properly.
        _ (ok? (http :get (str "/force-login?skip-dummy-data=true&email="
                               email)))
        expected [{:name "First" :id proj-dflt-id
                   :type "regular"
                   :actions (map-by-id [act-dflt-one
                                        act-dflt-three
                                        act-dflt-four])}]
        _ (data-equal (map-by-id expected)
                      (-> (http :get "/projects") ok? :projects))
        ]))

(def long-string (apply str (repeat 1000 "1111")))

(defn without-each-key [action]
  (map #(dissoc action %) (keys action)))

(defn make-invalid-email-actions [proj-id email]
  (let [valid {:project-id proj-id
               :type "email"
               :subject "Subject dflt one"
               :body "Template dflt one"
               :library {:name "quil"
                         :type "jvm"}}]
    (concat (without-each-key valid)
            (map #(merge valid %)
                 [{:library "i am string"}
                  {:library {:name "doN't exist"
                             :type "jvm"}}
                  {:library {:name "quil"
                             :type "pascal"}} ; pascal is not supported
                  {:type "unknown"}
                  {:body long-string}
                  {:subject long-string}
                  {:id "1234"}]))))

(defn make-invalid-github-issue-actions [proj-id]
  (let [valid {:project-id proj-id
               :library {:name "quil"
                         :type "jvm"}
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
               :library {:name "quil"
                         :type "jvm"}
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
  (let [quil-ver (-> (http :get "/library-version?name=quil&type=jvm")
                     ok? :version)
        ring-ver (-> (http :get "/library-version?name=ring&type=jvm")
                     ok? :version)
        email "me@email.com"
        proj-dflt-id (login-and-check-default-project-created email)

        ; Create action in Default.
        act-dflt-one {:project-id proj-dflt-id
                      :type "email"
                      :subject "Subject dflt one"
                      :body "Template dflt one"
                      :library {:name "quil"
                                :type "jvm"}}
        resp (ok? (http :post "/actions" act-dflt-one))
        _ (assert (= (:last-processed-version resp) quil-ver))
        act-dflt-one (merge act-dflt-one (dissoc resp :result))

        ; Try to create project without name, empty name, long name,
        ; invalid keys. Also try to update existing project.
        _ (doseq [proj [{}
                        {:name "" :type "regular"}
                        {:name "Valid" :type "regular" :another-key "Hey"}
                        {:name long-string :type "regular"}
                        {:name "No type"}]]
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
                   :type "regular"
                   :actions (map-by-id [act-dflt-one])}]
        _ (data-equal (-> (http :get "/projects") ok? :projects)
                      (map-by-id expected))

        ; Try to create build file action for regular project.
        ; Build file actions cannot be created by users.
        build-file-action {:project-id proj-dflt-id
                           :type "build-file"
                           :library {:name "quil"
                                     :type "jvm"}}
        _ (error? (http :post "/actions" build-file-action))
        _ (error? (http :put (str "/actions/" (:id act-dflt-one))
                        build-file-action))
        ]))

(deftest test-api-access-to-other-users
  (let [quil-ver (-> (http :get "/library-version?name=quil&type=jvm")
                     ok? :version)
        ring-ver (-> (http :get "/library-version?name=ring&type=jvm")
                     ok? :version)
        email "me@email.com"
        proj-id (login-and-check-default-project-created email)

        ; Create action in Default.
        act-base {:project-id proj-id
                  :type "email"
                  :subject "Subject dflt one"
                  :body "Template dflt one"
                  :library {:name "quil"
                            :type "jvm"}}
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
        _ (ok? (http :put (str "/projects/" proj-id) {:name "I changed your project!" :type "regular"}))
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
                                   :type "regular"
                                   :actions (map-by-id [act-full])}]))
        ]))

(defn create-github-pr-action [proj-id operations library version]
  (let [action {:project-id proj-id
                :type "github-pull-request"
                :repo "quil/quil"
                :body (str "Template body PR "
                           (rand-int 100))
                :title (str "Template title PR "
                            (rand-int 100))
                :operations operations
                :library library}
        resp (ok? (http :post "/actions" action))]
    (assert (= (:last-processed-version resp) version))
    (merge action (dissoc resp :result))))

(deftest test-api-github-pull-request-actions
  (let [quil-ver (-> (http :get "/library-version?name=quil&type=jvm")
                     ok? :version)
        ring-ver (-> (http :get "/library-version?name=ring&type=jvm")
                     ok? :version)
        email "me@email.com"

        resp (ok? (http :get (str "/force-login?skip-dummy-data=true&email="
                                  email)))

        ; Check that default project is create for user
        projects (-> (http :get "/projects") ok? :projects)
        proj-id (-> projects first first name)
        _ (data-equal projects
                      (map-by-id [{:name "Default" :id proj-id
                                   :type "regular" :actions {}}]))

        ; Create pull request action with custom operations
        act-custom (create-github-pr-action
                    proj-id
                    [{:file "project.clj"
                      :regex "hello"
                      :replacement "world"}]
                    {:name "ring"
                     :type "jvm"}
                    ring-ver)
        act-project-clj (create-github-pr-action
                         proj-id
                         "project.clj"
                         {:name "quil"
                          :type "jvm"}
                         quil-ver)
        expected [{:name "Default" :id proj-id :type "regular"
                   :actions (map-by-id [act-custom
                                        act-project-clj])}]
        _ (data-equal (-> (http :get "/projects") ok? :projects)
                      (map-by-id expected))]))
