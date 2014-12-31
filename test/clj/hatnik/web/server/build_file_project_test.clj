(ns hatnik.web.server.build-file-project-test
  (:require [clojure.test :refer :all]
            [ring.adapter.jetty :refer [run-jetty]]
            [clj-http.client :as c]
            [hatnik.versions :as ver]
            [hatnik.test-utils :refer :all]
            [compojure.route :as route]))

(def file-server-port 49052)

(defn file-server [folder]
  (run-jetty
   (route/files "/" {:root folder})
   {:join? false
    :port file-server-port
    :host "localhost"}))

(defn file-server-fixture [f]
  (let [server (file-server "dev/build-files")]
    (try
      (f)
      (finally
        (.stop server)))))

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
                 :build-file (str "http://localhost:" file-server-port
                                  "/project.clj")
                 :action {:library "none"
                          :project-id "none"
                          :type "noop"}}

        resp (http :post "/projects" project)
        _ (ok? resp)
        proj-id (:id resp)
        actions (:actions resp)
        actions-without-ids (->> actions vals (map #(dissoc % :id)) doall)

        ; Check that actions are present in response
        ; and also try to get all projects and check that it is correct.
        _ (are [library]
            (let [expected {:library library
                            :last-processed-version (ver/latest-release library)
                            :type "build-file"
                            :project-id proj-id}]
              (some #(= % expected) actions-without-ids))
            "org.clojure/clojure" "quil" "ring")
        ; Checking all projects
        _ (data-equal (map-by-id [(assoc project
                                    :id proj-id
                                    :actions actions)])
                      (-> (http :get "/projects") ok? :projects))
        ]))


(comment

 ((join-fixtures [system-fixture cookie-store-fixture file-server-fixture])
  build-file-project-test)

 )
