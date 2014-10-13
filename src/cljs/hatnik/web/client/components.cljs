(ns hatnik.web.client.components
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [hatnik.web.client.app-state :as state]
            [hatnik.web.client.form.add-action :as add-action])
  (:use [jayq.core :only [$]]))

(defn ^:export add-new-project []
  (let [project-name-input (.getElementById js/document "project-name-input")]
    (set! (.-value project-name-input) "")
    (.modal ($ :#iModalProject))))

(defn render-action-type [a-type]
  (when (= "email" a-type)
    (dom/span #js {:className "glyphicon glyphicon-envelope action-type"})))

(defn render-action [project-id action]
  (let [name (get action "library")
        template (get action "template")]
  (dom/div
   #js {:onClick 
        (fn []
          (state/set-form-type :email-edit-action)
          (state/set-current-project project-id)
          (state/set-current-action action)
          (state/set-current-artifact-value name)
          (state/set-current-email-template template)
          (.modal ($ :#iModal)))
        :className "panel panel-default action"}
   (dom/div
    #js {:className "panel-body bg-success"}
    (render-action-type (get action "type"))
    (dom/span #js {:className "action-info"}
              (dom/div #js {:className "library-name"}
                       (get action "library"))
              (dom/div #js {:className "version"}
                       (get action "last-processed-version")))))))

(defn add-new-action [project-id]
  (dom/div #js {:className "panel panel-default panel-info action add-action"
                :onClick #(add-action/show project-id)}
           (dom/div #js {:className "panel-body bg-info"}
                    (dom/span #js {:className "glyphicon glyphicon-plus"})
                    " Add action")))


(defn actions-table [id actions]
  (let [actions (->> actions
                     (sort-by first)
                     (map second))
        rendered
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

(defn project-view [prj owner]
  (reify
    om/IRender
    (render [_]
      (dom/div
       #js {:className "panel panel-default panel-primary"}
       (dom/div
        #js {:className "panel-heading"}
        (dom/div
         nil
         (dom/h4
          #js {:className "panel-title"}
          (dom/div #js {:className "row"}
                   (dom/div #js {:className "col-sm-8 col-md-8 col-lg-8"}
                            (dom/a
                             #js {:data-parent "#accrodion"
                                  :data-toggle "collapse"
                                  :href (str "#" (str "__PrjList" (get prj "id")))}
                             (dom/div #js {:className "bg-primary"} (get prj "name"))))
                   (dom/div
                    #js {:className "col-sm-2 col-md-1 col-lg-1 pull-right"}
                    (project-header-menu-button prj))))))
       (dom/div #js {:className "panel-collapse collapse in"
                     :id (str "__PrjList" (get prj "id"))}
                (dom/div #js {:className "panel-body"}
                         (actions-table (get prj "id") (get prj "actions"))))))))

(defn project-list [data owner]
  (reify
    om/IRender
    (render [this]
      (apply 
       dom/div nil
       (map #(om/build project-view %)
            (->> (-> data :data :projects)
                 (sort-by first)
                 (map second)))))))

