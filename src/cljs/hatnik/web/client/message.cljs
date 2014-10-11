(ns hatnik.web.client.message
  (:use [jayq.core :only [$]]))


;; type can be "null", "info", "danger", "success"
(defn show [message & {:keys [type]
                       :or {type nil}}]
  (.bootstrapGrowl js/$ message #js {"type" type}))

;; helpers
(defn success [msg] (show msg :type "success"))
(defn info [msg] (show msg :type "info"))
(defn danger [msg] (show msg :type "danger"))

