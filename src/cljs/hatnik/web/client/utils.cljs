(ns hatnik.web.client.utils
  (:require [schema.core :as s]
            [hatnik.web.client.message :as msg]
            [jayq.core :as jq]
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
           feedback on-change popover]}]
  (dom/div #js {:className (str "form-group "
                                (if feedback "has-feedback " "")
                                (if (fn? validator)
                                  (validator)
                                  (validate validator
                                            (field data))))}
           (dom/label #js {:htmlFor id
                           :className "control-label col-sm-2 no-padding-right"}
                      title)
           (dom/div #js {:className "col-sm-10"}
            (let [attrs #js {:type "text"
                             :className "form-control"
                             :id id
                             :value (field data)
                             :placeholder placeholder
                             :data-content popover
                             :data-toggle "popover"
                             :data-placement "auto"
                             :data-trigger "focus"
                             :onChange (or on-change
                                           #(om/update! data field (.. % -target -value)))}]
              (case type
                :text (dom/input attrs)
                :textarea (dom/textarea attrs)))
            feedback)))

(defn ajax [url type data success & [error]]
  (jq/ajax url
           {:type type
            :data (if data
                    (.stringify js/JSON
                                (clj->js data))
                    nil)
            :contentType "application/json"
            :dataType "json"
            :async true
            :error (or error
                       #(msg/danger "Invalid request. Please, check request data."))
            :success #(success (js->clj % :keywordize-keys true))}))
