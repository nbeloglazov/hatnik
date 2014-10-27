(ns hatnik.web.client.form.add-action
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [hatnik.web.client.z-actions :as action]
            [hatnik.schema :as schm]
            [schema.core :as s])
  (:use [jayq.core :only [$]]
        [hatnik.web.client.form.artifact-input :only [artifact-input-component]]
        [hatnik.web.client.form.action-type :only [action-type-component]]
        [hatnik.web.client.form.email :only [email-component]]
        [hatnik.web.client.form.github-issue :only [github-issue-component]]))

(defn on-modal-close [component]
  (om/detach-root
   (om/get-node component)))

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
      (let [; TODO: simplify code below
            project-id (:project-id data)
            artifact (:artifact-value data)
            type (:type data)
            email-body (:email-body data)
            gh-repo (:gh-repo data)
            gh-issue-title (:gh-issue-title data)
            gh-issue-body (:gh-issue-body data)
            data-pack {:type type
                       :project-id project-id
                       :artifact-value artifact
                       :gh-repo gh-repo
                       :gh-issue-title gh-issue-title
                       :gh-issue-body gh-issue-body
                       :email-body email-body}]
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
      (let [; TODO: simplify code below
            project-id (:project-id data)
            artifact (:artifact-value data)
            action-id (:action-id data)
            type (:type data)
            email-body (:email-body data)
            gh-repo (:gh-repo data)
            gh-issue-title (:gh-issue-title data)
            gh-issue-body (:gh-issue-body data)
            data-pack {:type type
                       :project-id project-id
                       :artifact-value artifact
                       :action-id action-id
                       :gh-repo gh-repo
                       :gh-issue-title gh-issue-title
                       :gh-issue-body gh-issue-body
                       :email-body email-body}]
          (dom/div
           nil
           (dom/button
            #js {:className "btn btn-primary pull-left"
                 :onClick #(action/update-action data-pack)} "Update")

           (when-not (= :noop type)
             (dom/button
              #js {:className "btn btn-default"
                   :onClick #(action/test-action data-pack)} "Test")))))))

(def default-email-body
  (str "Hello there\n\n"
       "{{library}} {{version}} has been released! "
       "Previous version was {{previous-version}}\n\n"
       "Your Hatnik"))

(def default-github-issue-title "Release {{library}} {{version}}")
(def default-github-issue-body "Time to update your project.clj to {{library}} {{version}}")

(defmulti get-init-state #(:type %))

(def new-init-state {:artifact-value ""})
(def email-init-state {:email-body default-email-body})
(def github-issue-init-state
  {:gh-repo ""
   :gh-issue-title default-github-issue-title
   :gh-issue-body default-github-issue-body})

(defmethod get-init-state :add [data _]
  (merge
   {:project-id (:project-id data)
    :email-body default-email-body
    :user-email (:user-email data)
    :type :email}
   new-init-state email-init-state github-issue-init-state))

(defmulti get-init-state-by-action #(get % "type"))
(defmethod get-init-state-by-action "noop" [action]
  (merge email-init-state github-issue-init-state))

(defmethod get-init-state-by-action "email" [action]
  (merge
   {:email-body (get action "body")}
    github-issue-init-state))

(defmethod get-init-state-by-action "github-issue" [action]
  (merge
   {:gh-issue-title (get action "title")
    :gh-issue-body (get action "body")
    :gh-repo (get action "repo")}
   email-init-state))

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
                            {:repo {:value (:gh-repo state)
                                    :handler #(om/set-state! owner :gh-repo %)}
                             :title {:value (:gh-issue-title state)
                                     :handler #(om/set-state! owner :gh-issue-title %)}
                             :body {:value (:gh-issue-body state)
                                    :handler #(om/set-state! owner :gh-issue-body %)}}

                            :email
                            {:body (:email-body state)
                             :body-handler #(om/set-state! owner :email-body %)
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
