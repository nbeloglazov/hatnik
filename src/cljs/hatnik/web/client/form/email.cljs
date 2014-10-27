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
                        (dom/label #js {:for "email-input"} "Email")
                        (dom/p #js {:id "email-input"}
                               (:email data)))
               (dom/div #js {:className (str "form-group " (:status state))}
                        (dom/label #js {:for "emain-body-input"} "Email body")
                        (dom/textarea #js {:cols "40"
                                           :className "form-control"
                                           :id "emain-body-input"
                                           :value (:body data)
                                           :onChange #(do
                                                        ((:body-handler data) (.. % -target -value))
                                                        (om/set-state! owner :status
                                                                       (if (s/check schm/TemplateBody (.. % -target -value))
                                                                         "has-error"
                                                                         "has-success")))}))))))
