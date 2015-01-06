(ns hatnik.worker.worker-integration-test
  (:require [clojure.test :refer :all]
            [hatnik.worker.worker :as worker]
            [hatnik.db.storage :as stg]
            [hatnik.web.server.handler :refer [map->WebServer]]
            [hatnik.worker.handler :refer [map->WorkerWebServer]]
            [hatnik.versions :as ver]
            [com.stuartsierra.component :as component]
            [hatnik.test-utils :refer :all :exclude [config]]
            [taoensso.timbre :as timbre])
  (:import [java.util.concurrent Semaphore TimeUnit]))

;;;
;;; Integration test that verifies that worker job is executed periodically.
;;; The job checks latest versions of all libraries, performs actions and
;;; then updates changed actions.
;;; We'll use REST API to create 2 users. Then we'll create an action of
;;; each type for the first user and single action for the second.
;;; Along with regular actions we'll create build-file project for the
;;; first user to check that build-file action also processed correctly.
;;;
;;; We'll also rebind 'latest-release' function so it returns new version
;;; every time it called. Thus each action will be "released" each time
;;; worker job is executed.
;;;
;;; To verify that actions are performed we'll mock send-email and
;;; create-github-issue functions and check that they're called with
;;; apropriate arguments.
;;;
;;; We'll wait until job is executed 2 times and then verify all arguments.
;;;

(def config
  {:web {:port test-web-port}
   :worker-server {:host "localhost"
                   :port 6790}
   :enable-force-login true
   :quartz {:initial-delay-in-seconds 0
            :interval-in-seconds 2
            :jobs #{:update-actions}}})

(defn file-server-fixture [f]
  (let [server (file-server "dev/build-files")]
    (try
      (f)
      (finally
        (.stop server)))))

(use-fixtures :each file-server-fixture)

(defn mock-latest-release-fn
  "Create latest-release version which returns
  new version every time it called."
  [versions]
  (fn [library]
    (str (@versions library) ".0")))

(defn create-function-mock
  "Creates mock funciton which saves all arguments it called with into
  'arguments' atom vector. Also releases a permit on provided semaphore."
  [arguments semaphore]
  (fn [& args]
    (swap! arguments conj (vec args))
    (.release semaphore)))

(defn create-build-file-project []
  (->> {:name "Build file project"
        :type "build-file"
        :build-file (str "http://localhost:" file-server-port
                         "/project.clj")
        :action {:project-id "none"
                 :library "none"
                 :type "email"
                 :subject (str "Build file subject {{library}} {{version}} "
                               "{{previous-version}} {{project}} "
                               "{{not-used}}")
                 :body (str "Build file email {{library}} {{version}} "
                            "{{previous-version}} {{project}} "
                            "{{not-used}}")}}
       (http :post "/projects")
       ok?))

(defn create-actions []
  (binding [clj-http.core/*cookie-store* (clj-http.cookies/cookie-store)]
    (let [; Login as foo@email.com and create an action of each type.
          _ (http :get "/force-login?email=foo@email.com&skip-dummy-data=true")
          proj1-id (-> (http :get "/projects") ok?
                       :projects vals first :id)
          _ (ok? (http :post "/actions"
                       {:project-id proj1-id
                        :library "lib-1"
                        :type "noop"}))
          _ (ok? (http :post "/actions"
                       {:project-id proj1-id
                        :library "lib-2"
                        :type "email"
                        :subject (str "Subject {{library}} {{version}} "
                                      "{{previous-version}} {{project}} "
                                      "{{not-used}}")
                        :body (str "Email {{library}} {{version}} "
                                   "{{previous-version}} {{project}} "
                                   "{{not-used}}")}))
          _ (ok? (http :post "/actions"
                       {:project-id proj1-id
                        :library "lib-3"
                        :type "github-issue"
                        :repo "someone/cool-repo"
                        :title (str "Title {{library}} {{version}} "
                                    "{{previous-version}} {{project}} "
                                    "{{not-used}}")
                        :body (str "Body {{library}} {{version}} "
                                   "{{previous-version}} {{project}} "
                                   "{{not-used}}")}))
          _ (create-build-file-project)

          ; Login as bar@email.com and create single email action.
          _ (http :get "/force-login?email=bar@email.com&skip-dummy-data=true")
          proj2-id (-> (http :get "/projects") ok?
                       :projects vals first :id)
          _ (ok? (http :post "/actions"
                       {:project-id proj2-id
                        :library "lib-4"
                        :type "email"
                        :subject (str "Subject bar {{library}} {{version}} "
                                      "{{previous-version}} {{project}} "
                                      "{{not-used}}")
                        :body (str "Email {{library}} {{version}} "
                                   "{{previous-version}} {{project}} "
                                   "{{not-used}}")}))])))

(defn assert-emails-args
  "Validates that send-email was called with expected args."
  [args]
  ; send-email should be called 4 times.
  (assert (= (count args) 7))

  (are [v] (some #(= % v) args)
       ; Check that email for lib-2 release was sent 2 times.
       ["foo@email.com" "Subject lib-2 3.0 2.0 Default {{not-used}}"
        "Email lib-2 3.0 2.0 Default {{not-used}}"]
       ["foo@email.com" "Subject lib-2 4.0 3.0 Default {{not-used}}"
        "Email lib-2 4.0 3.0 Default {{not-used}}"]

       ; Check that email for org.clojure/clojure release was sent 2 times.
       ["foo@email.com" "Build file subject org.clojure/clojure 2.0 1.0 Build file project {{not-used}}"
        "Build file email org.clojure/clojure 2.0 1.0 Build file project {{not-used}}"]
       ["foo@email.com" "Build file subject org.clojure/clojure 3.0 2.0 Build file project {{not-used}}"
        "Build file email org.clojure/clojure 3.0 2.0 Build file project {{not-used}}"]

       ; Check that email for quil release was sent once.
       ["foo@email.com" "Build file subject quil 3.0 1.0 Build file project {{not-used}}"
        "Build file email quil 3.0 1.0 Build file project {{not-used}}"]

       ; Check that email for lib-4 release was sent 2 times.
       ["bar@email.com" "Subject bar lib-4 5.0 4.0 Default {{not-used}}"
        "Email lib-4 5.0 4.0 Default {{not-used}}"]
       ["bar@email.com" "Subject bar lib-4 6.0 5.0 Default {{not-used}}"
        "Email lib-4 6.0 5.0 Default {{not-used}}"]))

(defn assert-github-issue-args
  "Validates that create-github-action was called with expected args."
  [args]
  ; create-github-issue should be called 2 times.
  (assert (= (count args) 2))

  (are [v] (some #(= % [v]) args)
       ; Github issue should be created for lib-3 2 times.
       {:user "someone"
        :repo "cool-repo"
        :title "Title lib-3 4.0 3.0 Default {{not-used}}"
        :body (str "Body lib-3 4.0 3.0 Default {{not-used}}"
                   "\n\nThis issue is created on behalf of @dummy-login")}
       {:user "someone"
        :repo "cool-repo"
        :title "Title lib-3 5.0 4.0 Default {{not-used}}"
        :body (str "Body lib-3 5.0 4.0 Default {{not-used}}"
                   "\n\nThis issue is created on behalf of @dummy-login")}))

(defn acquire
  "Acquires a permit from semaphore with 10s timeout."
  [semaphore permits message]
  (assert (.tryAcquire semaphore permits 10 TimeUnit/SECONDS) message))

(defn run-worker
  "Run worker jobs. The job will be run 2 times."
  [db versions]
  (reset! versions {"lib-1" 2
                    "lib-2" 3
                    "lib-3" 4
                    "lib-4" 5
                    "org.clojure/clojure" 2
                    "quil" 3
                    "ring" 1})
  (let [; send-email is called 4 times: 2 times for 2 actions.
        ; Both first and second users have email actions.
        email-args (atom [])
        email-semaphore (Semaphore. 0)
        send-email (create-function-mock email-args email-semaphore)

        ; create-github-issue is run 2 times: first user has single
        ; github-issue action.
        gi-args (atom [])
        gi-semaphore (Semaphore. 0)
        create-github-issue (create-function-mock gi-args gi-semaphore)

        utils {:send-email send-email
               :create-github-issue create-github-issue}
        worker (-> (worker/map->Worker {:config config
                                        :db db
                                        :perform-action worker/perform-action
                                        :utils utils})
                   component/start)]
    (try
      ; Wait for the first update by worker and update libraries.
      (acquire email-semaphore 4 "Email semaphore")
      (acquire gi-semaphore 1 "Github semaphore")
      (reset! versions {"lib-1" 3
                        "lib-2" 4
                        "lib-3" 5
                        "lib-4" 6
                        "org.clojure/clojure" 3
                        "quil" 3
                        "ring" 1})

      ; Wait for the second update by worker and verify send-email
      ; and create-github-issue mocks.
      (acquire email-semaphore 3 "Email semaphore")
      (acquire gi-semaphore 1 "Github semaphore")
      (assert-emails-args @email-args)
      (assert-github-issue-args @gi-args)

      (finally
        (component/stop worker)))))

(defn assert-actions-updated
  "Checks via REST API that actions actually were stored in db."
  []
  (binding [clj-http.core/*cookie-store* (clj-http.cookies/cookie-store)]
    (let [; Login as foo@email.com and get actions.
          _ (http :get "/force-login?email=foo@email.com&skip-dummy-data=true")
          foo-actions (-> (http :get "/projects") ok?
                          :projects vals first :actions vals)

          ; Login as bar@email.com and get actions.
          _ (http :get "/force-login?email=bar@email.com&skip-dummy-data=true")
          bar-actions (-> (http :get "/projects") ok?
                          :projects vals first :actions vals)]
      (assert (= (count foo-actions) 3))
      (are [library version] (some #(and (= (:library %) library)
                                         (= (:last-processed-version %) version))
                                   foo-actions)
           "lib-1" "3.0"
           "lib-2" "4.0"
           "lib-3" "5.0")

      (assert (= (count bar-actions) 1))
      (assert (and (= (:library (first bar-actions)) "lib-4")
                   (= (:last-processed-version (first bar-actions))
                      "6.0"))))))

(deftest worker-integration-test
  (let [db (component/start (get-db))
        web-server (component/start (map->WebServer {:config config
                                                     :db db}))
        versions (atom {"lib-1" 1
                        "lib-2" 2
                        "lib-3" 3
                        "lib-4" 4
                        "org.clojure/clojure" 1
                        "quil" 1
                        "ring" 1})]
    (timbre/set-level! :info)
    (try
      (with-redefs [ver/latest-release (mock-latest-release-fn versions)]
        (create-actions)
        (run-worker db versions)
        (assert-actions-updated))
      (finally
        (component/stop web-server)
        (component/stop db)))))
