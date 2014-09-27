(ns hatnik.web.server.actions
  (:require [ring.util.response :as resp]
            [compojure.core :refer :all]
            [clj-http.client :as client]

            [hatnik.versions :as ver]
            [hatnik.db.storage :as stg]
            [hatnik.config :refer [config]]))

(defn get-user [req]
  (-> req :session :user))

(defn create-action [user data]
  (let [version (ver/latest-release (:library data))
        action (assoc data
                 :last-processed-version version)
        id (stg/create-action! @stg/storage (:id user) action)]
   (resp/response
    (if id
      {:result :ok
       :id id
       :last-processed-version version}
      {:result :error
       :message "Couldn't create action."}))))

(defn update-action [user id data]
  (let [version (ver/latest-release (:library data))
        action (assoc data
                 :last-processed-version version)]
    (stg/update-action! @stg/storage (:id user) id action)
    (resp/response
     {:result :ok
      :last-processed-version version})))

(defn delete-action [user id]
  (stg/delete-action! @stg/storage (:id user) id)
  (resp/response {:result :ok}))

(defn test-action [user data]
  (let [url (str "http://" (-> config :worker-server :host)
                 ":" (-> config :worker-server :port)
                 "/test-action")
        resp (->> {:form-params (assoc data
                                  :user user)
                   :content-type :json
                   :accept :json
                   :as :json}
                  (client/post url)
                  :body)]
    (if (= (:result resp) "ok")
      (resp/response {:result :ok})
      (resp/response {:result :error
                      :message "Couldn't test action"}))))

(defroutes actions-api
  (POST "/" req (create-action (get-user req) (:body req)))
  (PUT "/:id" [id :as req] (update-action (get-user req) id (:body req)))
  (DELETE "/:id" [id :as req] (delete-action (get-user req) id))
  (POST "/test" req (test-action (get-user req) (:body req))))
