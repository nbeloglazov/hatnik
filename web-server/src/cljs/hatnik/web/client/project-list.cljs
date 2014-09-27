(ns hatnik.web.client.project-list
  (:require goog.net.XhrIo
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [hatnik.web.client.components :as widget]))

(def app-state 
  (atom {:projects []}))


(om/root widget/project-list app-state
         {:target (. js/document (getElementById "iProjectList"))})


(defn update-projects-list [reply]
  (let [json (.getResponseJson (.-target reply))
        data (js->clj json)]
    (when (= "ok" (get data "result"))
      (swap! app-state
             assoc :projects
             (get data "projects")))))

(.send goog.net.XhrIo "/api/projects" update-projects-list)
