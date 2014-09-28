(ns hatnik.web.server.login
  (:require [ring.util.response :as resp]
            [taoensso.timbre :as timbre]
            [compojure.core :refer :all]
            [compojure.route :refer [not-found]]
            [clj-http.client :as client]
            [tentacles.users :as github]
            [hatnik.config :refer [config]]
            [hatnik.db.storage :as stg]
            [hatnik.web.server.dummy-data :as dd]))

; https://github.com/login/oauth/authorize?scope=user:email&client_id=f850785344ec6d812ab2

(defn create-user [email]
  (timbre/info "Creating new user" email)
  (let [id (stg/create-user! @stg/storage email)]
    (timbre/info "Create default project for user")
    (stg/create-project! @stg/storage {:name "Default"
                                       :user-id id})
    {:email email
     :id id}))

(defn github-login [code state]
  (let [resp (client/post "https://github.com/login/oauth/access_token"
                          {:form-params {:client_id (:github-id config)
                                         :client_secret (:github-secret config)
                                         :code code}
                           :content-type :json
                           :accept :json
                           :as :json})
        user-token (-> resp :body :access_token)
        emails (github/emails {;:client-id (:github-id config)
                               :oauth-token user-token})
        email (first (for [entry emails
                           :when (:primary entry)]
                       (:email entry)))
        response (resp/redirect "/")]
    (timbre/debug "Github login."
                  "resp:" resp
                  "emails:" emails
                  "selected email:" email)
    (if email
      (let [user (or (stg/get-user @stg/storage email)
                     (create-user email))]
       (assoc response :session {:user user}))
      response)))

(defn force-login [email]
  (if (:enable-force-login config)
    (if-let [user (stg/get-user @stg/storage email)]
      (-> (resp/response {:result :ok})
          (assoc :session {:user user}))
      (let [id (stg/create-user! @stg/storage email)]
        (dd/create-dummy-data id)
        (-> (resp/response {:result :ok})
            (assoc :session {:user {:email email
                                    :id id}}))))
    (not-found ":(")))

(defn current-user [req]
  (resp/response
   (if-let [email (-> req :session :user :email)]
     {:result :ok
      :logged-in? true
      :email email}
     {:result :ok
      :logged-in? false})))

(defn logout []
  (assoc (resp/response {:result :ok})
    :headers {"Location" "/"}
    :status 302
    :session nil))

(defroutes login-api
  (GET "/github" [code state] (github-login code state))
  (GET "/current-user" req (current-user req))
  (GET "/force-login" [email] (force-login email))
  (GET "/logout" [] (logout)))
