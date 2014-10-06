(ns hatnik.web.server.actions
  (:require [ring.util.response :as resp]
            [compojure.core :refer :all]
            [clj-http.client :as client]

            [hatnik.versions :as ver]
            [hatnik.db.storage :as stg]
            [hatnik.web.server.schema :as s]))

(defn get-user
  "Retrieves user from the request map. Assumes that user logged in and
  session exists."
  [req]
  (-> req :session :user))

(defn restrict-email-to-be-current
  "Makes :address field in the action map contain email of current user.
  Currently we allow to send emails only to the user itself, can't send
  to other addresses."
  [action user]
  (if (:address action)
    (assoc action :address (:email user))
    action))

(defn create-action
  "Creates action for the given user and returns id of the created action.
  Response contains the latest library version."
  [db user data]
  (if-let [version (ver/latest-release (:library data))]
    (let [action (-> data
                     (assoc :last-processed-version version)
                     (restrict-email-to-be-current user))
          id (stg/create-action! db (:id user) action)]
      (resp/response
       (if id
         {:result :ok
          :id id
          :last-processed-version version}
         {:result :error
          :message "Couldn't create action."})))
    (resp/response
     {:result :error
      :message (str "Uknown library: " (:library data))})))

(defn update-action
  "Updates action for given user and returns response containing the
  latest version of the library."
  [db user id data]
  (if-let [version (ver/latest-release (:library data))]
    (let [action (-> data
                     (assoc :last-processed-version version)
                     (restrict-email-to-be-current user))]
      (stg/update-action! db (:id user) id action)
      (resp/response
       {:result :ok
        :last-processed-version version}))
    (resp/response
     {:result :error
      :message (str "Uknown library: " (:library data)) })))

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
        resp (->> {:form-params (restrict-email-to-be-current data user)
                   :content-type :json
                   :accept :json
                   :as :json}
                  (client/post url)
                  :body)]
    (if (= (:result resp) "ok")
      (resp/response {:result :ok})
      (resp/response {:result :error
                      :message (:message resp)}))))

(defn actions-api-routes
  "Builds routes for acessing actions API."
  [db config]
  (routes
   (POST "/" req
         (s/ensure-valid s/Action (:body req)
                         (create-action db (get-user req) (:body req))))

   (PUT "/:id" [id :as req]
        (s/ensure-valid s/Action (:body req)
                        (update-action db (get-user req) id (:body req))))

   (DELETE "/:id" [id :as req] (delete-action db (get-user req) id))

   (POST "/test" req
         (s/ensure-valid s/Action (:body req)
                         (test-action (get-user req) (:body req)
                                      (:worker-server config))))))
