(ns hatnik.web.server.dummy-data
  (:require [taoensso.timbre :as timbre]
            [hatnik.db.storage :as stg]))


(defn create-default-project [db user-id]
  (let [proj-id (stg/create-project! db {:name "Default"
                                         :user-id user-id})]
    (stg/create-action! db user-id {:project-id proj-id
                                    :type "email"
                                    :subject "{{library}} {{version}} released"
                                    :body "Hey {{library}} released. New version {{VERSION}}."
                                    :library "com.nbeloglazov/hatnik-test-lib"
                                    :last-processed-version "0.0.9"})
    (stg/create-action! db user-id {:project-id proj-id
                                    :type "github-issue"
                                    :repo "nbeloglazov/hatnik"
                                    :title "{{library}} {{version}} released"
                                    :body "Hey ho. {{library}} {{version}}, was {{previous-version}}."
                                    :library "org.clojure/clojure"
                                    :last-processed-version "1.6.0"})))

(defn create-quil-project [db user-id]
  (let [proj-id (stg/create-project! db {:name "Quil"
                                         :user-id user-id})]
    (stg/create-action! db user-id {:project-id proj-id
                                    :type "email"
                                    :subject "{{library}} {{version}} released"
                                    :body "Quil {{version}} was released. Go and update wiki and examples!"
                                    :library "quil"
                                    :last-processed-version "2.2.2"})
    (stg/create-action! db user-id {:project-id proj-id
                                    :type "noop"
                                    :library "org.clojure/clojurescript"
                                    :last-processed-version "0.0-0"})))

(defn create-hatnik-project [db user-id]
  (stg/create-project! db {:name "Hatnik"
                           :user-id user-id}))

(defn create-dummy-data [db user-id]
  (create-default-project db user-id)
  (create-quil-project db user-id)
  (create-hatnik-project db user-id))
