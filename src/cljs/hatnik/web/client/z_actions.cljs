(ns hatnik.web.client.z-actions
  (:require [jayq.core :as jq]
            [hatnik.web.client.app-state :as state]
            [hatnik.web.client.message :as msg])
  (:use [jayq.core :only [$]]))

(defn get-data-from-input [id]
  (.-value (.getElementById js/document id)))

(defn wrap-error-alert [callback]
  (fn [reply]
    (let [resp (js->clj reply)]
      (when (= "error" (get resp "result")) (msg/danger (get resp "message")))
      (callback reply))))

(defn ajax [url type data callback]
  (jq/ajax url
           {:type type
            :data (.stringify js/JSON 
                              (clj->js data))
            :contentType "application/json"
            :dataType "json"
            :async true
            :success callback}))

(defn common-update-callback [msg data reply]
  (let [resp (js->clj reply)]
    (when (= "ok" (get resp "result"))
      (state/update-all-view))))

(defn create-new-project-callback [name reply]
  (let [resp (js->clj reply)]
    (when (= "ok" (get resp "result"))
      (state/update-all-view))))


(defn ^:export send-new-project-request []
  (let [name (.-value (.getElementById js/document "project-name-input"))]
    (if (= "" name)
      (msg/danger "Project name must be not empty!")
      (do
        (.modal ($ :#iModalProject) "hide")
        (ajax  "/api/projects" "POST" {:name name} #(create-new-project-callback name %))))))

(defn create-new-action-callback [data reply]
  (let [resp (js->clj reply)]
    (when (= "ok" (get resp "result"))
      (state/update-all-view))))

(defn send-new-email-action [project-id artifact email email-body]
  (let [data {:project-id project-id
              :type "email"
              :address email
              :template email-body
              :library artifact}]
    (if (or
         (= "" artifact)
         (= "" email)
         (= "" email-body))
      (msg/danger "Wrong data! Check out fields!")

      (do
        (.modal ($ :#iModalAddAction) "hide")
        (ajax "/api/actions" "POST" data 
              (wrap-error-alert #(create-new-action-callback data %)))))))

(defn send-new-noop-action [project-id artifact]
  (let [data {:project-id project-id
              :type "noop"
              :library artifact}]
    (if (= "" artifact)
      (msg/danger "Wrong data! Check out fields!")

      (do
        (.modal ($ :#iModalAddAction) "hide")
        (ajax "/api/actions" "POST" data 
              (wrap-error-alert #(create-new-action-callback data %)))))))

(defn send-new-github-issue-action [project-id repo title body library]
  (let [data {:project-id project-id
              :type "github-issue"
              :repo repo
              :title title
              :body body
              :library library}]
    (if (or (= "" library)
            (= "" repo)
            (= "" title))
      (msg/danger "Wrong data! Check out fields!")

      (do
        (.modal ($ :#iModalAddAction) "hide")
        (ajax "/api/actions" "POST" data 
              (wrap-error-alert #(create-new-action-callback data %)))))))

(defmulti send-new-action #(:type %))
(defmethod send-new-action :email [data-pack]
  (send-new-email-action (:project-id data-pack)
                         (:artifact-value data-pack)
                         (:user-email data-pack)
                         (:email-template data-pack)))
(defmethod send-new-action :noop [data-pack]
  (send-new-noop-action (:project-id data-pack)
                        (:artifact-value data-pack)))
(defmethod send-new-action :github-issue [data-pack]
  (send-new-github-issue-action (:project-id data-pack)
                                (:gh-repo data-pack)
                                (:gh-issue-title data-pack)
                                (:gh-issue-body data-pack)
                                (:artifact-value data-pack)))


(defn test-new-email-action [project-id artifact email email-body]
  (let [data {:project-id project-id
              :type "email"
              :address email
              :template email-body
              :library artifact}]
    (if (or
         (= "" artifact)
         (= "" email)
         (= "" email-body))
      (msg/danger "Wrong data! Check out fields!")

      (ajax "/api/actions/test" "POST" data
            (wrap-error-alert (fn [e] (msg/success "Email sent. Check your inbox.")))))))

(defn test-github-issue-action [project-id repo title body library]
  (let [data {:project-id project-id
              :type "github-issue"
              :repo repo
              :title title
              :body body
              :library library}]
    (if (or (= "" library)
            (= "" repo)
            (= "" title))
      (msg/danger "Wrong data! Check out fields!")

      (ajax "/api/actions/test" "POST" data
            (wrap-error-alert (fn [e] (msg/success "GitHub issue created. Check out your project.")))))))

(defmulti test-action #(:type %))
(defmethod test-action :email [data-pack]
  (test-new-email-action (:project-id data-pack)
                         (:artifact-value data-pack)
                         (:user-email data-pack)
                         (:email-template data-pack)))
(defmethod test-action :github-issue [data-pack]
  (test-github-issue-action (:project-id data-pack)
                            (:gh-repo data-pack)
                            (:gh-issue-title data-pack)
                            (:gh-issue-body data-pack)
                            (:artifact-value data-pack)))

(defn update-email-action [project-id action-id artifact email email-body]
    (let [data {:project-id project-id
                :type "email"
                :address email
                :template email-body
                :library artifact}]
    (if (or
         (= "" artifact)
         (= "" email)
         (= "" email-body))
      (msg/danger "Wrong data! Check out fields!")

      (do
        (.modal ($ :#iModalAddAction) "hide")
        (ajax 
         (str "/api/actions/" action-id) "PUT" 
         data (wrap-error-alert
               #(common-update-callback "Action don't updated!" data %)))))))

(defn update-noop-action [project-id action-id artifact]
  (let [data {:project-id project-id
              :type "noop"
              :library artifact}]
    (if (= "" artifact)
      (msg/danger "Wrong data! Check out fields!")

      (do
        (.modal ($ :#iModalAddAction) "hide")
        (ajax 
         (str "/api/actions/" action-id) "PUT" 
         data (wrap-error-alert
               #(common-update-callback "Action don't updated!" data %)))))))

(defn update-github-issue-action [project-id action-id repo title body library]
  (let [data {:project-id project-id
              :type "github-issue"
              :repo repo
              :title title
              :body body
              :library library}]
    (if (or (= "" library)
            (= "" repo)
            (= "" title))
      (msg/danger "Wrong data! Check out fields!")

      (do
        (.modal ($ :#iModalAddAction) "hide")
        (ajax 
         (str "/api/actions/" action-id) "PUT" 
         data (wrap-error-alert
               #(common-update-callback "Action don't updated!" data %)))))))

(defmulti update-action #(:type %))
(defmethod update-action :email [data-pack]
  (update-email-action (:project-id data-pack)
                       (:action-id data-pack)
                       (:artifact-value data-pack)
                       (:user-email data-pack)
                       (:email-template data-pack)))
(defmethod update-action :noop [data-pack]
  (update-noop-action (:project-id data-pack)
                      (:action-id data-pack)
                      (:artifact-value data-pack)))
(defmethod update-action :github-issue [data-pack]
  (update-github-issue-action (:project-id data-pack)
                              (:action-if data-pack)
                              (:gh-repo data-pack)
                              (:gh-issue-title data-pack)
                              (:gh-issue-body data-pack)
                              (:artifact-value data-pack)))

(defn delete-action [action-id]
  (.modal ($ :#iModalAddAction) "hide")
  (ajax 
   (str "/api/actions/" action-id) "DELETE"
   {} (wrap-error-alert
       #(common-update-callback "Action don't deleted!" {} %))))

(defn ^:export delete-project [project-id]
  (.modal ($ :#iModalProjectMenu) "hide")
  (ajax
   (str "/api/projects/" project-id) "DELETE"
   {} (wrap-error-alert 
       #(common-update-callback "Project don't deleted!" {} %))))

(defn ^:export update-project [project-id new-name]
  (.modal ($ :#iModalProjectMenu) "hide")
  (ajax
   (str "/api/projects/" project-id) "PUT"
   {:name new-name} 
   (wrap-error-alert #(common-update-callback "Project don't renamed!" {} %))))


(defn get-library [library callback]
  (ajax
   (str "/api/library-version?library=" library) "GET"
   {} callback))

