(ns hatnik.web.client.form.project-menu
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [hatnik.web.client.z-actions :as action]
            [hatnik.web.client.utils :as u]
            [hatnik.web.client.app-state :as app-state]
            [hatnik.web.client.form.add-action :as action-form]
            [hatnik.schema :as schm]
            [jayq.core :refer [$]]))

(defn header [project]
  (dom/div
   #js {:className "modal-header"}
   (dom/h4 #js {:className "modal-title"}
           (if (:id project)
             "Update project"
             "Create project")
           (when (u/mobile?)
             (dom/button
              #js {:className "btn btn-default close-btn"
                   :onClick #(.modal ($ :#iModalProjectMenu) "hide")}
              "Close")))))

(defn build-file-body-addition [project]
  [(u/form-field {:data project
                  :field :build-file
                  :id "project-name"
                  :title "Build file"
                  :type :text
                  :validator (schm/string-of-length 1 1028)})
   (dom/div
    #js {:className "form-group"}
    (dom/h4
     #js {:className "control-label col-sm-2 no-padding-right"}
     "Action"))
   (om/build action-form/action-input-form (:action project))])

(defn body [project]
  (dom/div
   #js {:className "modal-body"}
   (apply dom/form
    #js {:className "form-horizontal"}
    (u/form-field {:data project
                   :field :name
                   :id "project-name"
                   :title "Name"
                   :type :text
                   :validator (schm/string-of-length 1 128)})

    ; Project type radio button
    (dom/div
     #js {:className (str "form-group")}
     (dom/label #js {:htmlFor "project-type"
                     :className "control-label col-sm-2 no-padding-right"}
                "Type")
     (apply dom/div
            #js {:className "col-sm-10"}
            (for [[name value] [["regular" "regular"]
                                ["build file" "build-file"]]]
              (dom/label
               #js {:className "radio-inline"}
               (dom/input #js {:type "radio"
                               :checked (if (= (:type project) value)
                                          "checked"
                                          nil)
                               :value value
                               :onChange #(om/update! project :type value)})
               name))))

    (if (= (:type project) "build-file")
      (build-file-body-addition project)
      []))))

(defn footer [project]
  (let [id (:id project)]
    (dom/div
     #js {:className "modal-footer"}
     (dom/div
      #js {:className "btn btn-primary pull-left"
           :onClick (if id
                      #(action/update-project project)
                      #(action/create-project project))}
      (if id
        "Update"
        "Create"))
     (when id
       (dom/div #js {:className "btn btn-danger pull-right"
                     :onClick #(action/delete-project id)}
                "Delete")))))

(defn- project-menu [project owner]
  (reify
    om/IDidMount
    (did-mount [this]
      (.modal ($ :#iModalProjectMenu)))

    om/IRender
    (render [this]
      (dom/div
       #js {:className "modal-dialog"}
       (dom/div
        #js {:className "modal-content"}
        (header project)
        (body project)
        (footer project))))))

(defn adapt-action-in-project [project]
  (let [email (-> @app-state/app-state :user :email)
        action (:action project)
        type (:type action "email")]
    (assoc project :action
           (merge action-form/default-state
                  (if action
                    (action-form/get-state-from-action action)
                    {})
                  {:type type
                   :email-address email
                   :library "none"
                   :last-processed-version "none"}))))

(defn show [project]
  (om/root project-menu (adapt-action-in-project project)
           {:target (.getElementById js/document "iModalProjectMenu")}))
