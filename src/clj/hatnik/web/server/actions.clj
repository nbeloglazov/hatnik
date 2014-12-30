(ns hatnik.web.server.actions
  (:require [ring.util.response :as resp]
            [compojure.core :refer :all]
            [clj-http.client :as client]

            [hatnik.versions :as ver]
            [hatnik.db.storage :as stg]
            [hatnik.schema :as s]
            [hatnik.schema-utils :as su]))

(defn get-user
  "Retrieves user from the request map. Assumes that user logged in and
  session exists."
  [req]
  (-> req :session :user))

(defn create-action
  "Creates action for the given user and returns id of the created action.
  Response contains the latest library version."
  [db user data]
  (let [version (ver/latest-release (:library data))]
    (cond (nil? version)
          (resp/response
           {:result :error
            :message (str "Unknown library: " (:library data))})

          (= (:type data) "build-file")
          (resp/response
           {:result :error
            :message "Build-file actions cannot be created manually."})

          :default
          (let [action (assoc data :last-processed-version version)
                id (stg/create-action! db (:id user) action)]
            (resp/response
             (if id
               {:result :ok
                :id id
                :last-processed-version version}
               {:result :error
                :message "Couldn't create action."}))))))

(defn update-action
  "Updates action for given user and returns response containing the
  latest version of the library."
  [db user id data]
  (let [version (ver/latest-release (:library data))]
    (cond (nil? version)
          (resp/response
           {:result :error
            :message (str "Unknown library: " (:library data))})

          (= (:type data) "build-file")
          (resp/response
           {:result :error
            :message "Build-file actions cannot be created manually."})
          :default
          (let [action (assoc data :last-processed-version version)]
            (stg/update-action! db (:id user) id action)
            (resp/response
             {:result :ok
              :last-processed-version version})))))

(defn delete-action
  "Deletes action for given user."
  [db user id]
  (stg/delete-action! db (:id user) id)
  (resp/response {:result :ok}))

(defn test-action
  "Performs test action invocation."
  [user data worker-server]
  (let [url (str "http://" (:host worker-server)
                 ":" (:port worker-server)
                 "/test-action")
        data (assoc data
               :version "2.3.4"
               :previous-version "1.2.3")
        resp (->> {:form-params data
                   :content-type :json
                   :accept :json
                   :as :json}
                  (client/post url)
                  :body)]
    (if (and (= (:result resp) "ok")
             (not= (:result-for-user resp) "error"))
      (resp/response {:result :ok})
      (resp/response {:result :error
                      :message (:message resp)}))))

(defn actions-api-routes
  "Builds routes for acessing actions API."
  [db config]
  (routes
   (POST "/" req
         (su/ensure-valid s/Action (:body req)
                          (create-action db (get-user req) (:body req))))

   (PUT "/:id" [id :as req]
        (su/ensure-valid s/Action (:body req)
                         (update-action db (get-user req) id (:body req))))

   (DELETE "/:id" [id :as req] (delete-action db (get-user req) id))

   (POST "/test" req
         (su/ensure-valid s/Action (:body req)
                          (test-action (get-user req) (:body req)
                                       (:worker-server config))))))
