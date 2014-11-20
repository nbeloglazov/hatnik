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

(defn action-input-form [data owner]
  (reify
    om/IRender
    (render [this]
      (dom/form #js {:className "form-horizontal"}
                (om/build library-input-component data)
                (om/build action-type-component data)

                (when (= "email" (:type data))
                  (om/build email-component data))

                (when (= "github-issue" (:type data))
                  (om/build github-issue-component data))

                (when (= "github-pull-request" (:type data))
                  (om/build github-pull-request-component data))))))

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

                 (when-not (= :noop (:type data))
                   (if (:test-in-progress? state)
                     (dom/span #js {:className "test-spinner"}
                               (dom/img #js {:src "/img/ajax-loader.gif"
                                             :alt "Testing"
                                             :title "Testing"}))
                     (dom/button
                      #js {:className "btn btn-default"
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
                      :replacement ""}]})

(defmulti get-state-from-action #(get % "type"))

(defmethod get-state-from-action "noop" [action]
  {})

(defmethod get-state-from-action "email" [action]
  {:body (get action "body")
   :title (get action "subject")})

(defmethod get-state-from-action "github-issue" [action]
  {:title (get action "title")
   :body (get action "body")
   :gh-repo (get action "repo")})

(defmethod get-state-from-action "github-pull-request" [action]
  {:body (get action "body")
   :title (get action "title")
   :file-operations (keywordize-keys (get action "operations"))
   :gh-repo (get action "repo")})

(defmulti get-init-state #(:type %))

(defmethod get-init-state :add [data _]
  (merge default-state
         {:project-id (:project-id data)
          :email-address (:user-email data)
          :type :email}))

(defmethod get-init-state :update [data _]
  (let [action (:action data)]
    (merge default-state
           (get-state-from-action action)
           {:type (get action "type")
            :project-id (:project-id data)
            :email-address (:user-email data)
            :library (get action "library")
            :library-version (get action "last-processed-version")
            :id (get action "id")})))

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
                 (om/build action-input-form data))
        (dom/div #js {:className "modal-footer"}
                 (om/build action-footer data)))))

    om/IDidMount
    (did-mount [this]
      (.modal ($ "#iModalAddAction")))))

(defn show [data]
  (om/root add-action-component
           (get-init-state data)
           {:target (.getElementById js/document "iModalAddAction")}))
