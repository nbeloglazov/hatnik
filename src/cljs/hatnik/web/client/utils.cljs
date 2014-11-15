(ns hatnik.web.client.utils
  (:require [schema.core :as s]))

(defn validate
  "Validates data using given schema. Returns one of two
  css classes: has-success or has-error"
  [schema data]
  (cond
   (empty? data) "has-warning"
   (s/check schema data) "has-error"
   :else "has-success"))
