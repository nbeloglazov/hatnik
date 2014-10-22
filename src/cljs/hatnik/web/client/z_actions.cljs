(ns hatnik.web.client.z-actions
  (:require [jayq.core :as jq]
            [hatnik.web.client.app-state :as state]
            [hatnik.web.client.message :as msg]
            [hatnik.schema :as schm]
            [schema.core :as s])
  (:use [jayq.core :only [$]]))

(def default-error-message "Data is wrong! Check out highlighted fields please.")

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
            :error #(msg/danger "Invalid request! Check out your request data!")
            :success callback}))

(defn get-github-repos [github-name callback error-handler]
  (jq/ajax (str "https://api.github.com/users/" github-name "/repos")
           {:type "GET"
            :success callback
            :error error-handler}))

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
    (if (s/check schm/Project {:name name})
      (msg/danger default-error-message)
      (do
        (.modal ($ :#iModalProject) "hide")
        (ajax  "/api/projects" "POST" {:name name} #(create-new-project-callback name %))))))

(defn create-new-action-callback [data reply]
  (let [resp (js->clj reply)]
    (when (= "ok" (get resp "result"))
      (state/update-all-view))))


(defmulti send-new-action #(:type %))

(defmethod send-new-action :email [data-pack]
  (let [data {:project-id (:project-id data-pack)
              :type "email"
              :address (:user-email data-pack)
              :template (:email-template data-pack)
              :library (:artifact-value data-pack)}]
    (if (s/check schm/EmailAction data)
      (msg/danger default-error-message)
      (do
        (.modal ($ :#iModalAddAction) "hide")
        (ajax "/api/actions" "POST" data 
              (wrap-error-alert #(create-new-action-callback data %)))))))

(defmethod send-new-action :noop [data-pack]
  (let [data {:type "noop"
              :project-id (:project-id data-pack)
              :library (:artifact-value data-pack)}]
    (if (s/check schm/NoopAction data)
      (msg/danger default-error-message)
      (do
        (.modal ($ :#iModalAddAction) "hide")
        (ajax "/api/actions" "POST" data 
              (wrap-error-alert #(create-new-action-callback data %)))))))

(defmethod send-new-action :github-issue [data-pack]
  (let [data {:project-id (:project-id data-pack)
              :type "github-issue"
              :repo (:gh-repo data-pack)
              :title (:gh-issue-title data-pack)
              :body (:gh-issue-body data-pack)
              :library (:artifact-value data-pack)}]
    (if (s/check schm/GithubIssueAction data)
      (msg/danger default-error-message)
      (do
        (.modal ($ :#iModalAddAction) "hide")
        (ajax "/api/actions" "POST" data 
              (wrap-error-alert #(create-new-action-callback data %)))))))


(defmulti test-action #(:type %))
(defmethod test-action :email [data-pack]
  (let [data {:project-id (:project-id data-pack)
              :type "email"
              :address (:user-email data-pack)
              :template (:email-template data-pack)
              :library (:artifact-value data-pack)}]
    (if (s/check schm/EmailAction data)
      (msg/danger default-error-message)      
      (do
        (msg/info "We are trying to send email to you...")
       (ajax "/api/actions/test" "POST" data
            (wrap-error-alert (fn [e] (msg/success "Email sent. Check your inbox."))))))))

(defmethod test-action :github-issue [data-pack]
  (let [data {:project-id (:project-id data-pack)
              :type "github-issue"
              :repo (:gh-repo data-pack)
              :title (:gh-issue-title data-pack)
              :body (:gh-issue-body data-pack)
              :library (:artifact-value data-pack)}]
    (if (s/check schm/GithubIssueAction data)
      (msg/danger default-error-message)
      (do
        (msg/info "We are trying to create issue on Github...")
        (ajax "/api/actions/test" "POST" data
              (wrap-error-alert (fn [e] (msg/success "GitHub issue created. Check out your project."))))))))


(defmulti update-action #(:type %))
(defmethod update-action :email [data-pack]
  (let [data {:project-id (:project-id data-pack)
              :type "email"
              :address (:user-email data-pack)
              :template (:email-template data-pack)
              :library (:artifact-value data-pack)}]
    (if (s/check schm/EmailAction data)
      (msg/danger default-error-message)
      (do
        (.modal ($ :#iModalAddAction) "hide")
        (ajax 
         (str "/api/actions/" (:action-id data-pack)) "PUT" 
         data (wrap-error-alert
               #(common-update-callback "Action don't updated!" data %)))))))

(defmethod update-action :noop [data-pack]
  (let [data {:project-id (:project-id data-pack)
              :type "noop"
              :library (:artifact-value data-pack)}]
    (if (s/check schm/NoopAction data)
      (msg/danger default-error-message)
      (do
        (.modal ($ :#iModalAddAction) "hide")
        (ajax 
         (str "/api/actions/" (:action-id data-pack)) "PUT" 
         data (wrap-error-alert
               #(common-update-callback "Action don't updated!" data %)))))))

(defmethod update-action :github-issue [data-pack]
  (let [data {:project-id (:project-id data-pack)
              :type "github-issue"
              :repo (:gh-repo data-pack)
              :title (:gh-issue-title data-pack)
              :body (:gh-issue-body data-pack)
              :library (:artifact-value data-pack)}]
    (if (s/check schm/GithubIssueAction data)
      (msg/danger default-error-message)
      (do
        (.modal ($ :#iModalAddAction) "hide")
        (ajax 
         (str "/api/actions/" (:action-id data-pack)) "PUT" 
         data (wrap-error-alert
               #(common-update-callback "Action don't updated!" data %)))))))
   
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
  (let [data {:name new-name}]
    (if (s/check schm/Project data)
      (msg/danger default-error-message)
      (do
        (.modal ($ :#iModalProjectMenu) "hide")
        (ajax
         (str "/api/projects/" project-id) "PUT"  data
         (wrap-error-alert #(common-update-callback "Project don't renamed!" {} %)))))))


(defn get-library [library callback]
  (ajax
   (str "/api/library-version?library=" library) "GET"
   {} callback))

