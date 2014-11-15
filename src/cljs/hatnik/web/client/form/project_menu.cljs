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
      (let [modal-window ($ (:modal-jq-id data))]
        (.modal modal-window)))

    om/IRender
    (render [this]
      (let [name (:name data)
            id (:project-id data)]
        (dom/div
         #js {:className "modal-dialog"}
         (dom/div
          #js {:className "modal-content"}
          (dom/div #js {:className "modal-header"}
                   (dom/h4 #js {:className "modal-title"} "Project menu"))

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
                                 :onClick #(action/update-project id name)}
                            "Update")
                   (dom/div #js {:className "btn btn-danger pull-right"
                                 :onClick #(action/delete-project id)}
                            "Delete"))))))))

(defn show [& data-pack]
  (om/root project-menu (assoc (into {} (map vec (partition-all 2 data-pack)))
                          :modal-jq-id :#iModalProjectMenu)
           {:target (.getElementById js/document "iModalProjectMenu")}))
