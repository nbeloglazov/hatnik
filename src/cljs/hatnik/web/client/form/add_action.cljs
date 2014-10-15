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

(defn add-action-footer [data owner]
  (reify
    om/IRender
    (render [this]
      (let [project-id (:project-id data)
            artifact (:artifact-value data)
            type (:type data)
            email (:user-email data)
            template (:email-template data)]
        (dom/div nil
                 (dom/button 
                  
                  #js {:className "btn btn-primary pull-left"
                       :onClick #(action/send-new-action
                                  {:type type
                                   :project-id project-id
                                   :artifact-value artifact
                                   :user-email email
                                   :email-template template})} "Submit")

                 (dom/button 
                  #js {:className "btn btn-default"
                       :onClick #(action/test-action 
                                  {:type type
                                   :project-id project-id
                                   :artifact-value artifact
                                   :user-email email
                                   :email-template template})} "Test"))))))

(defn update-action-footer [data owner]
  (reify
    om/IRender
    (render [this]
      (let [project-id (:project-id data)
            artifact (:artifact-value data)
            action-id (:action-id data)
            type :email
            email (:user-email data)
            template (:email-template data)]
          (dom/div 
           nil
           (dom/button 
            #js {:className "btn btn-primary pull-left"
                 :onClick #(action/update-action 
                             {:type type
                              :action-id action-id
                              :project-id project-id
                              :artifact-value artifact
                              :user-email email
                              :email-template template})} "Update")

           (dom/button 
            #js {:className "btn btn-default"
                 :onClick #(action/test-action 
                            {:type type
                             :project-id project-id
                             :artifact-value artifact
                             :user-email email
                             :email-template template})} "Test"))))))

(def default-email-template
  (str "Hello there\n\n"
       "{{library}} {{version}} has been released! "
       "Previous version was {{previous-version}}\n\n"
       "Your Hatnik"))

(defmulti get-init-state #(:type %))

(defmethod get-init-state :add [data _] 
  {:artifact-value ""
   :project-id (:project-id data)
   :email-template default-email-template
   :user-email (:user-email data)
   :type :email})

(defmethod get-init-state :update [data _]
  (let [action (:action data)]
    {:type :email
     :project-id (:project-id data)
     :artifact-value (get action "library")
     :action-id (get action "id")
     :email-template (get action "template")
     :user-email (get action "address")
     :callback action/update-email-action}))

(defmulti get-action-header #(:type %))
(defmethod get-action-header :add [_ _] (dom/h4 nil "Add new action"))
(defmethod get-action-header :update [data _] 
  (let [action-id (:action-id data)]
    (dom/h4 #js {:className "modal-title"} "Update action"
            (dom/button 
             #js {:className "btn btn-danger pull-right"
                  :onClick #(action/delete-action action-id)}
             "Delete"))))

(defmulti get-action-footer #(:type %))

(defmethod get-action-footer :add [data state _]
  (om/build add-action-footer state))

(defmethod get-action-footer :update [data state _]
  (om/build update-action-footer state))

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
                 (get-action-header {:type (:type data) :action-id (:action-id state)} owner))

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
                 (get-action-footer data state owner)))))

    om/IDidMount
    (did-mount [this]
      (let [modal-window ($ (:modal-jq-id data))]
        (.modal modal-window)))))

(defn show [& data-pack]
  (.log js/console "!")
  (om/root add-action-component 
           (assoc 
               (into {} (map vec (partition-all 2 data-pack)))
             :modal-jq-id :#iModalAddAction)            
           {:target (.getElementById js/document "iModalAddAction")}))


