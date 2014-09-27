(ns hatnik.web.client.components
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))


(defn accordion-panel [& {:keys [body-id header body open] :or {:open true}}]
  (dom/div 
   #js {:className "panel panel-default"}
   (dom/div 
    #js {:className "panel-heading"}
    (dom/div 
     nil
     (dom/h4 #js {:className "panel-title"}
             (dom/a #js 
                    {:data-parent "#accrodion"
                     :data-toggle "collapse"
                     :href (str "#" body-id)} header))))
           (dom/div #js {:className (if open 
                                      "panel-collapse collapse in"
                                      "panel-collapse collapse")
                         :id body-id}
                    (dom/div #js {:className "panel-body"}
                             body))))

