(ns hatnik.web.client.form.project-menu
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [hatnik.web.client.z-actions :as action]
            [hatnik.web.client.utils :as u]
            [hatnik.schema :as schm]
            [jayq.core :refer [$]]))

(defn header [data]
  (dom/div
   #js {:className "modal-header"}
   (dom/h4 #js {:className "modal-title"}
           (if (:project-id data)
             "Update project"
             "Create project")
           (when (u/mobile?)
             (dom/button
              #js {:className "btn btn-default close-btn"
                   :onClick #(.modal ($ :#iModalProjectMenu) "hide")}
              "Close")))))

(defn body [data]
  (dom/div
   #js {:className "modal-body"}
   (dom/form #js {:className "form-horizontal"}
             (u/form-field {:data data
                            :field :name
                            :title "Name"
                            :type :text
                            :validator (schm/string-of-length 1 128)}))))

(defn footer [data]
  (let [id (:project-id data)
        name (:name data)]
    (dom/div
     #js {:className "modal-footer"}
     (dom/div
      #js {:className "btn btn-primary pull-left"
           :onClick (if id
                      #(action/update-project id name)
                      #(action/send-new-project-request name))}
      (if id
        "Update"
        "Create"))
     (when id
       (dom/div #js {:className "btn btn-danger pull-right"
                     :onClick #(action/delete-project id)}
                "Delete")))))

(defn- project-menu [data owner]
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
        (header data)
        (body data)
        (footer data))))))

(defn show [project]
  (om/root project-menu project
           {:target (.getElementById js/document "iModalProjectMenu")}))
