(ns hatnik.web.client.form.add-action
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [hatnik.web.client.z-actions :as action])
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

(defn user-email-component [data owner]
  (reify
    om/IRender
    (render [this]
      (dom/div #js {:className "form-group"}
               (dom/label #js {:for "emain-input"} "Email")
               (dom/input #js {:type "email"
                               :className "form-control"
                               :id "emain-input"
                               :value (:value data)
                               :disabled "disabled"})))))

(defn email-template-component [data owner]
  (reify
    om/IRender
    (render [this]
      (dom/div #js {:className "form-group"}
                     (dom/label #js {:for "emain-body-input"} "Email body")
                     (dom/textarea #js {:cols "40"
                                        :className "form-control"
                                        :id "emain-body-input"
                                        :value (:template data)
                                        :onChange #((:template-handler data) (.. % -target -value))})))))

(defn action-input-form [data owner]
  (reify
    om/IRender
    (render [this]
      (dom/form nil
                (om/build artifact-input-component (:artifact-value data))
                (om/build user-email-component (:user-email data))
                (om/build email-template-component (:email data))))))

(defn action-footer [data owner]
  (reify
    om/IRender
    (render [this]
      (let [project-id (:project-id data)
            artifact (:artifact-value data)
            type "email"
            email (:user-email data)
            template (:email-template data)
            callback (:callback data)]
        (dom/div nil
                 (dom/button 
                  
                  #js {:className "btn btn-primary pull-left"
                       :onClick #(callback
                                  project-id type artifact email template)} "Submit")

                 (dom/button 
                  #js {:className "btn btn-default"
                       :onClick #(action/test-new-email-action (-> @data :ui :current-project))} "Test"))))))

(def default-email-template
  (str "Hello there\n\n"
       "{{library}} {{version}} has been released! "
       "Previous version was {{previous-version}}\n\n"
       "Your Hatnik"))

(defmulti get-init-state #(:type %))

(defmethod get-init-state :add [_ _] 
  {:artifact-value ""
   :email-template default-email-template
   :user-email "email@template.com"
   :type :email})

(defmethod get-init-state :update [data _]
  (let [action (:action data)]
    {:artifact-value (get action "library")
     :email-template (get action "template")
     :user-email (get action "address")
     :type (keyword (get action "type"))}))

(defmulti get-action-header #(:type %))
(defmethod get-action-header :add [_ _] (dom/h4 nil "Add new action"))
(defmethod get-action-header :update [_ _] (dom/h4 nil "Update action"))

(defn- add-action-component [data owner]
  (reify
    om/IInitState
    (init-state [_]
      (get-init-state data owner))

    om/IRenderState
    (render-state [this state]
      (dom/div 
       #js {:className "modal-dialog"}
       (dom/div 
        #js {:className "modal-content"}
        (dom/div #js {:className "modal-header"}
                 (get-action-header data owner))

        (dom/div #js {:className "modal-body"}
                 (om/build action-input-form 
                           {:artifact-value 
                            {:value (:artifact-value state)
                             :handler #(om/set-state! owner :artifact-value %)}

                            :user-email
                            {:value (:user-email state)}
                            
                            :email
                            {:template (:email-template state)
                             :template-handler #(om/set-state! owner :email-template %)}}))
        (dom/div #js {:className "modal-footer"}
                 (om/build action-footer 
                           (merge state
                                  {:project-id (:project-id data)
                                   :callback (:callback data)}))))))

    om/IDidMount
    (did-mount [this]
      (let [modal-window ($ (:modal-jq-id data))]
        (.on modal-window
             "hidden.bs.modal" (fn [_ _] (on-modal-close owner)))
        (.modal modal-window)))))

(defn show [project-id & {:keys [type action callback]
                          :or {action nil
                               type :add
                               callback action/send-new-email-action}}]
  (om/root add-action-component 
           {:project-id project-id 
            :action action
            :modal-jq-id :#iModalAddAction 
            :callback callback 
            :type type}
           {:target (.getElementById js/document "iModalAddAction")}))


