(ns hatnik.web.client.form.action-type
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [hatnik.web.client.z-actions :as action]
            [hatnik.web.client.utils :as u]
            [hatnik.schema :as schm]
            [schema.core :as s]))

(defn option-element-map [val cur]
  (if (= val cur)
    #js {:className "form-control" :selected "selected" :value val}
    #js {:className "form-control" :value val}))

(defn action-type-component [data owner]
  (reify
    om/IRender
    (render [this]
      (let [type (:type data)]
       (dom/div #js {:className "form-group action-type-component has-success"}
                (dom/label #js {:htmlFor "action-type"
                                :className "control-label col-sm-2 no-padding-right"}
                           "Type")
                (dom/div #js {:className "col-sm-10"}
                         (dom/select #js {:className "form-control"
                                          :id "action-type"
                                          :defaultValue type
                                          :onChange #(om/update! data :type (u/ev-value %))}
                                     (dom/option (option-element-map "email" type)  "Email")
                                     (dom/option (option-element-map "noop" type) "Noop")
                                     (dom/option (option-element-map "github-issue" type) "GitHub issue")
                                     (dom/option (option-element-map "github-pull-request" type)
                                                 "GitHub pull request"))))))))

