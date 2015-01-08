(ns hatnik.web.client.app-state
  (:require [hatnik.web.client.utils :as u]))

(def app-state
  (atom {; Here we store data from the server
         :projects []
         :user {}}))

(defn update-projects-list [data]
  (when (= "ok" (get data "result"))
    (swap! app-state
           assoc-in [:projects]
           (get data "projects"))))

(defn add-new-project [id name]
  (swap! app-state
         assoc-in [:data :projects]
         (into [{"id" id "name" name}]
               (-> @app-state
                   :data
                   :projects))))

(defn update-user-data [data]
  (when (= "ok" (get data "result"))
    (swap! app-state
           assoc-in [:user :email]
           (get data "email"))))

(defn set-current-project [id]
  (swap! app-state
         assoc :current-project id))

(defn update-all-views []
  (u/ajax "/api/projects" "GET" nil update-projects-list))

(defn update-project-actions [action]
  (update-all-view))

