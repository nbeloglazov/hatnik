(ns hatnik.web.client.app-state)

(def default-email-template
  (str "Hello there\n\n"
       "{{library}} {{version}} has been released! "
       "Previous version was {{previous-version}}\n\n"
       "Your Hatnik"))

(def app-state 
  (atom {; Here we store data from the server
         :projects []
         :user {}}))

(defn update-projects-list [reply]
  (let [json (.getResponseJson (.-target reply))
        data (js->clj json)]
    (when (= "ok" (get data "result"))
      (swap! app-state
             assoc-in [:projects]
             (get data "projects")))))

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
             assoc-in [:user :email]
             (get data "email")))))

(defn set-current-project [id]
  (swap! app-state 
         assoc :current-project id))

(defn update-all-view []
  (.send goog.net.XhrIo "/api/projects" update-projects-list) )

(defn update-project-actions [action]
  (.send goog.net.XhrIo "/api/projects" update-projects-list))

