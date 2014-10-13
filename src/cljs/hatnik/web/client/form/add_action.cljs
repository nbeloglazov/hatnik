(ns hatnik.web.client.form.add-action
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true])
  (:use [jayq.core :only [$]]))

(defn on-modal-close [component]  
  (om/detach-root 
   (om/get-node component)))

(defn artifact-input-component [data owner]
  (reify
    om/IRender
    (render [this]
      (dom/div #js {:className "form-group has-warning"
                    :id "artifact-input-group"}
               (dom/label #js {:htmlFor "artifact-input"} "Library")
               (dom/input #js {:type "text"
                               :className "form-control"
                               :placeholder "e.g. org.clojure/clojure"
                               :onChange #((:handler data) (.. % -target -value))
                               :value (:value data)})))))

(defn action-input-form [data owner]
  (reify
    om/IRender
    (render [this]
      (dom/form nil
                (om/build artifact-input-component (:artifact-value data))))))

(defn action-footer [data owner]
  (reify
    om/IRender
    (render [this]
      (dom/div nil
               (dom/p nil "hi: ")
               (dom/p nil
                      (:artifact-value data))))))

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
                 (om/build action-input-form 
                           {:artifact-value 
                            {:value (:artifact-value state)
                             :handler #(om/set-state! owner :artifact-value %)}}))
        (dom/div #js {:className "modal-footer"}
                 (om/build action-footer state)))))

    om/IDidMount
    (did-mount [this]
      (let [modal-window ($ (:modal-jq-id data))]
        (.on modal-window
             "hidden.bs.modal" (fn [_ _] (on-modal-close owner)))
        (.modal modal-window)))))

(defn show [project-id]
  (om/root add-action-component {:project-id project-id :modal-jq-id :#iModalAddAction}
           {:target (.getElementById js/document "iModalAddAction")}))


