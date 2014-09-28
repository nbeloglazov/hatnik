(ns hatnik.web.client.app-state)

(def app-state 
  (atom {:projects []
         :form-type :email-action
         :user {}
         :current-project nil
         :current-action nil
         :email-form-timer false}))

(defn update-projects-list [reply]
  (let [json (.getResponseJson (.-target reply))
        data (js->clj json)]
    (when (= "ok" (get data "result"))
      (swap! app-state
             assoc :projects
             (get data "projects")))))

(defn set-form-type [action-type]
  (swap! app-state
         assoc :form-type
         action-type))

(defn set-current-project [id]
  (swap! app-state 
         assoc :current-project id))

(defn set-current-action [action]
  (swap! app-state
         assoc :current-action
         action))


(defn add-new-project [id name]
  (swap! app-state
         assoc :projects
         (into [{"id" id "name" name}]
               (:projects @app-state))))


(defn update-user-data [reply]
  (let [json (.getResponseJson (.-target reply))
        data (js->clj json)]
    (when (= "ok" (get data "result"))
      (swap! app-state
             assoc-in [:user :email]
             (get data "email")))))

(defn update-all-view []
  (.send goog.net.XhrIo "/api/projects" update-projects-list) )

(defn update-project-actions [action]
  (.send goog.net.XhrIo "/api/projects" update-projects-list))


(defn get-email-form-timer []
  (:email-form-timer @app-state))

(defn set-email-form-timer [callback time]
  (let [timer (get-email-form-timer)]
    (when timer (js/clearIntervar timer))
    (swap! app-state
           assoc :email-form-timer
           (js/setTimeout callback time))))
