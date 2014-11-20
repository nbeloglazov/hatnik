(ns hatnik.web.client.utils
  (:require [schema.core :as s]
            [goog.dom :as dom]))

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
  (-> (dom/getViewportSize)
      (.-width)
      (< 768)))
