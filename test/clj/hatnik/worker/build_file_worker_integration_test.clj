(ns hatnik.worker.build-file-worker-integration-test
  (:require [clojure.test :refer :all]
            [me.raynes.fs :as fs]
            [clojure.string :refer [join]]
            [com.stuartsierra.component :as component]
            [hatnik.versions :as ver]
            [hatnik.test-utils :refer :all :exclude [config]]
            [hatnik.worker.worker :as worker]
            [hatnik.db.storage :as stg]
            [hatnik.web.server.handler :refer [map->WebServer]]
            [taoensso.timbre :as timbre]))

;;;
;;; Integrations test that verifies that actions in projects that based on build
;;  files are updated periodically.
;;;
;;; Following situation is exercised:
;;; 1. Create a project based on a build file that served locally. Build file
;;;    contains 2 libs: clojure, ring.
;;; 2. Once project is created we modify original build file by replacing ring
;;;    with quil and increasing version of clojure. Version increased not in
;;;    build file, but in mocked 'latest-release` version.
;;; 3. SyncBuildFileActionsJob should be executed and it should remove the
;;;    action based on ring, create new action for quil and don't modify
;;;    clojure action even though version is "changed". Only UpdateActionsJob
;;;    can change versions of libraries in action.
;;;

(def config
  {:web {:port test-web-port}
   :enable-force-login true
   :quartz {:initial-delay-in-seconds 0
            :interval-in-seconds 2
            :jobs #{:sync-build-file-actions}}})

(def dir (fs/temp-dir "build-file-worker-integration"))

(defn create-project-clj [libs]
  (let [template (join \newline ["(defproject my-proj \"0.0.1\""
                                 "  :dependencies ["
                                 "%s"
                                 "])"])
        content (->> (map #(str "[" % " \"0.0.1\"]") libs)
                     (join \newline)
                     (format template))]
    (spit (fs/file dir "project.clj") content)))

(defn file-server-fixture [f]
  (let [server (file-server (.getAbsolutePath dir))]
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
    (str "0." (@versions library) ".2")))

(defn assert-actions
  "Wait until actions for the user match provided actions. Timeout is 5 seconds."
  [actions]
  (binding [clj-http.core/*cookie-store* (clj-http.cookies/cookie-store)]
    (http :get "/force-login?email=foo@email.com&skip-dummy-data=true")
    (let [timeout-time (+ (System/currentTimeMillis) 5000)
          actions (->> actions
                       (sort-by :library)
                       (map #(assoc % :type "build-file")))
          cur-actions (fn []
                        (let [project (-> (http :get "/projects") ok?
                                          :projects vals first)]
                          (->> (:actions project)
                               vals
                               (map #(dissoc % :project-id :id))
                               (sort-by :library))) )]
      (while (not= actions (cur-actions))
        (when (> (System/currentTimeMillis) timeout-time)
          (throw (ex-info (str "Actions don't match. Expected "
                               (pr-str actions) ", actual "
                               (pr-str (cur-actions)))
                          {})))
        (Thread/sleep 250)))))

(defn create-project
  "Remove default project. Create a project based on build file.
  Initially build file contains only 2 libraries: clojure and ring."
  []
  (binding [clj-http.core/*cookie-store* (clj-http.cookies/cookie-store)]
    ; Create initial project.clj file
    (create-project-clj ["org.clojure/clojure" "ring"])

    ; Login, delete default project and create project based on the project.clj
    (http :get "/force-login?email=foo@email.com&skip-dummy-data=true")
    (let [id (-> (http :get "/projects") ok? :projects keys first name)]
      (http :delete (str "/projects/" id)))
    (ok? (http :post "/projects"
               {:name "Build file project"
                :type "build-file"
                :build-file (str "http://localhost:"
                                 file-server-port
                                 "/project.clj")
                :action {:library "none"
                         :project-id "none"
                         :type "noop"}}))

    ; Verify that actions are created.
    (assert-actions [{:library "org.clojure/clojure"
                      :last-processed-version "0.1.2"}
                     {:library "ring"
                      :last-processed-version "0.2.2"}])))

(defn run-worker [db versions]
  (let [worker (-> (worker/map->Worker {:config config
                                        :db db})
                   component/start)]
    (try
      (assert-actions [{:library "org.clojure/clojure"
                        :last-processed-version "0.1.2"}
                       {:library "quil"
                        :last-processed-version "0.3.2"}])

      ; Change ring version and update project.clj
      (swap! versions assoc-in ["ring"] 3)
      (create-project-clj ["quil" "ring"])

      (assert-actions [{:library "quil"
                        :last-processed-version "0.3.2"}
                       {:library "ring"
                        :last-processed-version "0.3.2"}])

      ; Remove all deps to simulate some error when no deps were returned.
      ; Old actions should not be deleted.
      (create-project-clj [])
      (Thread/sleep 3000)
      (assert-actions [{:library "quil"
                        :last-processed-version "0.3.2"}
                       {:library "ring"
                        :last-processed-version "0.3.2"}])

      ; Return deps back
      (create-project-clj ["org.clojure/clojure" "quil" "ring"])
      (assert-actions [{:library "org.clojure/clojure"
                        :last-processed-version "0.2.2"}
                       {:library "quil"
                        :last-processed-version "0.3.2"}
                       {:library "ring"
                        :last-processed-version "0.3.2"}])
      (finally
        (component/stop worker)))))

(deftest build-file-worker-integration-test
  (let [db (component/start (get-db))
        web-server (component/start (map->WebServer {:config config
                                                     :db db}))
        versions (atom {"org.clojure/clojure" 1
                        "ring" 2
                        "quil" 3})]
    (timbre/set-level! :info)
    (try
      (with-redefs [ver/latest-release (mock-latest-release-fn versions)]
        (create-project)
        (create-project-clj ["org.clojure/clojure" "quil"])
        (swap! versions assoc-in ["org.clojure/clojure"] 2)
        (run-worker db versions))
      (finally
        (component/stop web-server)
        (component/stop db)))))


(comment

  (file-server-fixture build-file-worker-integration-test)

  )
