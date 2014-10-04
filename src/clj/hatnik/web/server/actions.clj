(ns hatnik.web.server.actions
  (:require [ring.util.response :as resp]
            [compojure.core :refer :all]
            [clj-http.client :as client]

            [hatnik.versions :as ver]
            [hatnik.db.storage :as stg]
            [hatnik.config :refer [config]]
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
  [user data]
  (if-let [version (ver/latest-release (:library data))]
    (let [action (-> data
                     (assoc :last-processed-version version)
                     (restrict-email-to-be-current user))
          id (stg/create-action! @stg/storage (:id user) action)]
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
  [user id data]
  (if-let [version (ver/latest-release (:library data))]
    (let [action (-> data
                     (assoc :last-processed-version version)
                     (restrict-email-to-be-current user))]
      (stg/update-action! @stg/storage (:id user) id action)
      (resp/response
       {:result :ok
        :last-processed-version version}))
    (resp/response
     {:result :error
      :message (str "Uknown library: " (:library data)) })))

(defn delete-action
  "Deletes action for given user."
  [user id]
  (stg/delete-action! @stg/storage (:id user) id)
  (resp/response {:result :ok}))

(defn test-action
  "Performs test action invocation."
  [user data]
  (let [url (str "http://" (-> config :worker-server :host)
                 ":" (-> config :worker-server :port)
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

(defroutes actions-api
  (POST "/" req
        (s/ensure-valid s/Action (:body req)
                        (create-action (get-user req) (:body req))))

  (PUT "/:id" [id :as req]
       (s/ensure-valid s/Action (:body req)
                       (update-action (get-user req) id (:body req))))

  (DELETE "/:id" [id :as req] (delete-action (get-user req) id))

  (POST "/test" req
        (s/ensure-valid s/Action (:body req)
                        (test-action (get-user req) (:body req)))))
