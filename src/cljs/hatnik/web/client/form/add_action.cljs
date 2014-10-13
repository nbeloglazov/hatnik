(ns hatnik.web.client.form.add-action
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true])
  (:use [jayq.core :only [$]]))

(defn on-modal-close [component]  
  (om/detach-root 
   (om/get-node component)))

(defn- add-action-component [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:artifact-value ""
       :email-template ""
       :user-email "email@template.com"
       :type :email})

    om/IRenderState
    (render-state [this state]
      (dom/div 
       #js {:className "modal-dialog"}
       (dom/div 
        #js {:className "modal-content"}
        (dom/div #js {:className "modal-header"}
                 (dom/h4 nil "Add new action"))

        (dom/div #js {:className "modal-body"}
                 (dom/p nil "BODY"))
        (dom/div #js {:className "modal-footer"}
                 (dom/p nil "FOOTER")))))

    om/IDidMount
    (did-mount [this]
      (let [modal-window ($ (:modal-jq-id data))]
        (.on modal-window
             "hidden.bs.modal" (fn [_ _] (on-modal-close owner)))
        (.modal modal-window)))))

(defn show [project-id]
  (om/root add-action-component {:project-id project-id :modal-jq-id :#iModalAddAction}
           {:target (.getElementById js/document "iModalAddAction")}))


