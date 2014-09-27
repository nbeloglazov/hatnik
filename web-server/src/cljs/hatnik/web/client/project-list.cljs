(ns hatnik.web.client.project-list
  (:require goog.net.XhrIo
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [hatnik.web.client.components :as widget]))

(def app-state 
  (atom {:projects []}))


(defn project-list-widget [data owner]
  (reify
    om/IRender
    (render [this]
      (apply dom/div nil
       (map 
        (fn [prj]
          (widget/accordion-panel
           :header (get prj "name")
           :body (dom/p nil "This project is good.")
           :body-id (str "__" (get prj "name"))))
        (:projects data))))))

(om/root project-list-widget app-state
         {:target (. js/document (getElementById "iProjectList"))})


(defn update-projects-list [reply]
  (let [json (.getResponseJson (.-target reply))
        data (js->clj json)]
    (when (= "ok" (get data "result"))
      (swap! app-state
             assoc :projects
             (get data "projects")))))

(.send goog.net.XhrIo "/api/projects" update-projects-list)
