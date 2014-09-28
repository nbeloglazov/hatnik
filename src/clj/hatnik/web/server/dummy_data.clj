(ns hatnik.web.server.dummy-data
  (:require [taoensso.timbre :as timbre]
            [hatnik.db.storage :as stg]))


(defn create-default-project [user-id]
  (let [proj-id (stg/create-project! @stg/storage {:name "Default"
                                                   :user-id user-id})]
    (stg/create-action! @stg/storage user-id {:project-id proj-id
                                              :type :email
                                              :template "Hey {{LIBRARY}} released. New version {{VERSION}}."
                                              :address "nikelandjelo@gmail.com"
                                              :library "com.nbeloglazov/hatnik-test-lib"
                                              :last-processed-version "0.0.9"})))

(defn create-quil-project [user-id]
  (let [proj-id (stg/create-project! @stg/storage {:name "Quil"
                                                   :user-id user-id})]
    (stg/create-action! @stg/storage user-id {:project-id proj-id
                                              :type :email
                                              :template "Quil {{VERSION}} was released. Go and update wiki and examples!"
                                              :address "nikelandjelo@gmail.com"
                                              :library "quil"
                                              :last-processed-version "2.2.2"})
    (stg/create-action! @stg/storage user-id {:project-id proj-id
                                              :type :email
                                              :template "ClojureScript {{VERSION}} was released. Go and update template!"
                                              :address "norg113@gmail.com"
                                              :library "org.clojure/clojurescript"
                                              :last-processed-version "0.0.0"})))

(defn create-hatnik-project [user-id]
  (stg/create-project! @stg/storage {:name "Hatnik"
                                     :user-id user-id}))

(defn create-dummy-data [user-id]
  (create-default-project user-id)
  (create-quil-project user-id)
  (create-hatnik-project user-id))
