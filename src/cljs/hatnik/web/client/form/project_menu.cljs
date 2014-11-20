(ns hatnik.web.client.form.project-menu
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [hatnik.web.client.z-actions :as action]
            [hatnik.web.client.utils :as u]
            [hatnik.schema :as schm])
  (:use [jayq.core :only [$]]))


(defn- project-menu [data owner]
  (reify
    om/IDidMount
    (did-mount [this]
      (.modal ($ :#iModalProjectMenu)))

    om/IRender
    (render [this]
      (let [name (:name data)
            id (:project-id data)]
        (dom/div
         #js {:className "modal-dialog"}
         (dom/div
          #js {:className "modal-content"}
          (dom/div #js {:className "modal-header"}
                   (dom/h4 #js {:className "modal-title"}
                           (if id
                             "Update project"
                             "Create project")
                           (when (u/mobile?)
                             (dom/button
                              #js {:className "btn btn-default close-btn"
                                   :onClick #(.modal ($ :#iModalProjectMenu) "hide")}
                              "Close"))))

          (dom/div
           #js {:className "modal-body"}
           (dom/form nil
                     (dom/div #js {:className (str "form-group "
                                                   (u/validate (schm/string-of-length 1 128) name))}
                              (dom/input #js {:className "form-control"
                                              :type "text"
                                              :value name
                                              :onChange #(om/update! data :name (.. % -target -value))}))))

          (dom/div #js {:className "modal-footer"}

                   (dom/div #js {:className "btn btn-primary pull-left"
                                 :onClick (if id
                                            #(action/update-project id name)
                                            #(action/send-new-project-request name))}
                            (if id
                              "Update"
                              "Create"))
                   (when id
                     (dom/div #js {:className "btn btn-danger pull-right"
                                   :onClick #(action/delete-project id)}
                              "Delete")))))))))

(defn show [project]
  (om/root project-menu project
           {:target (.getElementById js/document "iModalProjectMenu")}))
