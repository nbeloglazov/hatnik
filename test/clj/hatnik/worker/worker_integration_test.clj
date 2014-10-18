(ns hatnik.worker.worker-integration-test
  (:require [clojure.test :refer :all]
            [hatnik.worker.worker :as worker]
            [hatnik.db.storage :as stg]
            [hatnik.web.server.handler :refer [map->WebServer]]
            [hatnik.worker.handler :refer [map->WorkerWebServer]]
            [hatnik.versions :as ver]
            [com.stuartsierra.component :as component]
            [hatnik.test-utils :refer :all]
            [taoensso.timbre :as timbre]))

;;;
;;; Integration test that verifies that worker job is executed periodically.
;;; The job checks latest versions of all libraries, performs actions and
;;; then updates changed actions.
;;; We'll use REST API to create 2 users. Then we'll create an action of
;;; each type for the first user and single action for the second.
;;; We'll also rebind 'latest-release' function so it returns new version
;;; every time it called. Thus each action will be "released" each time
;;; worker job is executed.
;;;
;;; To verify that actions are performed we'll mock send-email and
;;; create-github-issue functions and check that they're called with
;;; apropriate arguments.
;;;
;;; We'll wait until job will be executed 2 times and then verify all arguments.
;;;

(def config
  {:web {:port test-web-port}
   :worker-server {:host "localhost"
                   :port 6790}
   :enable-force-login true
   :quartz {:initial-delay-in-seconds 0
            :interval-in-seconds 2}})

(defn mock-latest-release-fn
  "Create latest-release version which returns
  new version every time it called."
  []
  (let [versions (atom {"lib-1" 0
                        "lib-2" 1
                        "lib-3" 2
                        "lib-4" 3})]
    (fn [library]
      (timbre/debug library)
      (let [version (-> versions
                        (swap! update-in [library] inc)
                        (get library))]
        (str "0." version ".2")))))

(defn create-function-mock
  "Creates mock funciton which saves all arguments it called with to
  'arguments' atom vector. Also delivers 'true' to the 'finished' promise
  once it called times-to-call times."
  [arguments times-to-call finished]
  (let [counter (atom times-to-call)]
    (fn [& args]
      (when (zero? @counter)
        (throw (ex-info (str "Function expected to be called only "
                             times-to-call " times.")
                        {})))
      (swap! arguments conj (vec args))
      (when (zero? (swap! counter dec))
        (deliver finished true)))))

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
                        :address "foo@email.com"
                        :template (str "Email {{library}} {{version}} "
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

          ; Login as bar@email.com and create single email action.
          _ (http :get "/force-login?email=bar@email.com&skip-dummy-data=true")
          proj2-id (-> (http :get "/projects") ok?
                       :projects vals first :id)
          _ (ok? (http :post "/actions"
                       {:project-id proj2-id
                        :library "lib-4"
                        :type "email"
                        :address "foo@email.com"
                        :template (str "Email {{library}} {{version}} "
                                       "{{previous-version}} {{project}} "
                                       "{{not-used}}")}))])))

(defn assert-emails-args
  "Validates that send-email was called with expected args."
  [args]
  ; send-email should be called 4 times.
  (is (= (count args) 4))

  (are [v] (some #(= % v) args)
       ; Check that email for lib-2 release was sent 2 times.
       ["foo@email.com" "[Hatnik] lib-2 0.3.2 released"
        "Email lib-2 0.3.2 0.2.2 Default {{not-used}}"]
       ["foo@email.com" "[Hatnik] lib-2 0.4.2 released"
        "Email lib-2 0.4.2 0.3.2 Default {{not-used}}"]

       ; Check that email for lib-4 release was sent 2 times.
       ["bar@email.com" "[Hatnik] lib-4 0.5.2 released"
        "Email lib-4 0.5.2 0.4.2 Default {{not-used}}"]
       ["bar@email.com" "[Hatnik] lib-4 0.6.2 released"
        "Email lib-4 0.6.2 0.5.2 Default {{not-used}}"]))

(defn assert-github-issue-args
  "Validates that create-github-action was called with expected args."
  [args]
  ; create-github-issue should be called 2 times.
  (is (= (count args) 2))

  (are [v] (some #(= % [v]) args)
       ; Github issue should be created for lib-3 2 times.
       {:user "someone"
        :repo "cool-repo"
        :title "Title lib-3 0.4.2 0.3.2 Default {{not-used}}"
        :body (str "Body lib-3 0.4.2 0.3.2 Default {{not-used}}"
                   "\n\nThis issue is created on behalf of @dummy-login")}
       {:user "someone"
        :repo "cool-repo"
        :title "Title lib-3 0.5.2 0.4.2 Default {{not-used}}"
        :body (str "Body lib-3 0.5.2 0.4.2 Default {{not-used}}"
                   "\n\nThis issue is created on behalf of @dummy-login")}))

(defn run-worker
  "Run worker jobs. The job will be run 2 times."
  [db]
  (let [; send-email is called 4 times: 2 times for 2 actions.
        ; Both first and second users have email actions.
        email-args (atom [])
        emails-done (promise)
        send-email (create-function-mock email-args 4 emails-done)

        ; create-github-issue is run 2 times: first user has single
        ; github-issue action.
        gi-args (atom [])
        gi-done (promise)
        create-github-issue (create-function-mock gi-args 2 gi-done)

        utils {:send-email send-email
               :create-github-issue create-github-issue}
        worker (-> (worker/map->Worker {:config config
                                        :db db
                                        :perform-action worker/perform-action
                                        :utils utils})
                   component/start)]
    (try
      (is (deref emails-done 5000 false) "send-email not done")
      (is (deref gi-done 5000 false) "create-github-issue not done")
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
      (is (= (count foo-actions) 3))
      (are [library version] (some #(and (= (:library %) library)
                                         (= (:last-processed-version %) version))
                                   foo-actions)
           "lib-1" "0.3.2"
           "lib-2" "0.4.2"
           "lib-3" "0.5.2")

      (is (= (count bar-actions) 1))
      (is (and (= (:library (first bar-actions)) "lib-4")
               (= (:last-processed-version (first bar-actions))
                  "0.6.2"))))))

(deftest worker-integration-test
  (let [db (component/start (get-db))
        web-server (component/start (map->WebServer {:config config
                                                     :db db}))]
    (timbre/set-level! :info)
    (try
      (with-redefs [ver/latest-release (mock-latest-release-fn)]
        (create-actions)
        (run-worker db)
        (assert-actions-updated))
      (finally
        (component/stop web-server)
        (component/stop db)))))

