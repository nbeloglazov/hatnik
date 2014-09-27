(ns hatnik.web.server.login
  (:require [ring.util.response :as resp]
            [compojure.core :refer :all]))

(defn github-login [code state]
  (resp/redirect "/"))

(defn current-user []
  (resp/response
   {:result :ok
    :logged-in? true
    :email "me@myplace.com"}))

(defn logout []
  (resp/response {:result :ok}))

(defroutes login-api
  (GET "/github" [code state] (github-login code state))
  (GET "/current-user" [] (current-user))
  (GET "/logout" [] (logout)))
