(ns hatnik.web.client.components
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [hatnik.web.client.app-state :as state]
            [hatnik.web.client.form.add-action :as add-action]
            [hatnik.web.client.form.project-menu :as pmenu]
            [hatnik.web.client.z-actions :as action])
  (:use [jayq.core :only [$]]))

(defn ^:export add-new-project []
  (pmenu/show {:name ""}))

(defn add-new-action-card [data owner]
  (reify
    om/IRender
    (render [this]
      (let [id (:project-id data)
            email (:user-email data)]
      (dom/div #js {:className "panel panel-default action add-action"
                    :onClick #(add-action/show {:type :add
                                                :project-id id
                                                :user-email email})}
               (dom/div #js {:className "panel-body"}
                        (dom/span #js {:className "glyphicon glyphicon-plus"})
                        " Add action"))))))

(def action-types
  {"noop" {:text "noop"
           :icon nil
           :alt nil}
   "email" {:text "email"
            :icon "/img/email-icon.png"
            :alt "email"}
   "github-issue" {:text "issue"
                   :icon "/img/github-icon.png"
                   :alt "github"}
   "github-pull-request" {:text "pull request"
                          :icon "/img/github-icon.png"
                          :alt "github"}})

(defn render-action-type [type]
  (let [{:keys [text icon alt]} (action-types type)]
    (dom/div nil
      (when icon
        (dom/img #js {:src icon
                      :className "action-icon"
                      :alt alt
                      :title alt}))
      (dom/span #js {:className "action-name"}
                text))))

(defn action-card [data owner]
  (reify
    om/IRender
    (render [this]
      (let [id (:project-id data)
            email (:user-email data)
            library (get data "library")
            library-class (if (< (count library) 27)
                            "" ; regular class
                            "long-name")]
        (dom/div #js {:className "panel panel-default action"
                      :onClick #(add-action/show {:type :update
                                                  :project-id id
                                                  :user-email email
                                                  :action @data})}
                 (dom/div #js {:className "panel-body"}
                   (render-action-type (:type data))
                   (dom/div #js {:className (str "library-name " library-class)
                                 :title library}
                            library)
                   (dom/div #js {:className "version"}
                            (get data "last-processed-version"))))))))

(defn render-action [data]
  (if (= (:type data) :add)
    (om/build add-new-action-card data)
    (om/build action-card data)))

(defn actions-table [id actions email]
  (let [actions (->> actions
                     (sort-by first)
                     (map second))
        rendered
        (map (fn [act]
               (dom/div #js {:className "col-sm-6 col-md-4 col-lg-3 prj-list-item"}
                        (render-action (assoc act :project-id id
                                              :type (get act "type")
                                              :user-email email))))
             actions)]
    (apply dom/div #js {:className "row"}
           (concat rendered
                   [(dom/div #js {:className "col-sm-6 col-md-4 col-lg-3 prj-list-item"}
                             (render-action {:type :add
                                             :project-id id
                                             :user-email email}))]))))

(defn project-header-menu-button [project]
  (let [id (get project "id")
        name (get project "name")]
    (dom/div #js {:className "dropdown pull-right"}
             (dom/button
              #js {:className "btn btn-default"
                   :type "button"
                   :onClick #(pmenu/show {:project-id id
                                          :name name})}
              (dom/span #js {:className "glyphicon glyphicon-pencil pull-right"})))))

(defn project-view [prj owner]
  (reify
    om/IRender
    (render [_]
      (let [id (str "__PrjList" (get prj "id"))]
       (dom/div
        #js {:className "panel panel-default panel-primary"}
        (dom/div
         #js {:className "panel-heading clearfix"}
         (dom/h4
          #js {:className "panel-title pull-left project-name"}
          (dom/a
           #js {:data-toggle "collapse"
                :href (str "#" id)}
           (dom/div #js {:className "bg-primary"} (get prj "name"))))
         (project-header-menu-button prj))
        (dom/div #js {:className "panel-collapse collapse in"
                      :id id}
                 (dom/div #js {:className "panel-body"}
                          (actions-table (get prj "id") 
                                         (get prj "actions") 
                                         (-> prj :user :email)))))))))

(defn project-list [data owner]
  (reify
    om/IRender
    (render [this]
      (let [project-data (->> (:projects data)
                              (sort-by first)
                              (map second))
            user-data (:user data)]
        (apply dom/div #js {:className "panel-group" :id "iProjectList"}
               (map #(om/build project-view (assoc % :user user-data))
                    project-data))))))


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
                            :onClick add-new-project} "Add project"))
       (dom/div #js {:className "col-md-10"}))

      (dom/div nil
               (dom/p nil "")
               (om/build project-list data))))))
