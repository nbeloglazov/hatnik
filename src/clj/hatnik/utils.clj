(ns hatnik.utils
  (:require [postal.core :as p]
            [taoensso.timbre :as timbre]))

(defn send-email [config to subject body]
  (p/send-message (select-keys config
                               [:host :user :ssl :pass])
                  {:from (:from config)
                   :to to
                   :subject subject
                   :body body}))
