(ns hatnik.web.server.projects
  (:require [ring.util.response :as resp]
            [compojure.core :refer :all]
            [hatnik.db.storage :as stg]))

(defn get-user [req]
  (-> req :session :user))

(defn load-actions [user-id project]
  (let [actions (stg/get-actions @stg/storage user-id (:id project))]
    (assoc project :actions actions)))

(defn all-projects [user]
  (let [projects (->> (stg/get-projects @stg/storage (:id user))
                      (map #(dissoc % :user-id))
                      (map #(load-actions (:id user) %)))]
    (resp/response
     {:result :ok
      :projects projects})))

(defn create-project [user data]
  (let [project (assoc data :user-id (:id user))
        id (stg/create-project! @stg/storage project)]
    (resp/response {:result :ok :id id})))

(defn update-project [user id data]
  (let [project (assoc data :user-id (:id user))]
    (stg/update-project! @stg/storage (:id user) id project))
  (resp/response {:result :ok}))

(defn delete-project [user id]
  (stg/delete-project! @stg/storage (:id user) id)
  (resp/response {:result :ok}))

(defroutes projects-api
  (GET "/" req (all-projects (get-user req)))
  (POST "/" req (create-project (get-user req) (:body req)))
  (PUT "/:id" [id :as req] (update-project (get-user req) id (:body req)))
  (DELETE "/:id" [id :as req] (delete-project (get-user req) id)))
