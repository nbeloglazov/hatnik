(ns hatnik.web.server.projects
  (:require [ring.util.response :as resp]
            [compojure.core :refer :all]

            [hatnik.db.storage :as stg]
            [hatnik.web.server.build-files :as bf]
            [hatnik.schema :as s]
            [hatnik.schema-utils :as su]))

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
  [db user-id project]
  (let [actions (stg/get-actions db user-id (:id project))]
    (assoc project :actions (map-by :id actions))))

(defn all-projects
  "Returns all projects together with actions for the user."
  [db user]
  (let [projects (->> (stg/get-projects db (:id user))
                      (map #(dissoc % :user-id))
                      (map #(load-actions db (:id user) %)))]
    (resp/response
     {:result :ok
      :projects (map-by :id projects)})))

(defn valid-project?
  "Checks whether provided project is valid. If project has only name
  then it is valied. Otherwise it has to have :build-file and :action fields. Also :build-file should point to a valid project file."
  [data]
  (case (:type data)
    "regular" true
    "build-file" (and (not (empty? (bf/actions-from-build-file (:build-file data))))
                      (= (-> data :action :project-id)
                         (-> data :action :library :name)
                         "none"))))

(defn delete-actions
  "Deletes all actions for given project."
  [db user-id project-id]
  (doseq [action (stg/get-actions db user-id project-id)]
    (stg/delete-action! db user-id (:id action))))

(defn create-actions
  "Creates actions for given project."
  [db user-id project-id actions]
  (->> actions
       (map #(assoc % :project-id project-id))
       (map #(assoc % :id (stg/create-action! db user-id %)))
       doall))

(defn create-project
  "Creates project from given data. Returns the id of the new project."
  [db user data]
  (if (valid-project? data)
    (let [project (assoc data :user-id (:id user))
          id (stg/create-project! db project)]
      (if (:build-file data)
        (resp/response {:result :ok
                        :id id
                        :actions (->> (:build-file data)
                                      bf/actions-from-build-file
                                      (create-actions db (:id user) id)
                                      (map-by :id))})
        (resp/response {:result :ok :id id})))
    (resp/response {:result :error
                    :message "Invalid project data."})))

(defn update-project [db user id data]
  (if (valid-project? data)
    (let [old-project (->> (stg/get-projects db (:id user))
                           (filter #(= (:id %) id))
                           first)
          project (assoc data :user-id (:id user))]
      (if (and old-project
               (not= (:type old-project) (:type project)))
        (resp/response {:result :error
                        :message "Projects cannot change types."})
        (do
          (stg/update-project! db (:id user) id project)
          (if (:build-file data)
            (do (delete-actions db (:id user) id)
                (resp/response {:result :ok
                                :actions (->> (:build-file data)
                                              bf/actions-from-build-file
                                              (create-actions db (:id user) id)
                                              (map-by :id))}))
            (resp/response {:result :ok})))))
    (resp/response {:result :error
                    :message "Invalid project data."})))

(defn delete-project [db user id]
  (delete-actions db (:id user) id)
  (stg/delete-project! db (:id user) id)
  (resp/response {:result :ok}))

(defn projects-api-routes
  "Builds routes for acessing projects API."
  [db]
  (routes
   (GET "/" req (all-projects db (get-user req)))

   (POST "/" req
         (su/ensure-valid s/Project (:body req)
                          (create-project db (get-user req) (:body req))))

   (PUT "/:id" [id :as req]
        (su/ensure-valid s/Project (:body req)
                         (update-project db (get-user req) id (:body req))))

   (DELETE "/:id" [id :as req] (delete-project db (get-user req) id))))
