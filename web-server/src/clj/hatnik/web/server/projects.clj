(ns hatnik.web.server.projects
  (:require [ring.util.response :as resp]
            [compojure.core :refer :all]))

(defn all-projects []
  (resp/response
   {:result :ok
    :projects [{:id "1"
                :name "Foo"
                :actions [{:id "100"
                           :group "org.clojure"
                           :artifact "clojure"
                           :last-processed-version "1.6.0"
                           :type "email"
                           :address "email@example.com"
                           :template "Library release: {{LIBRARY}} {{VERSION}}"
                           :disabled? false}]}
               {:id "2"
                :name "Baz"
                :actions [{:id "200"
                           :group ""
                           :artifact "quil"
                           :last-processed-version "2.2.0"
                           :type "email"
                           :address "email@example.com"
                           :template "Library release: {{LIBRARY}} {{VERSION}}"
                           :disabled? true}]}]}))

(defn create-project [config]
  (resp/response {:result :ok :id "23"}))

(defn update-project [id config]
  (resp/response {:result :ok}))

(defn delete-project [id]
  (resp/response {:result :ok}))

(defroutes projects-api
  (GET "/" [] (all-projects))
  (POST "/" req (create-project (:body req)))
  (PUT "/:id" [id :as req] (update-project id (:body req)))
  (DELETE "/:id" [id :as req] (delete-project id)))
