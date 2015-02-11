(ns hatnik.web.server.build-file-project-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [clj-http.client :as c]
            [hatnik.versions :as ver]
            [hatnik.test-utils :refer :all]
            [hatnik.web.server.build-files :as bf]))

(defn file-server-fixture [f]
  (let [server (file-server "dev/build-files")]
    (try
      (f)
      (finally
        (.stop server)
        ; Hack to make sure server stopped and port is free.
        ; Ideally we should select free port randomly for each
        ; test but it requires some refactoring.
        (Thread/sleep 2000)))))

(use-fixtures :each system-fixture cookie-store-fixture file-server-fixture)

(defn delete-project [id]
  (-> (http :delete (str "/projects/" id))
      ok?))

(deftest build-file-project-test
  (let [proj-dflt-id (login-and-check-default-project-created "me@email.com")
        _ (delete-project proj-dflt-id)

        ; Create project based on dev/build-files/project.clj file.
        ; Uses noop action for all libraries.
        project {:name "Build file project"
                 :type "build-file"
                 :build-file (str "http://localhost:" file-server-port
                                  "/project.clj")
                 :action {:library {:name "none"
                                    :type "jvm"}
                          :project-id "none"
                          :type "noop"}}

        resp (http :post "/projects" project)
        _ (ok? resp)
        proj-id (:id resp)
        actions (:actions resp)
        assert-actions-match (fn [actions expected-libs proj-id]
                               (let [no-ids (->> actions
                                                 vals
                                                 (map #(dissoc % :id))
                                                 doall)]
                                 (doseq [lib expected-libs
                                         :let [expected {:library lib
                                                         :last-processed-version (ver/latest-release lib)
                                                         :type "build-file"
                                                         :project-id proj-id}]]
                                   (assert (some #(= % expected) no-ids)))))

        ; Check that actions are present in response
        ; and also try to get all projects and check that it is correct.
        _ (assert-actions-match actions
                                [{:name "org.clojure/clojure"
                                  :type "jvm"}
                                 {:name "quil"
                                  :type "jvm"}
                                 {:name "ring"
                                  :type "jvm"}]
                                proj-id)

        ; Checking all projects
        _ (data-equal (map-by-id [(assoc project
                                    :id proj-id
                                    :actions actions)])
                      (-> (http :get "/projects") ok? :projects))

        ; Change project by modifying its build-file to use nbeloglazov/hatnik-test-lib project.clj
        ; and use email new action.
        project {:name "Updated project"
                 :type "build-file"
                 :build-file "nbeloglazov/hatnik-test-lib"
                 :action {:library {:name "none"
                                    :type "jvm"}
                          :project-id "none"
                          :type "email"
                          :subject "Hello"
                          :body "World"}}
        actions (-> (http :put (str "/projects/" proj-id) project) ok? :actions)

        ; Check that new updated project has only 2 actions now: clojure and clojurescript.
        ; To verify it is correct check project.clj from nbeloglazov/hatnik-test-lib
        _ (assert-actions-match actions
                                [{:name "org.clojure/clojure"
                                  :type "jvm"}
                                 {:name "org.clojure/clojurescript"
                                  :type "jvm"}]
                                proj-id)]))

(deftest changing-project-type-test
  (let [proj-dflt-id (login-and-check-default-project-created "me@email.com")
        project {:name "Build file project"
                 :type "build-file"
                 :build-file (str "http://localhost:" file-server-port
                                  "/project.clj")
                 :action {:library {:name "none"
                                    :type "jvm"}
                          :project-id "none"
                          :type "noop"}}
        proj-bf-id (-> (http :post "/projects" project) ok? :id)

        ; Now we have 2 projects. One is regular and another on is build-file.
        ; Try to change type of each and make sure we get an error.

        _ (error? (http :put (str "/projects/" proj-dflt-id) project))
        _ (error? (http :put (str "/projects/" proj-bf-id) {:name "Hello"
                                                            :type "regular"}))

        ; Verify that projects weren't change.
        projects (-> (http :get "/projects") ok? :projects)
        proj-eq (fn [id expected]
                  (-> (keyword id)
                      (projects)
                      (select-keys [:name :type])
                      (= expected)
                      (assert)))
        _ (proj-eq proj-dflt-id {:name "Default" :type "regular"})
        _ (proj-eq proj-bf-id {:name "Build file project" :type "build-file"})]))

(deftest complex-project-clj-parse-test
  (let [expected-libs (map (fn [name]
                             {:name name
                              :type "jvm"})
                           ["org.clojure/clojure" "quil"
                            "net.sf.ehcache/ehcache" "log4j" "org.lwjgl.lwjgl/lwjgl"
                            "org.lwjgl.lwjgl/lwjgl-platform" "lein-pprint" "lein-assoc"
                            "s3-wagon-private" "clj-stacktrace" "cider/cider-nrepl"])]
    (data-equal (map (fn [lib]
                       {:library lib
                        :last-processed-version (ver/latest-release lib)
                        :type "build-file"})
                     expected-libs)
                (bf/actions-from-build-file (str "http://localhost:" file-server-port
                                                 "/complex.project.clj")))))

(deftest invalid-build-file-test
  (login-and-check-default-project-created "me@email.com")
  (let [project {:name "Build file project"
                 :type "build-file"
                 :action {:library {:name "none"
                                    :type "jvm"}
                          :project-id "none"
                          :type "noop"}}
        invalid-build-files [(str (.toURI (io/file "dev/build-files/project.clj")))
                             "http://example.com/project.clj"
                             "nbeloglazov/not-existing-repo"]]
    (doseq [invalid-file invalid-build-files]
      (->> (assoc project :build-file invalid-file)
           (http :post "/projects")
           error?))))

(comment

 ((join-fixtures [system-fixture cookie-store-fixture file-server-fixture])
  complex-project-clj-parse-test)

 )
