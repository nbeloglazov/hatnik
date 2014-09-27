(ns hatnik.web.client.z-actions
  (:require [jayq.core :as jq]
            [hatnik.web.client.app-state :as state])
  (:use [jayq.core :only [$]]))

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
