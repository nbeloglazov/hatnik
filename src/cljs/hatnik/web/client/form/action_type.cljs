(ns hatnik.web.client.form.action-type
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [hatnik.web.client.z-actions :as action]
            [hatnik.schema :as schm]
            [schema.core :as s]))

(defn option-element-map [val pred]
  (if pred
    #js {:className "form-control" :selected "selected" :value val}
    #js {:className "form-control" :value val}))

(defn action-type-component [data owner]
  (reify
    om/IRender
    (render [this]
      (let [callback (:handler data)]
        (dom/div #js {:className "form-group action-type-component"}
                 (dom/label #js {:htmlFor "action-type"} "Action type")
                 (dom/select #js {:className "form-control"
                                  :id "action-type"
                                  :defaultValue (name (:type data))
                                  :onChange #(callback (keyword (.. % -target -value)))}
                             (dom/option (option-element-map "email" (= :email (:type data)))  "Email")
                             (dom/option (option-element-map "noop" (= :noop (:type data))) "Noop")
                             (dom/option (option-element-map "github-issue" (= :github-issue (:type data))) "GitHub issue")))))))

