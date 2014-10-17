(ns hatnik.web.client.form.add-action
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [hatnik.web.client.z-actions :as action])
  (:use [jayq.core :only [$]]
        [clojure.string :only [split]]))

(defn on-modal-close [component]  
  (om/detach-root 
   (om/get-node component)))

(defn library-check-handler [reply callback]
  (let [resp (js->clj reply)]
    (if (= "ok" (get resp "result"))
      (callback "has-success" nil)
      (callback "has-error" nil))))

(defn check-input-value [data-handler check-handler timer new-val]
  (js/clearTimeout timer)  
  (check-handler 
   "has-warning"
   (js/setTimeout 
    (fn []
      (action/get-library new-val #(library-check-handler % check-handler)))
    2000))
  (data-handler new-val))

(defn artifact-input-component [data owner]
  (reify
    om/IInitState
    (init-state [this]
      {:timer nil
       :form-status (if (= "" (:value data))
                      "has-warning"
                      "has-success")
       :value (:value data)})

    om/IRenderState
    (render-state [this state]
      (dom/div #js {:className (str "form-group " (:form-status state))
                    :id "artifact-input-group"}
               (dom/label #js {:htmlFor "artifact-input"} "Library")
               (dom/input #js {:type "text"
                               :className "form-control"
                               :placeholder "e.g. org.clojure/clojure"
                               :onChange #(check-input-value 
                                           (:handler data) 
                                           (fn [x timer] 
                                             (om/set-state! owner :form-status x)
                                             (om/set-state! owner :timer timer))
                                           (:timer state)
                                           (.. % -target -value))
                               :value (:value data)})))))

(defn option-element-map [val pred]
  (if pred
    #js {:className "form-control" :selected "selected" :value val}
    #js {:className "form-control" :value val}))

(defn action-type-component [data owner]
  (reify
    om/IRender
    (render [this]
      (let [callback (:handler data)]
        (dom/div #js {:className "form-group action-type-component"}
                 (dom/label nil "Action type")
                 (dom/select #js {:className "form-control"
                                  :defaultValue (name (:type data))
                                  :onChange #(callback (keyword (.. % -target -value)))}
                             (dom/option (option-element-map "email" (= :email (:type data)))  "Email")
                             (dom/option (option-element-map "noop" (= :noop (:type data))) "Noop")
                             (dom/option (option-element-map "github-issue" (= :github-issue (:type data))) "GitHub issue")))))))

(defn email-component [data owner]
  (reify
    om/IRender
    (render [this]
      (dom/div nil
               (dom/div #js {:className "form-group"}
                        (dom/label #js {:for "email-input"} "Email")
                        (dom/p #js {:id "email-input"}
                               (:email data)))
               (dom/div #js {:className "form-group"}
                        (dom/label #js {:for "emain-body-input"} "Email body")
                        (dom/textarea #js {:cols "40"
                                           :className "form-control"
                                           :id "emain-body-input"
                                           :value (:template data)
                                           :onChange #((:template-handler data) (.. % -target -value))}))))))

(defn github-issue-component [data owner]
  (reify
    om/IRender
    (render [this]
      (dom/div nil 
               (dom/div #js {:className "form-group"}
                        (dom/label nil "GitHub repository")
                        (dom/div #js {:className "form-inline"}
                                 (dom/input #js {:type "text"
                                                 :className "form-control"
                                                 :value (:value (:user data))
                                                 :placeholder "username or organization"
                                                 :onChange #((:handler (:user data)) (.. % -target -value))
                                                 })
                                 (dom/span nil " / ")
                                 (dom/input #js {:type "text"
                                                 :className "form-control"
                                                 :value (:value (:repo data))
                                                 :placeholder "repository"
                                                 :onChange #((:handler (:repo data)) (.. % -target -value))})))
               (dom/div #js {:className "form-group"}
                        (dom/label nil "Issue title")
                        (dom/input #js {:type "text"
                                        :className "form-control"
                                        :value (:value (:title data))
                                        :onChange #((:handler (:title data)) (.. % -target -value))}))
               (dom/div #js {:className "form-group"}
                        (dom/label nil "Issue body")
                        (dom/textarea #js {:cols "40"
                                           :className "form-control"
                                           :value (:value (:body data))
                                           :onChange #((:handler (:body data)) (.. % -target -value))}))))))

(defn action-input-form [data owner]
  (reify
    om/IRender
    (render [this]
      (dom/form nil
                (om/build artifact-input-component (:artifact-value data))
                (om/build action-type-component (:action-type data))

                (when (= :email (-> data :action-type :type))
                  (om/build email-component (:email data)))

                (when (= :github-issue (-> data :action-type :type))
                  (om/build github-issue-component (:github-issue data)))))))

(defn add-action-footer [data owner]
  (reify
    om/IRender
    (render [this]
      (let [project-id (:project-id data)
            artifact (:artifact-value data)
            type (:type data)
            email (:user-email data)
            template (:email-template data)
            gh-repo (:gh-repo data)
            gh-user (:gh-user data)
            gh-issue-title (:gh-issue-title data)
            gh-issue-body (:gh-issue-body data)
            data-pack {:type type
                       :project-id project-id
                       :artifact-value artifact
                       :gh-repo (str gh-user "/" gh-repo)
                       :gh-issue-title gh-issue-title
                       :gh-issue-body gh-issue-body
                       :user-email email
                       :email-template template}]
        (dom/div nil
                 (dom/button 
                  
                  #js {:className "btn btn-primary pull-left"
                       :onClick #(action/send-new-action data-pack)} "Submit")

                 (when-not (= :noop type)
                   (dom/button 
                    #js {:className "btn btn-default"
                         :onClick #(action/test-action data-pack)} "Test")))))))

(defn update-action-footer [data owner]
  (reify
    om/IRender
    (render [this]
      (let [project-id (:project-id data)
            artifact (:artifact-value data)
            action-id (:action-id data)
            type (:type data)
            email (:user-email data)
            template (:email-template data)
            gh-repo (:gh-repo data)
            gh-user (:gh-user data)
            gh-issue-title (:gh-issue-title data)
            gh-issue-body (:gh-issue-body data)
            data-pack {:type type
                       :project-id project-id
                       :artifact-value artifact
                       :action-id action-id
                       :gh-repo (str gh-user "/" gh-repo)
                       :gh-issue-title gh-issue-title
                       :gh-issue-body gh-issue-body
                       :user-email email
                       :email-template template}]
          (dom/div 
           nil
           (dom/button 
            #js {:className "btn btn-primary pull-left"
                 :onClick #(action/update-action data-pack)} "Update")

           (when-not (= :noop type)
             (dom/button 
              #js {:className "btn btn-default"
                   :onClick #(action/test-action data-pack)} "Test")))))))

(def default-email-template
  (str "Hello there\n\n"
       "{{library}} {{version}} has been released! "
       "Previous version was {{previous-version}}\n\n"
       "Your Hatnik"))

(def default-github-issue-title "Release {{library}} {{version}}")
(def default-github-issue-body "Time to update your project.clj to {{library}} {{version}}!")

(defmulti get-init-state #(:type %))

(def new-init-state {:artifact-value ""})
(def email-init-state {:email-template default-email-template})
(def github-issue-init-state 
  {:gh-repo "" 
   :gh-user ""
   :gh-issue-title default-github-issue-title 
   :gh-issue-body default-github-issue-body})

(defmethod get-init-state :add [data _] 
  (merge
   {:project-id (:project-id data)
    :email-template default-email-template
    :user-email (:user-email data)
    :type :email}
   new-init-state email-init-state github-issue-init-state))

(defmulti get-init-state-by-action #(get % "type"))
(defmethod get-init-state-by-action "noop" [action]
  (merge email-init-state github-issue-init-state))

(defmethod get-init-state-by-action "email" [action]
  (merge
   {:email-template (get action "template")}
    github-issue-init-state))

(defmethod get-init-state-by-action "github-issue" [action]
  (let [repo (split (get action "repo") "/")
        user (first repo)
        repository (second repo)]    
  (merge
   {:gh-issue-title (get action "title")
    :gh-issue-body (get action "body")
    :gh-user user
    :gh-repo repository}
   email-init-state)))

(defmethod get-init-state :update [data _]
  (let [action (:action data)]
    (merge {:type (keyword (get action "type"))
            :project-id (:project-id data)
            :user-email (:user-email data)
            :artifact-value (get action "library")
            :action-id (get action "id")}
           (get-init-state-by-action action))))

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

                            :action-type 
                            {:type (:type state)
                             :handler #(om/set-state! owner :type %)}

                            :github-issue
                            {:user {:value (:gh-user state)
                                    :handler #(om/set-state! owner :gh-user %)}
                             :repo {:value (:gh-repo state) 
                                    :handler #(om/set-state! owner :gh-repo %)}
                             :title {:value (:gh-issue-title state) 
                                     :handler #(om/set-state! owner :gh-issue-title %)}
                             :body {:value (:gh-issue-body state)
                                    :handler #(om/set-state! owner :gh-issue-body %)}}
                            
                            :email
                            {:template (:email-template state)
                             :template-handler #(om/set-state! owner :email-template %)
                             :email (:user-email state)}}))
        (dom/div #js {:className "modal-footer"}
                 (get-action-footer data state owner)))))

    om/IDidMount
    (did-mount [this]
      (let [modal-window ($ (:modal-jq-id data))]
        (.modal modal-window)))))

(defn show [& data-pack]
  (om/root add-action-component 
           (assoc 
               (into {} (map vec (partition-all 2 data-pack)))
             :modal-jq-id :#iModalAddAction)            
           {:target (.getElementById js/document "iModalAddAction")}))


