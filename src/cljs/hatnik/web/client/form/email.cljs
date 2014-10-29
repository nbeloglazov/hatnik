(ns hatnik.web.client.form.email
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [hatnik.web.client.z-actions :as action]
            [hatnik.schema :as schm]
            [schema.core :as s]))

(defn email-component [data owner]
  (reify
    om/IInitState
    (init-state [this]
      {:status "has-success"})

    om/IRenderState
    (render-state [this state]
      (dom/div nil
               (dom/div #js {:className "form-group"}
                        (dom/label nil "Email")
                        (dom/p #js {:id "email-input"}
                               (:email data)))
               (dom/div #js {:className (str "form-group " (:subject-status state))}
                        (dom/label #js {:htmlFor "email-subject-input"} "Subject")
                        (dom/input #js {:type "text"
                                        :className "form-control"
                                        :id "email-subject-input"
                                        :value (-> data :subject :value)
                                        :onChange #(do
                                                     ((-> data :subject :handler) (.. % -target -value))
                                                     (om/set-state! owner :subject-status
                                                                    (if (s/check schm/TemplateTitle (.. % -target -value))
                                                                      "has-error"
                                                                      "has-success")))}))
               (dom/div #js {:className (str "form-group " (:body-status state))}
                        (dom/label #js {:htmlFor "email-body-input"} "Body")
                        (dom/textarea #js {:cols "40"
                                           :className "form-control"
                                           :id "email-body-input"
                                           :value (-> data :body :value)
                                           :onChange #(do
                                                        ((-> data :body :handler) (.. % -target -value))
                                                        (om/set-state! owner :body-status
                                                                       (if (s/check schm/TemplateBody (.. % -target -value))
                                                                         "has-error"
                                                                         "has-success")))}))))))
