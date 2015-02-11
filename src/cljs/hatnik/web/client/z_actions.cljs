(ns hatnik.web.client.z-actions
  (:require [hatnik.web.client.app-state :as state]
            [hatnik.web.client.message :as msg]
            [hatnik.web.client.utils :as u]
            [hatnik.schema :as schm]
            [schema.core :as s])
  (:use [jayq.core :only [$]]))

(def default-error-message "Fields highlighted in red are invalid. Please check them out.")

(defn get-data-from-input [id]
  (.-value (.getElementById js/document id)))

(defn wrap-error-alert [callback]
  (fn [response]
    (when (= "error" (:result response))
      (msg/danger (:message response)))
    (callback response)))

(defn common-update-callback [msg data response]
  (when (= "ok" (:result response))
    (state/update-all-views)
    (.modal ($ :#iModalAddAction) "hide")
    (.modal ($ :#iModalProjectMenu) "hide")))

(defn create-new-action-callback [data response]
  (when (= "ok" (:result response))
    (state/update-all-views)
    (.modal ($ :#iModalAddAction) "hide")))

(defn build-email-action [data-pack]
  {:project-id (:project-id data-pack)
   :type "email"
   :body (:body data-pack)
   :subject (:title data-pack)
   :library (:library data-pack)})

(defn build-gh-issue-action [data-pack]
  {:project-id (:project-id data-pack)
   :type "github-issue"
   :repo (:gh-repo data-pack)
   :title (:title data-pack)
   :body (:body data-pack)
   :library (:library data-pack)})

(defn build-gh-pull-request [data-pack]
  {:project-id (:project-id data-pack)
   :library (:library data-pack)
   :type "github-pull-request"
   :repo (:gh-repo data-pack)
   :title (:title data-pack)
   :body (:body data-pack)
   :operations (if (= (:file-operation-type data-pack) "manual")
                 (:file-operations data-pack)
                 (:file-operation-type data-pack))})

(defn build-noop-action [data-pack]
  {:type "noop"
   :project-id (:project-id data-pack)
   :library (:library data-pack)})

(def actions-config
  {"noop" {:build build-noop-action
           :schema schm/NoopAction}
   "email" {:build build-email-action
            :schema schm/EmailAction
            :text-progress "Sending test email..."
            :text-done "The email is sent. Check your inbox."}
   "github-issue" {:build build-gh-issue-action
                   :schema schm/GithubIssueAction
                   :text-progress "Creating test issue on Github..."
                   :text-done "The issue is created. Check the repository."}
   "github-pull-request" {:build build-gh-pull-request
                          :schema schm/GithubPullRequestAction
                          :text-progress "Creating test pull request on GitHub..."
                          :text-done "The pull request is created. Check the repository."}})

(defn convert-action-to-schema
  "Converts action represented by a map with client-specific structur to a map that conforms
  Action schema and can be sent to server."
  [action]
  (let [build (-> (:type action) actions-config :build)]
    (build action)))

(defn send-new-action [data-pack]
  (let [schema (-> data-pack :type actions-config :schema)
        data (convert-action-to-schema data-pack)]
    (if (s/check schema data)
      (msg/danger default-error-message)
      (u/ajax "/api/actions" "POST" data
            (wrap-error-alert #(create-new-action-callback data %))))))

(defn test-action [data-pack done-callback]
  (let [config (actions-config (:type data-pack))
        data (convert-action-to-schema data-pack)]
    (if (s/check (:schema config) data)
      (do (msg/danger default-error-message)
          (done-callback))
      (do
        (msg/info (:text-progress config))
       (u/ajax "/api/actions/test" "POST" data
            (fn [response]
              (done-callback)
              (case (:result response)
                "ok" (msg/success (:text-done config))
                "error" (msg/danger (:message response)))))))))

(def action-update-error-message "Couldn't update the action. Please file a bug if the issue persists.")

(defn update-action [data-pack]
  (let [schema (-> data-pack :type actions-config :schema)
        data (convert-action-to-schema data-pack)]
    (if (s/check schema data)
      (msg/danger default-error-message)
      (u/ajax
       (str "/api/actions/" (:id data-pack)) "PUT"
       data (wrap-error-alert
             #(common-update-callback action-update-error-message data %))))))

(defn delete-action [action-id]
  (u/ajax
   (str "/api/actions/" action-id) "DELETE"
   {} (wrap-error-alert
       #(common-update-callback "Couldn't delete the action. Please file a bug if the issue persists." {} %))))

(defn create-new-project-callback [name response]
  (when (= "ok" (:result response))
    (state/update-all-views)
    (.modal ($ :#iModalProjectMenu) "hide")))

(defn convert-project-to-schema
  "Converts project map that has client-specific structure to a map that conforms Project schema
  and can be sent to server."
  [project]
  (let [base (select-keys project [:name :type])]
    (if (= (:type project) "build-file")
      (assoc base
             :build-file (:build-file project)
             :action (convert-action-to-schema (:action project)))
      base)))

(defn create-project [project done-callback]
  (let [project (convert-project-to-schema project)]
    (if (s/check schm/Project project)
      (do (msg/danger default-error-message)
          (done-callback))
      (u/ajax "/api/projects" "POST" project
              (wrap-error-alert
               #(do (done-callback) (create-new-project-callback name %))))
)))

(defn delete-project [project-id]
  (u/ajax
   (str "/api/projects/" project-id) "DELETE"
   {} (wrap-error-alert
       #(common-update-callback "Couldn't delete the project. Please file a bug if the issue persists." {} %))))

(defn update-project [project done-callback]
  (let [id (:id project)
        project (convert-project-to-schema project)]
    (if (s/check schm/Project project)
      (do (msg/danger default-error-message)
          (done-callback))
      (u/ajax
       (str "/api/projects/" id) "PUT"  project
       (wrap-error-alert #(do
                            (done-callback)
                            (common-update-callback "Couldn't update the project. Please file a bug if the issue persists." {} %)))
))))


(defn get-library [library callback]
  (u/ajax
   (str "/api/library-version?name=" (:name library)
        "&type=" (:type library))
   "GET"
   {} callback))
