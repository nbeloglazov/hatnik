(ns hatnik.web.client.components
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [hatnik.web.client.app-state :as state])
  (:use [jayq.core :only [$]]))


(defn accordion-panel [& {:keys [body-id header body open] :or {:open true}}]
  (dom/div 
   #js {:className "panel panel-default"}
   (dom/div 
    #js {:className "panel-heading"}
    (dom/div 
     nil
     (dom/h4 #js {:className "panel-title"}
             (dom/a #js 
                    {:data-parent "#accrodion"
                     :data-toggle "collapse"
                     :href (str "#" body-id)} header))))
           (dom/div #js {:className (if open 
                                      "panel-collapse collapse in"
                                      "panel-collapse collapse")
                         :id body-id}
                    (dom/div #js {:className "panel-body"}
                             body))))

(defn render-action [project-id action]
  (dom/p nil
         (str
          (get action "artifact")
          " - "
          (get action "type"))))

(defn add-action [id]
  (state/set-form-type :email-action)
  (.modal ($ :#iModal)))

(defn add-project []
  (state/set-form-type :project-action)
  (.modal ($ :#iModal)))

(defn add-new-action [project-id]
  (dom/a #js {:className "btn btn-default"
              :onClick #(add-action project-id)}
         "Add new action"))


(defn actions-table [id actions]
  (let [rendered 
        (map (fn [action]
               (dom/div #js {:className "col-sm-6 col-md-4 col-lg-2"}
                        (render-action id action)))
             actions)]
    (apply dom/div #js {:className "row"}
           (concat rendered
                   [(dom/div #js {:className "col-sm-6 col-md-4 col-lg-2"} 
                             (add-new-action id))]))))                   

(defn project-header [project]
  (dom/div #js {:className "project-header"}
   (dom/p nil (get project "name"))
   (dom/span #js {:className "glyphicon glyphicon-pencil project-header-button"})))

(defn project-list [data owner]
  (reify
    om/IRender
    (render [this]
      (apply dom/div nil
       (map 
        (fn [prj]
          (accordion-panel
           :header (project-header prj)
           :body (actions-table (get prj "id") (get prj "actions"))
           :body-id (str "__" (get prj "name"))))
        (:projects data))))))

