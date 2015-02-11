(ns hatnik.web.client.form.project-menu
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [hatnik.web.client.z-actions :as action]
            [hatnik.web.client.utils :as u]
            [hatnik.web.client.app-state :as app-state]
            [hatnik.web.client.form.add-action :as action-form]
            [hatnik.schema :as schm]
            [jayq.core :refer [$]]))

(defn header [project]
  (dom/div
   #js {:className "modal-header"}
   (dom/h4 #js {:className "modal-title"}
           (if (:id project)
             "Update project"
             "Create project")
           (when (u/mobile?)
             (dom/button
              #js {:className "btn btn-default close-btn"
                   :onClick #(.modal ($ :#iModalProjectMenu) "hide")}
              "Close")))))

(defn update-build-file-field [project value]
  (let [gh-repo (-> project :action :gh-repo)
        new-gh-repo (if (= gh-repo (:build-file project))
                      value
                      gh-repo)]
    (-> project
        (assoc :build-file value)
        (assoc-in [:action :gh-repo] new-gh-repo))))

(defn build-file-body-addition [project]
  [(u/form-field {:data project
                  :field :build-file
                  :id "build-file"
                  :title "Build file"
                  :type :text
                  :validator (schm/string-of-length 1 1028)
                  :placeholder "github username/repo or URL to project.clj"
                  :on-change (fn [event]
                               (om/transact! project
                                             #(update-build-file-field % (u/ev-value event))))})
   (dom/div
    #js {:className "form-group"}
    (dom/h4
     #js {:className "control-label col-sm-2 no-padding-right"}
     "Action"))
   (om/build action-form/action-input-elements (:action project))])

(defn body [project]
  (dom/div
   #js {:className "modal-body"}
   (apply dom/form
    #js {:className "form-horizontal"}
    (u/form-field {:data project
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
            (for [[name value tooltip] [["build file" "build-file"
                                         (str "Libraries extracted from build file. "
                                              "They all share same settings.")]
                                        ["manual" "regular"
                                         (str "Action for each library created manually. "
                                              "Each action has its own settings.")]]]
              (dom/label
               #js {:className "radio-inline"
                    :data-title tooltip
                    :data-toggle "tooltip"
                    :data-container "body"}
               (dom/input #js {:type "radio"
                               :checked (if (= (:type project) value)
                                          "checked"
                                          nil)
                               :value value
                               :disabled (if (:id project) "disabled" "")
                               :onChange #(om/update! project :type value)})
               name))))

    (if (= (:type project) "build-file")
      (build-file-body-addition project)
      []))))

(defn footer [project state owner]
  (let [id (:id project)
        action-text (if id "Update" "Create")]
    (dom/div
     #js {:className "modal-footer"}
     (if (:test-in-progress? state)
       (dom/span #js {:className "test-spinner pull-left"}
                 (dom/img #js {:src "/img/ajax-loader.gif"
                               :alt action-text
                               :title action-text}))
       (dom/div
        #js {:className "btn btn-primary pull-left"
             :onClick (fn []
                        (let [done-callback #(om/set-state!
                                              owner :test-in-progress? false)]
                          (om/set-state! owner :test-in-progress? true)
                          (if id
                            (action/update-project project done-callback)
                            (action/create-project project done-callback))))}
        action-text))
     (when id
       (dom/div #js {:className "btn btn-danger pull-right"
                     :onClick #(action/delete-project id)}
                "Delete")))))

(defn- project-menu [project owner]
  (reify
    om/IInitState
    (init-state [this]
      {:test-in-progress? false})

    om/IDidUpdate
    (did-update [this prev-props prev-state]
      (when-not (u/mobile?)
        (.tooltip ($ "[data-toggle='tooltip']") "fixTitle")))

    om/IDidMount
    (did-mount [this]
      (.modal ($ :#iModalProjectMenu))
      (when-not (u/mobile?)
        (.tooltip ($ "[data-toggle='tooltip']"))))

    om/IRenderState
    (render-state [this state]
      (dom/div
       #js {:className "modal-dialog"}
       (dom/div
        #js {:className "modal-content"}
        (header project)
        (body project)
        (footer project state owner))))))

(defn adapt-action-in-project [project]
  (let [email (-> @app-state/app-state :user :email)
        action (:action project)
        type (:type action "email")]
    (assoc project :action
           (merge action-form/default-state
                  (if action
                    (action-form/get-state-from-action action)
                    {})
                  {:type type
                   :email-address email
                   :project-id "none"
                   :library {:name "none"
                             :type "jvm"}
                   :file-operation-type "project.clj"}))))

(defn show [project]
  (om/root project-menu (adapt-action-in-project project)
           {:target (.getElementById js/document "iModalProjectMenu")}))
