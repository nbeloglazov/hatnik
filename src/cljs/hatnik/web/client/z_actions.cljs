(ns hatnik.web.client.z-actions
  (:require [jayq.core :as jq]
            [hatnik.web.client.app-state :as state])
  (:use [jayq.core :only [$]]))

(defn get-data-from-input [id]
  (.-value (.getElementById js/document id)))

(defn create-new-project-callback [name reply]
  (let [resp (js->clj reply)]
    (if (= "ok" (get resp "result"))
      (state/add-new-project (get resp "id") name)
      (js/alert (str "Sorry. Project " name " can't be created.")))))

(defn send-new-project-request []
  (let [name (.-value (.getElementById js/document "project-name-input"))]
    (if (= "" name)
      (js/alert "Project name must be not empty!")
        (jq/ajax "/api/projects" 
                 {:type "POST"
                  :data (.stringify js/JSON 
                                    (clj->js {:name name}))
                  :contentType "application/json"
                  :dataType "json"
                  :async false
                  :success #(create-new-project-callback name %)}))))

(defn create-new-email-action-callback [data reply]
  (let [resp (js->clj reply)]
    (if (= "ok" (get resp "result"))
      (state/update-project-actions (assoc data
                                      "id" (get resp "id")
                                      "last-processed-version" (get rest "last-processed-version")))
      (js/alert "Action don't created."))))


(defn send-new-email-action [project-id]
  (let [artifact (get-data-from-input "artifact-input")
        email (get-data-from-input "emain-input")
        email-body (get-data-from-input "emain-body-input")
        data {:project-id project-id
              :type "email"
              :address email
              :template email-body
              :library artifact}]
    (if (or
         (= "" artifact)
         (= "" email)
         (= "" email-body))
      (js/alert "Wrong data! Check out fields!")

      (do
        (.modal ($ :#iModal) "hide")
        (jq/ajax "/api/actions"
                 {:type "POST"
                  :data (.stringify js/JSON 
                                    (clj->js data))
                  :contentType "application/json"
                  :dataType "json"
                  :async false
                  :success #(create-new-email-action-callback data %)})))))

