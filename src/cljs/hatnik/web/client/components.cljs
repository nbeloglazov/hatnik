(ns hatnik.web.client.components
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [hatnik.web.client.app-state :as state]
            [hatnik.web.client.form.add-action :as add-action]
            [hatnik.web.client.z-actions :as action])
  (:use [jayq.core :only [$]]))

(defn ^:export add-new-project []
  (let [project-name-input (.getElementById js/document "project-name-input")]
    (set! (.-value project-name-input) "")
    (.modal ($ :#iModalProject))))

(defn render-action-type [a-type]
  (when (= "email" a-type)
    (dom/span #js {:className "glyphicon glyphicon-envelope action-type"})))

(defn add-new-action-card [data owner]
  (reify
    om/IRender
    (render [this]
      (let [id (:project-id data)
            email (:user-email data)]
      (dom/div #js {:className "panel panel-default panel-info action add-action"
                    :onClick #(add-action/show :type :add 
                                               :project-id id
                                               :user-email email)}
               (dom/div #js {:className "panel-body bg-info"}
                        (dom/span #js {:className "glyphicon glyphicon-plus"})
                        " Add action"))))))

(defn email-action-card [data owner]
  (reify
    om/IRender
    (render [this]
      (let [id (:project-id data)]
      (dom/div #js {:className "panel panel-default action"
                    :onClick #(add-action/show :type :update
                                               :project-id id
                                               :action @data)}
               (dom/div 
                #js {:className "panel-body bg-success"}
                (dom/span #js {:className "glyphicon glyphicon-envelope action-type"})
                (dom/span #js {:className "action-info"}
                          (dom/div #js {:className "library-name"}
                                   (get data "library"))
                          (dom/div #js {:className "version"}
                                   (get data "last-processed-version")))))))))

(defn noop-action-card [data owner]
  (reify
    om/IRender
    (render [this]
      (let [id (:project-id data)]
        (dom/div #js {:className "panel panel-default action"
                      :onClick #(add-action/show :type :update
                                                 :project-id id
                                                 :action @data)}
                 (dom/div 
                  #js {:className "panel-body bg-success"}
                  (dom/span #js {:className "action-info"}
                            (dom/div #js {:className "library-name"}
                                     (get data "library"))
                            (dom/div #js {:className "version"}
                                     (get data "last-processed-version")))))))))

(defmulti render-action #(:type %))
(defmethod render-action "email" [data] (om/build email-action-card data))
(defmethod render-action "noop" [data] (om/build noop-action-card data))
(defmethod render-action :add [data] (om/build add-new-action-card data))

(defn actions-table [id actions email]
  (let [actions (->> actions
                     (sort-by first)
                     (map second))
        rendered
        (map (fn [act]
               (dom/div #js {:className "col-sm-12 col-md-6 col-lg-4 prj-list-item"}
                        (render-action (assoc act :project-id id :type (get act "type")))))
             actions)]
    (apply dom/div #js {:className "row"}
           (concat rendered
                   [(dom/div #js {:className "col-sm-12 col-md-6 col-lg-4 prj-list-item"}
                             (render-action {:type :add 
                                             :project-id id
                                             :user-email email}))]))))

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
                         (actions-table (get prj "id") 
                                        (get prj "actions") 
                                        (-> prj :user :email))))))))

(defn project-list [data owner]
  (reify
    om/IRender
    (render [this]
      (let [project-data (->> (:projects data)
                              (sort-by first)
                              (map second))
            user-data (:user data)]
        (dom/div #js {:className "panel-group" :id "iProjectList"}   
                 (apply dom/div nil
                        (map #(om/build project-view (assoc % :user user-data))
                             project-data)))))))


(defn app-view [data owner]
  (reify

    om/IWillMount
    (will-mount [this]
      (.send goog.net.XhrIo "/api/current-user" state/update-user-data)
      (.send goog.net.XhrIo "/api/projects" state/update-projects-list))

    om/IRender 
    (render [this]
      (dom/div nil
      (dom/div 
       #js {:className "row"}
       (dom/div #js {:className "col-md-2"}
                (dom/a #js {:className "btn btn-success"
                            :onClick add-new-project} "Add new project"))
       (dom/div #js {:className "col-md-10"}))

      (dom/div nil
               (dom/p nil "")
               (om/build project-list data))))))
