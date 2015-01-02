(ns hatnik.web.server.login
  (:require [ring.util.response :as resp]
            [taoensso.timbre :as timbre]
            [compojure.core :refer :all]
            [compojure.route :refer [not-found]]
            [clj-http.client :as client]
            [tentacles.users :as github]
            [hatnik.db.storage :as stg]
            [hatnik.web.server.dummy-data :as dd]))

; https://github.com/login/oauth/authorize?scope=user:email&client_id=f850785344ec6d812ab2

(defn create-user
  "Creates new user. Also creates default project for the user. Returns user
  map with added :id key."
  [db user]
  (timbre/info "Creating new user" (:email user))
  (let [id (stg/create-user! db user)]
    (timbre/info "Create default project for user")
    (stg/create-project! db {:name "Default"
                             :user-id id
                             :type "regular"})
    (assoc user :id id)))

(defn get-or-create-user
  "Checks if user already exists. If it does - returns user object, if doesn't
  creates new user. Might update existing user object if it's not complete."
  [db user]
  (if-let [existing (stg/get-user db (:email user))]
    (if (and (contains? existing :github-token)
             (contains? existing :github-login))
      existing
      ; We didn't save user github token or username last time.
      ; Save it now.
      (do (timbre/debug "Updating existing user " (:email user))
          (stg/update-user! db (:email user) user)
          (merge existing user)))
    (create-user db user)))

(defn github-login
  "GitHub login callback. User redirected here after he authenticates via github.
  This function retrieves user's email and creates new user or logs him in.
  Returns redirect response if sucessfully logged in."
  [db config code state]
  (let [resp (client/post "https://github.com/login/oauth/access_token"
                          {:form-params {:client_id (:github-id config)
                                         :client_secret (:github-secret config)
                                         :code code}
                           :content-type :json
                           :accept :json
                           :as :json})
        user-token (-> resp :body :access_token)
        user-login (:login (github/me {:oauth-token user-token}))
        emails (github/emails {:oauth-token user-token})
        email (first (for [entry emails
                           :when (:primary entry)]
                       (:email entry)))
        response (resp/redirect "/")]
    (timbre/debug "Github login."
                  "resp:" resp
                  "emails:" emails
                  "selected email:" email)
    (if email
      (let [user (get-or-create-user db
                  {:email email
                   :github-token user-token
                   :github-login user-login})]
       (assoc response :session {:user user}))
      response)))

(defn force-login
  "Utility method to login without any authentication.
  Enabiled via :enable-force-login and should be used only
  for in development environment."
  [db email skip-dummy-data]
  (if-let [user (stg/get-user db email)]
    (-> (resp/response {:result :ok})
        (assoc :session {:user user}))
    (let [user {:email email
                :github-token "dummy-token"
                :github-login "dummy-login"}
          user (if skip-dummy-data
                 (create-user db user)
                 (let [id (stg/create-user! db user)]
                   (dd/create-dummy-data db id)
                   (assoc user :id id)))]
      (-> (resp/response {:result :ok})
          (assoc :session {:user user})))))

(defn current-user
  "Returns current user info."
  [req]
  (resp/response
   (if-let [email (-> req :session :user :email)]
     {:result :ok
      :logged-in? true
      :email email}
     {:result :ok
      :logged-in? false})))

(defn logout
  "Logs user out and returns redirect to home page."
  []
  (assoc (resp/response {:result :ok})
    :headers {"Location" "/"}
    :status 302
    :session nil))

(defn login-api-routes [db config]
  (->> [(GET "/github" [code state] (github-login db config code state))
        (GET "/current-user" req (current-user req))
        (when (:enable-force-login config)
          (GET "/force-login" [email skip-dummy-data]
               (force-login db email skip-dummy-data)))
        (GET "/logout" [] (logout))]
       (remove nil?)
       (apply routes)))
