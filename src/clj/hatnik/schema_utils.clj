(ns hatnik.schema-utils
  (:require [schema.core :as s]
            [cheshire.generate :as gen]
            [schema.utils :as u]))

(defmacro ensure-valid
  "Validates object using given schema and executes body if valid.
  If not valid - returns 400 response."
  [schema obj & body]
  `(if-let [error# (s/check ~schema ~obj)]
     {:body {:result :error
             :message "Invalid request format"
             :validation-error error#}
      :status 400}
     (do ~@body)))

; Add encoders to cheshire so validation errors can be
; encoded to string and sent in response.
(gen/add-encoder schema.utils.ValidationError
                 (fn [obj gen]
                   (gen/encode-seq (u/validation-error-explain obj) gen)))

(gen/add-encoder Class
                 (fn [obj gen]
                   (gen/encode-str (.getName obj) gen)))
