(ns hatnik.web.server.login
  (:require [ring.util.response :as resp]
            [compojure.core :refer :all]
            [clj-http.client :as client]
            [hatnik.config :refer [config]]
            [tentacles.users :as github]))

; https://github.com/login/oauth/authorize?scope=user:email&client_id=f850785344ec6d812ab2

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
    (println "Email found: " email emails)
    (if email
      (assoc response :session {:email email})
      response)))

;  (client/get "http://localhost:8080/api/projects" {:as :json})

#_(client/post "https://github.com/login/oauth/access_token"
                          {:form-params {:client_id (:github-id config)
                                         :client_secret (:github-secret config)
                                         :code "b0d0a3e3fca4ea77fab1"}
                           :content-type :json
                           :accept :json})

(defn current-user [req]
  (resp/response
   (if-let [email (-> req :session :email)]
     {:result :ok
      :logged-in? true
      :email email}
     {:result :ok
      :logged-in? false})))

(defn logout []
  (assoc (resp/response {:result :ok})
    :session nil))

(defroutes login-api
  (GET "/github" [code state] (github-login code state))
  (GET "/current-user" req (current-user req))
  (GET "/logout" [] (logout)))
