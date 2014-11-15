(ns hatnik.web.client.utils
  (:require [schema.core :as s]))

(defn validate
  "Validates data using given schema. Returns one of two
  css classes: has-success or has-error"
  [schema data]
  (if (s/check schema data) "has-error" "has-success"))
