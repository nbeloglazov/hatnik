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
               (dom/div #js {:className "form-group"}
                        (dom/label #js {:className "col-sm-2 control-label"}
                                   "Address")
                        (dom/div #js {:className "col-sm-10"}
                                 (dom/p #js {:className "form-control-static"}
                                           (:email-address data))))
               (u/form-field {:data data
                              :field :title
                              :id "email-subject"
                              :title "Subject"
                              :validator schm/TemplateTitle
                              :type :text})
               (u/form-field {:data data
                              :field :body
                              :id "email-body"
                              :title "Body"
                              :validator schm/TemplateBody
                              :type :textarea})))))
