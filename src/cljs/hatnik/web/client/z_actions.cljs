(ns hatnik.web.client.z-actions
  (:require [jayq.core :as jq]
            [hatnik.web.client.app-state :as state]
            [hatnik.web.client.message :as msg]
            [hatnik.schema :as schm]
            [schema.core :as s])
  (:use [jayq.core :only [$]]))

(def default-error-message "Fields highlighted in red are invalid. Please check them out.")

(defn get-data-from-input [id]
  (.-value (.getElementById js/document id)))

(defn wrap-error-alert [callback]
  (fn [response]
    (when (= "error" (get response "result"))
      (msg/danger (get response "message")))
    (callback response)))

(defn ajax [url type data callback]
  (jq/ajax url
           {:type type
            :data (.stringify js/JSON
                              (clj->js data))
            :contentType "application/json"
            :dataType "json"
            :async true
            :error #(msg/danger "Invalid request. Please, check request data.")
            :success #(callback (js->clj %))}))

(defn get-github-repos [github-name callback error-handler]
  (jq/ajax (str "https://api.github.com/users/" github-name "/repos")
           {:type "GET"
            :success callback
            :error error-handler}))

(defn common-update-callback [msg data response]
  (when (= "ok" (get response "result"))
    (.modal ($ :#iModalAddAction) "hide")
    (.modal ($ :#iModalProjectMenu) "hide")
    (state/update-all-view)))

(defn create-new-project-callback [name response]
  (when (= "ok" (get response "result"))
    (.modal ($ :#iModalProjectMenu) "hide")
    (state/update-all-view)))

(defn send-new-project-request [name]
  (if (s/check schm/Project {:name name})
    (msg/danger default-error-message)
    (ajax "/api/projects" "POST" {:name name} #(create-new-project-callback name %))))

(defn create-new-action-callback [data response]
  (when (= "ok" (get response "result"))
    (.modal ($ :#iModalAddAction) "hide")
    (state/update-all-view)))

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
   :operations (:file-operations data-pack)})

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

(defn send-new-action [data-pack]
  (let [{:keys [build schema]} (actions-config (:type data-pack))
        data (build data-pack)]
    (if (s/check schema data)
      (msg/danger default-error-message)
      (ajax "/api/actions" "POST" data
            (wrap-error-alert #(create-new-action-callback data %))))))

(defn test-action [data-pack done-callback]
  (let [config (actions-config (:type data-pack))
        data ((:build config) data-pack)]
    (if (s/check (:schema config) data)
      (do (msg/danger default-error-message)
          (done-callback))
      (do
        (msg/info (:text-progress config))
       (ajax "/api/actions/test" "POST" data
            (fn [response]
              (done-callback)
              (case (get response "result")
                "ok" (msg/success (:text-done config))
                "error" (msg/danger (get response "message")))))))))

(def action-update-error-message "Couldn't update the action. Please file a bug if the issue persists.")

(defn update-action [data-pack]
  (let [{:keys [build schema]} (actions-config (:type data-pack))
        data (build data-pack)]
    (if (s/check schema data)
      (msg/danger default-error-message)
      (ajax
       (str "/api/actions/" (:id data-pack)) "PUT"
       data (wrap-error-alert
             #(common-update-callback action-update-error-message data %))))))

(defn delete-action [action-id]
  (.modal ($ :#iModalAddAction) "hide")
  (ajax
   (str "/api/actions/" action-id) "DELETE"
   {} (wrap-error-alert
       #(common-update-callback "Couldn't delete the action. Please file a bug if the issue persists." {} %))))

(defn ^:export delete-project [project-id]
  (.modal ($ :#iModalProjectMenu) "hide")
  (ajax
   (str "/api/projects/" project-id) "DELETE"
   {} (wrap-error-alert
       #(common-update-callback "Couldn't delete the project. Please file a bug if the issue persists." {} %))))

(defn ^:export update-project [project-id new-name]
  (let [data {:name new-name}]
    (if (s/check schm/Project data)
      (msg/danger default-error-message)
      (ajax
       (str "/api/projects/" project-id) "PUT"  data
       (wrap-error-alert #(common-update-callback "Couldn't rename the project. Please file a bug if the issue persists." {} %))))))


(defn get-library [library callback]
  (ajax
   (str "/api/library-version?library=" library) "GET"
   {} callback))
