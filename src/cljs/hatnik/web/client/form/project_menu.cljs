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
   (dom/form
    #js {:className "form-horizontal"}
    (u/form-field {:data data
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
                               :checked (if (= (:type data) value)
                                          "checked"
                                          nil)
                               :value value
                               :onChange #(om/update! data :type value)})
               name)))))))

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
