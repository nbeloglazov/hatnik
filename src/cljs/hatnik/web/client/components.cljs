(ns hatnik.web.client.components
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [hatnik.web.client.app-state :as state])
  (:use [jayq.core :only [$]]))

(defn test-action []
  (js/alert "Hoooo!"))


(defn accordion-panel [& {:keys [body-id header body]}]
  (dom/div 
   #js {:className "panel panel-default panel-primary"}
   (dom/div 
    #js {:className "panel-heading"}
    (dom/div 
     nil     
     (dom/h4 
      #js {:className "panel-title"}
      (dom/a 
       #js {:data-parent "#accrodion"
            :data-toggle "collapse"
            :href (str "#" body-id)}
       header))))
   (dom/div #js {:className "panel-collapse collapse in"
                 :id body-id}
            (dom/div #js {:className "panel-body"}
                     body))))

(defn render-action-type [a-type]
  (when (= "email" a-type)
    (dom/span #js {:className "glyphicon glyphicon-envelope"})))

(defn render-action [project-id action]
  (dom/div
   #js {:onClick (fn []
                   (state/set-form-type :email-edit-action)
                   (state/set-current-project project-id)
                   (state/set-current-action action)
                   (.modal ($ :#iModal)))
        :className "panel panel-default"}
   (dom/div #js {:className "panel-body"}
            (render-action-type (get action "type"))
            (str " "
                 (get action "library")
                 ": " (get action "last-processed-version")))))

(defn add-action [id]
  (state/set-form-type :email-action)
  (state/set-current-project id)
  (.modal ($ :#iModal)))


(defn add-new-action [project-id]
  (dom/div #js {:className "panel panel-default panel-info"
              :onClick #(add-action project-id)}
           (dom/div #js {:className "panel-body"}
                    "Add new action")))


(defn actions-table [id actions]
  (let [rendered 
        (map (fn [action]
               (dom/div #js {:className "col-sm-12 col-md-6 col-lg-4 prj-list-item"}
                        (render-action id action)))
             actions)]
    (apply dom/div #js {:className "row"}
           (concat rendered
                   [(dom/div #js {:className "col-sm-12 col-md-6 col-lg-4 prj-list-item"} 
                             (add-new-action id))]))))   

(defn project-menu [project]
  (let [project-name-input (.getElementById js/document "project-name-edit-input")]
    (set! (.-value project-name-input) 
          (get @project "name"))
    (state/set-current-project (get @project "id"))
    (.modal ($ :#iModalProjectMenu))))

(defn project-header-menu-button [project]
  (dom/div #js {:className "dropdown"}
           (dom/button 
            #js {:className "btn btn-default"
                 :type "button"
                 :onClick #(project-menu project)}
            (dom/span #js {:className "glyphicon glyphicon-pencil pull-right"}))))

(defn project-header [project]
  (dom/div #js {:className "row"}
   (dom/div 
    #js {:className "col-sm-8 col-md-8 col-lg-8"} 
    (dom/p nil (get project "name")))
   (dom/div 
    #js {:className "col-sm-2 col-md-1 col-lg-1 pull-right"}
    (project-header-menu-button project))))

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
           :body-id (str "__PrjList" (get prj "id"))))
        (:projects data))))))

