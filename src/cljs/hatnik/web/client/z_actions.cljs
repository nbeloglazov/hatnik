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


(defn ajax [url type data callback]
  (jq/ajax url
           {:type type
            :data (.stringify js/JSON 
                              (clj->js data))
            :contentType "application/json"
            :dataType "json"
            :async false
            :success callback}))

(defn send-new-project-request []
  (let [name (.-value (.getElementById js/document "project-name-input"))]
    (if (= "" name)
      (js/alert "Project name must be not empty!")
      (ajax  "/api/projects" "POST" {:name name} #(create-new-project-callback name %)))))

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
        (ajax "/api/actions" "POST" data #(create-new-email-action-callback data %))))))


(defn test-new-email-action [project-id]
  (let [artifact (get-data-from-input "artifact-input")
        email (get-data-from-input "emain-input")
        email-body (get-data-from-input "emain-body-input")
        data {:project-id project-id
              :type "email"
              :address email
              :template email-body
              :library artifact
              :version "NEW-VERSION"
              :previous-version "OLD-VERSION"}]
    (if (or
         (= "" artifact)
         (= "" email)
         (= "" email-body))
      (js/alert "Wrong data! Check out fields!")

      (ajax "/api/actions/test" "POST" data (fn [e])))))

(defn wrap-error-alert [callback]
  (fn [reply]
    (let [resp (js->clj reply)]
      (when (= "error" (get resp "result")) (js/alert (get resp "message")))
      (callback reply))))


(defn common-update-callback [msg data reply]
  (let [resp (js->clj reply)]
    (if (= "ok" (get resp "result"))
      (state/update-all-view)
      (js/alert msg))))

(defn update-email-action [project-id action-id]
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
        (ajax 
         (str "/api/actions/" action-id) "PUT" 
         data (wrap-error-alert
               #(common-update-callback "Action don't updated!" data %)))))))


(defn delete-action [action-id]
  (.modal ($ :#iModal) "hide")
  (ajax 
   (str "/api/actions/" action-id) "DELETE"
   {} (wrap-error-alert
       #(common-update-callback "Action don't deleted!" {} %))))



(defn delete-project []
  (let [project-id (:current-project (deref state/app-state))]
    (.modal ($ :#iModalProjectMenu) "hide")
    (ajax
     (str "/api/projects/" project-id) "DELETE"
     {} (wrap-error-alert 
         #(common-update-callback "Project don't deleted!" {} %)))))

(defn update-project []
  (let [project-id (:current-project (deref state/app-state))
        new-name (get-data-from-input "project-name-edit-input")]
    (.modal ($ :#iModalProjectMenu) "hide")
    (ajax
     (str "/api/projects/" project-id) "PUT"
     {:name new-name} 
     (wrap-error-alert #(common-update-callback "Project don't renamed!" {} %)))))

