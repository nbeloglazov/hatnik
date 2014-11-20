(ns hatnik.web.client.utils
  (:require [schema.core :as s]
            [goog.dom :as gdom]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(enable-console-print!)

(defn validate
  "Validates data using given schema. Returns one of two
  css classes: has-success or has-error"
  [schema data]
  (cond
   (empty? data) "has-warning"
   (s/check schema data) "has-error"
   :else "has-success"))

(defn mobile?
  "Identifies whether current viewport is mobile viewport."
  []
  (-> (gdom/getViewportSize)
      (.-width)
      (< 768)))

(defn form-field
  "Creates standard form field"
  [{:keys [data field id title
           type placeholder validator
           feedback on-change]}]
  (dom/div #js {:className (str "form-group "
                                (if feedback "has-feedback " "")
                                (if (fn? validator)
                                  (validator)
                                  (validate validator
                                            (field data))))}
           (dom/label #js {:htmlFor id
                           :className "control-label"}
                      title)
           (case type
             :text
             (dom/input #js {:type "text"
                             :className "form-control"
                             :id id
                             :value (field data)
                             :placeholder placeholder
                             :onChange (or on-change
                                           #(om/update! data field (.. % -target -value)))})

             :textarea
             (dom/textarea #js {:cols "40"
                                :className "form-control"
                                :id "gh-issue-body"
                                :value (:body data)
                                :onChange #(om/update! data :body (.. % -target -value))}))
           feedback))
