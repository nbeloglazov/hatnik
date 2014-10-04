(ns hatnik.web.server.projects
  (:require [ring.util.response :as resp]
            [compojure.core :refer :all]

            [hatnik.db.storage :as stg]
            [hatnik.web.server.schema :as s]))

(defn get-user
  "Retrieves user from the request map. Assumes that user logged in and
  session exists."
  [req]
  (-> req :session :user))

(defn map-by
  "Converts collection of maps to map of maps. Example:
  (map-by :id [{:id 1 :a :b} {:id 3 :b :c}]) =>
  {1 {:id 1 :a :b} 3 {:id 3 :b :c}}"
  [key coll]
  (into {} (map #(vector (% key) %) coll)))

(defn load-actions
  "Loads all actions for the given project and assoc them to the project."
  [user-id project]
  (let [actions (stg/get-actions @stg/storage user-id (:id project))]
    (assoc project :actions (map-by :id actions))))

(defn all-projects
  "Returns all projects together with actions for the user."
  [user]
  (let [projects (->> (stg/get-projects @stg/storage (:id user))
                      (map #(dissoc % :user-id))
                      (map #(load-actions (:id user) %)))]
    (resp/response
     {:result :ok
      :projects (map-by :id projects)})))

(defn create-project
  "Creates project from given data. Returns the id of the new project."
  [user data]
  (let [project (assoc data :user-id (:id user))
        id (stg/create-project! @stg/storage project)]
    (resp/response {:result :ok :id id})))

(defn update-project [user id data]
  (let [project (assoc data :user-id (:id user))]
    (stg/update-project! @stg/storage (:id user) id project))
  (resp/response {:result :ok}))

(defn delete-project [user id]
  (doseq [action (stg/get-actions @stg/storage (:id user) id)]
    (stg/delete-action! @stg/storage (:id user) (:id action)))
  (stg/delete-project! @stg/storage (:id user) id)
  (resp/response {:result :ok}))

(defroutes projects-api
  (GET "/" req (all-projects (get-user req)))

  (POST "/" req
        (s/ensure-valid s/Project (:body req)
                        (create-project (get-user req) (:body req))))

  (PUT "/:id" [id :as req]
       (s/ensure-valid s/Project (:body req)
                       (update-project (get-user req) id (:body req))))

  (DELETE "/:id" [id :as req] (delete-project (get-user req) id)))
