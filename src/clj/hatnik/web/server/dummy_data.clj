(ns hatnik.web.server.dummy-data
  (:require [taoensso.timbre :as timbre]
            [hatnik.db.storage :as stg]))


(defn create-default-project [db user-id]
  (let [proj-id (stg/create-project! db {:name "Default"
                                         :user-id user-id
                                         :type "regular"})]
    (stg/create-action! db user-id {:project-id proj-id
                                    :type "email"
                                    :subject "{{library}} {{version}} released"
                                    :body "Hey {{library}} released. New version {{VERSION}}."
                                    :library {:name "com.nbeloglazov/hatnik-test-lib"
                                              :type "jvm"}
                                    :last-processed-version "0.0.9"})
    (stg/create-action! db user-id {:project-id proj-id
                                    :type "github-issue"
                                    :repo "nbeloglazov/hatnik"
                                    :title "{{library}} {{version}} released"
                                    :body "Hey ho. {{library}} {{version}}, was {{previous-version}}."
                                    :library {:name "org.clojure/clojure"
                                              :type "jvm"}
                                    :last-processed-version "1.6.0"})
    (stg/create-action! db user-id {:project-id proj-id
                                    :type "github-pull-request"
                                    :repo "nbeloglazov/hatnik-test-lib"
                                    :title "Update {{library}} to {{version}}"
                                    :body "Results \n {{results-table}}"
                                    :operations [{:file "some/file"
                                                  :regex "regex"
                                                  :replacement "replacement"}
                                                 {:file "project.clj"
                                                  :regex "{{library}} \"[^\"]+\""
                                                  :replacement "{{library}} \"{{version}}\""}
                                                 {:file "README.md"
                                                  :regex "hello"
                                                  :replacement "world"}]
                                    :library {:name "compojure"
                                              :type "jvm"}
                                    :last-processed-version "0.0.0"})))

(defn create-quil-project [db user-id]
  (let [proj-id (stg/create-project! db {:name "Quil"
                                         :user-id user-id
                                         :type "regular"})]
    (stg/create-action! db user-id {:project-id proj-id
                                    :type "email"
                                    :subject "{{library}} {{version}} released"
                                    :body "Quil {{version}} was released. Go and update wiki and examples!"
                                    :library {:name "quil"
                                              :type "jvm" }
                                    :last-processed-version "2.2.2"})
    (stg/create-action! db user-id {:project-id proj-id
                                    :type "noop"
                                    :library {:name "org.clojure/clojurescript"
                                              :type "jvm"}
                                    :last-processed-version "0.0-0"})))

(defn create-hatnik-project [db user-id]
  (stg/create-project! db {:name "Hatnik"
                           :user-id user-id
                           :type "regular"}))

(defn create-dummy-data [db user-id]
  (create-default-project db user-id)
  (create-quil-project db user-id)
  (create-hatnik-project db user-id))
