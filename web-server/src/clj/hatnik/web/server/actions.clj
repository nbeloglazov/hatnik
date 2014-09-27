(ns hatnik.web.server.actions
  (:require [ring.util.response :as resp]
            [compojure.core :refer :all]))

(defn create-action [config]
  (resp/response
   {:result :ok
    :id "23"
    :last-processed-version "1.6.0"}))

(defn update-action [id config]
  (resp/response
   {:result :ok
    :last-processed-version "1.7.0"}))

(defn delete-action [id]
  (resp/response {:result :ok}))

(defn test-action [config]
  (resp/response {:result :ok}))

(defroutes actions-api
  (POST "/" req (create-action (:body req)))
  (PUT "/:id" [id :as req] (update-action id (:body req)))
  (DELETE "/:id" [id] (delete-action id))
  (POST "/test" req (test-action (:body req))))
