(ns hatnik.web.client.form.add-action
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [hatnik.web.client.z-actions :as action]
            [hatnik.web.client.utils :as u]
            [clojure.walk :refer [keywordize-keys]])
  (:use [jayq.core :only [$]]
        [hatnik.web.client.form.library-input :only [library-input-component]]
        [hatnik.web.client.form.action-type :only [action-type-component]]
        [hatnik.web.client.form.email :only [email-component]]
        [hatnik.web.client.form.github-issue :only [github-issue-component]]
        [hatnik.web.client.form.github-pull-request :only [github-pull-request-component]]))

(def action-components
  {"email" email-component
   "github-issue" github-issue-component
   "github-pull-request" github-pull-request-component})

(def test-button-tooltip
  {"email" "Send test email"
   "github-issue" "Create test issue"
   "github-pull-request" "Open test pull request"})

(defn action-from-project? [data]
  (= (:library data) (:project-id data) "none"))

(defn action-input-elements [data owner]
  (reify
    om/IRender
    (render [this]
      (dom/div
       nil
       (when-not (action-from-project? data)
         (om/build library-input-component data))
       (om/build action-type-component data)
       (when-let [component (action-components (:type data))]
         (om/build component data))))

    om/IDidUpdate
    (did-update [this prev-props prev-state]
      (when-not (u/mobile?)
        (.popover ($ "[data-toggle='popover']"))
        (.tooltip ($ "[data-toggle='tooltip']") "fixTitle")))

    om/IDidMount
    (did-mount [this]
      (when-not (u/mobile?)
        (.popover ($ "[data-toggle='popover']"))
        (.tooltip ($ "[data-toggle='tooltip']"))))))

(defn action-footer [data owner]
  (reify
    om/IInitState
    (init-state [this]
      {:test-in-progress? false})

    om/IRenderState
    (render-state [this state]
      (let [add? (nil? (:id data))]
        (dom/div nil
                 (dom/button

                  #js {:className "btn btn-primary pull-left"
                       :onClick #(if add?
                                   (action/send-new-action @data)
                                   (action/update-action @data))}
                  (if add? "Create" "Update"))

                 (when-not (= "noop" (:type data))
                   (if (:test-in-progress? state)
                     (dom/span #js {:className "test-spinner"}
                               (dom/img #js {:src "/img/ajax-loader.gif"
                                             :alt "Testing"
                                             :title "Testing"}))
                     (dom/button
                      #js {:className "btn btn-default"
                           :data-toggle "tooltip"
                           :data-placement "top"
                           :data-container "body"
                           :title (test-button-tooltip (:type data))
                           :onClick (fn []
                                      (om/set-state! owner :test-in-progress? true)
                                      (action/test-action @data
                                                          #(om/set-state! owner :test-in-progress? false)))}
                      "Test"))))))))

(def default-state
  {:library ""
   :library-version ""
   :body (str "{{library}} {{version}} has been released. "
              "Previous version was {{previous-version}}.")
   :title "{{library}} {{version}} released"
   :gh-repo ""
   :file-operations [{:file ""
                      :regex ""
                      :replacement ""}]
   :file-operation-type "manual"})

(defmulti get-state-from-action :type)

(defmethod get-state-from-action "noop" [action]
  {})

(defmethod get-state-from-action "email" [action]
  {:body (:body action)
   :title (:subject action)})

(defmethod get-state-from-action "github-issue" [action]
  {:title (:title action)
   :body (:body action)
   :gh-repo (:repo action)})

(defmethod get-state-from-action "github-pull-request" [action]
  (let [base {:body (:body action)
              :title (:title action)
              :gh-repo (:repo action)}]
    (if (string? (:operations action))
      (assoc base
             :file-operation-type (:operations action))
      (assoc base
             :file-operation-type "manual"
             :file-operations (keywordize-keys (:operations action))))))

(defmulti get-init-state :type)

(defmethod get-init-state :add [data _]
  (merge default-state
         {:project-id (:project-id data)
          :email-address (:user-email data)
          :type "email"}))

(defmethod get-init-state :update [data _]
  (let [action (:action data)]
    (merge default-state
           (get-state-from-action action)
           {:type (:type action)
            :project-id (:project-id data)
            :email-address (:user-email data)
            :library (:library action)
            :library-version (:last-processed-version action)
            :id (:id action)})))

(defn get-action-header [data]
  (let [add? (nil? (:id data))]
    (dom/h4 #js {:className "modal-title"}
            (if add? "Add new action" "Update action")
          (when (u/mobile?)
            (dom/button
             #js {:className "btn btn-default close-btn"
                  :onClick #(.modal ($ :#iModalAddAction) "hide")}
             "Close"))
          (when-not add?
            (dom/button
             #js {:className "btn btn-danger pull-right"
                  :onClick #(action/delete-action (:id @data))}
             "Delete")))))

(defn- add-action-component [data owner]
  (reify
    om/IRender
    (render [this]
      (dom/div
       #js {:className "modal-dialog"}
       (dom/div
        #js {:className "modal-content"}
        (dom/div #js {:className "modal-header"}
                 (get-action-header data))
        (dom/div #js {:className "modal-body"}
                 (dom/form #js {:className "form-horizontal"}
                           (om/build action-input-elements data)))
        (dom/div #js {:className "modal-footer"}
                 (om/build action-footer data)))))

    om/IDidMount
    (did-mount [this]
      (.modal ($ "#iModalAddAction")))))

(defn show [data]
  (om/root add-action-component
           (get-init-state data)
           {:target (.getElementById js/document "iModalAddAction")}))
