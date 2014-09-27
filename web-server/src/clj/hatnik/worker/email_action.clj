(ns hatnik.worker.email-action
  (:require [postal.core :as p]
            [hatnik.config :refer [config]]
            [clojure.string :as cstr]))

(defn fill-template [template variables]
  (clojure.string/replace template #"\{\{([a-zA-Z-]+)\}\}"
                          (fn [[whole variable]]
                            (if-let [value (-> variable
                                               cstr/lower-case
                                               keyword
                                               variables)]
                              (str value)
                              whole))))

(defn send-email [to subject body]
  (p/send-message (select-keys (:email config)
                               [:host :user :ssl :pass])
                  {:from (-> config :email :from)
                   :to to
                   :subject subject
                   :body body}))

(defn perform [action user variables]
  (let [subject (format "[Hatnik] %s %s released"
                        (:library variables)
                        (:version variables))
        body (fill-template (:template action) variables)]
    (send-email (:address action) subject body)))

(comment
  (perform {:template "Library {{library}} released {{version}}"
            :address "me@nbeloglazov.com"}
           {}
           {:library "quil"
            :version "2.2.0"}))
