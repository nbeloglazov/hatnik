(ns hatnik.web.client.app-state)

(def app-state 
  (atom {
         ; Here we store data from the server
         :data {:projects []
                :user {}}

         ; Here we store a local data (like ui state)
         :ui {:form-type :email-action
              :current-project nil
              :current-action nil
              :email-form-timer false
              :email-artifact-value ""}}))

(defn update-projects-list [reply]
  (let [json (.getResponseJson (.-target reply))
        data (js->clj json)]
    (when (= "ok" (get data "result"))
      (swap! app-state
             assoc-in [:data :projects]
             (get data "projects")))))

(defn set-form-type [action-type]
  (swap! app-state
         assoc-in [:ui :form-type]
         action-type))

(defn set-current-project [id]
  (swap! app-state 
         assoc-in [:ui :current-project] id))

(defn set-current-action [action]
  (swap! app-state
         assoc-in [:ui :current-action]
         action))

(defn add-new-project [id name]
  (swap! app-state
         assoc-in [:data :projects]
         (into [{"id" id "name" name}]
               (-> @app-state
                   :data
                   :projects))))

(defn update-user-data [reply]
  (let [json (.getResponseJson (.-target reply))
        data (js->clj json)]
    (when (= "ok" (get data "result"))
      (swap! app-state
             assoc-in [:data :user :email]
             (get data "email")))))

(defn set-current-artifact-value [value]
  (swap! app-state
         assoc-in [:ui :email-artifact-value]
         value))

(defn update-all-view []
  (.send goog.net.XhrIo "/api/projects" update-projects-list) )

(defn update-project-actions [action]
  (.send goog.net.XhrIo "/api/projects" update-projects-list))

