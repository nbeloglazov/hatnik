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
                        (dom/label nil "Address")
                        (dom/p #js {:id "email-input"}
                               (:email data)))
               (dom/div #js {:className (str "form-group " (u/validate schm/TemplateTitle
                                                                       (:title data)))}
                        (dom/label #js {:htmlFor "email-subject-input"
                                        :className "control-label"} "Subject")
                        (dom/input #js {:type "text"
                                        :className "form-control"
                                        :id "email-subject-input"
                                        :value (:title data)
                                        :onChange #(om/update! data :title (.. % -target -value))}))
               (dom/div #js {:className (str "form-group " (u/validate schm/TemplateBody
                                                                       (:body data)))}
                        (dom/label #js {:htmlFor "email-body-input"
                                        :className "control-label"} "Body")
                        (dom/textarea #js {:cols "40"
                                           :className "form-control"
                                           :id "email-body-input"
                                           :value (:body data)
                                           :onChange #(om/update! data :body (.. % -target -value))}))))))
