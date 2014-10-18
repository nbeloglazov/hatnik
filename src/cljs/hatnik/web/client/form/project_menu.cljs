(ns hatnik.web.client.form.project-menu
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [hatnik.web.client.z-actions :as action])
  (:use [jayq.core :only [$]]))


(defn- project-menu [data owner]
  (reify
    om/IDidMount
    (did-mount [this]
      (let [modal-window ($ (:modal-jq-id data))]
        (.modal modal-window)))

    om/IInitState 
    (init-state [this]
      {:name (:name data)})

    om/IRenderState
    (render-state [this state]
      (let [name (:name state)
            id (:project-id data)]
        (dom/div
         #js {:className "modal-dialog"}
         (dom/div
          #js {:className "modal-content"}
          (dom/div #js {:className "modal-header"}
                   (dom/h4 #js {:className "modal-title"} "Project menu"))

          (dom/div 
           #js {:className "modal-body"}
           (dom/form nil
                     (dom/input #js {:className "form-control"
                                     :type "text"
                                     :value (:name state)
                                     :onChange #(om/set-state! owner :name (.. % -target -value))})))

          (dom/div #js {:className "modal-footer"}
                   (dom/div #js {:className "btn btn-primary pull-left"
                                 :onClick #(action/update-project id name)} 
                            "Update")
                   (dom/div #js {:className "btn btn-danger pull-right"
                                 :onClick #(action/delete-project id)} 
                            "Delete"))))))))

(defn show [& data-pack]
  (om/root project-menu (assoc (into {} (map vec (partition-all 2 data-pack)))
                          :modal-jq-id :#iModalProjectMenu)
           {:target (.getElementById js/document "iModalProjectMenu")}))
