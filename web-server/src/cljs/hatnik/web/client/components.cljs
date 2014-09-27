(ns hatnik.web.client.components
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))


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
         (get action "type")))

(defn test-action [id]
  (js/alert (str "Add new action for project id " id)))


(defn add-new-action [project-id]
  (dom/a #js {:className "btn btn-default"
              :onClick #(test-action project-id)}
         "Add new action"))


(defn actions-table [id actions]
  (let [rendered 
        (map (fn [action]
               (dom/div #js {:className "col-md-4"}
                        (render-action id action)))
             actions)]
    (apply dom/div #js {:className "row"}
           (concat rendered
                   [(dom/div #js {:className "col-md-4"} 
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
