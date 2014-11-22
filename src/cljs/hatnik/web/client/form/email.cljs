(ns hatnik.web.client.form.email
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [hatnik.web.client.z-actions :as action]
            [hatnik.web.client.utils :as u]
            [hatnik.schema :as schm]))

(defn email-component [data owner]
  (reify
    om/IRender
    (render [this]
      (dom/div nil
               (dom/div #js {:className "form-group has-success"}
                        (dom/label #js {:className "col-sm-2 control-label no-padding-right"}
                                   "Address")
                        (dom/div #js {:className "col-sm-10"}
                                 (dom/p #js {:className "form-control-static"}
                                           (:email-address data))))
               (u/form-field {:data data
                              :field :title
                              :id "email-subject"
                              :title "Subject"
                              :validator schm/TemplateTitle
                              :type :text
                              :popover "supported variables: {{library}} {{version}} {{previous-version}}"})
               (u/form-field {:data data
                              :field :body
                              :id "email-body"
                              :title "Body"
                              :validator schm/TemplateBody
                              :type :textarea
                              :popover "supported variables: {{library}} {{version}} {{previous-version}}"})))))
