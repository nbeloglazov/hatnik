(ns hatnik.web.client.project-list
  (:require goog.net.XhrIo
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [hatnik.web.client.components :as widget]))

(def app-state 
  (atom {:num 0
         :projects []}))


(defn click-handler []
  (swap! app-state
         assoc :num
         (+ 1 (:num @app-state))))


(defn project-list-widget [data owner]
  (reify
    om/IRender
    (render [this]
      (widget/accordion-panel
       :header "FOO project"
       :body (dom/p nil "Foo project is good.")
       :body-id "iFooProject"))))

(om/root project-list-widget app-state
         {:target (. js/document (getElementById "iProjectList"))})



