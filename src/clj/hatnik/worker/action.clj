(ns hatnik.worker.action
  (:require [hatnik.worker.email-action :as email]))

(defn perform-action [action user variables]
  (case (:type action)
    "email" (email/perform action user variables)))


