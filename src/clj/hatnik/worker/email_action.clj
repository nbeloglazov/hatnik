(ns hatnik.worker.email-action
  (:require [postal.core :as p]
            [clojure.string :as cstr]
            [taoensso.timbre :as timbre]))

(defn fill-template
  "Substitutes variables to all {{aba}} placeholders if variable exists.
  If there is no such variable - leaves placeholder unchanged."
  [template variables]
  (clojure.string/replace template #"\{\{([a-zA-Z-]+)\}\}"
                          (fn [[whole variable]]
                            (if-let [value (-> variable
                                               cstr/lower-case
                                               keyword
                                               variables)]
                              (str value)
                              whole))))

(defn perform
  "Sends email to provided user using template from action and
  provided variables."
  [action user variables utils]
  (let [subject (format "[Hatnik] %s %s released"
                        (:library variables)
                        (:version variables))
        body (fill-template (:template action) variables)
        send-email (:send-email utils)]
    (try
      (send-email (:address action) subject body)
      nil
      (catch Exception e
        (timbre/error e "Couldn't send email"
                      "Address:" (:address action)
                      "Subject:" subject
                      "Body:" body)
        "Couldn't send email."))))

(comment

  (require 'hatnik.config 'hatnik.utils)
  (let [config (:email (hatnik.config/get-config))
        send-email (partial hatnik.utils/send-email config)]
    (perform {:template "Library {{library}} released {{version}}"
              :address "nikelandjelo@gmail.com"}
             {}
             {:library "Meee"
              :version "2.2.0"}
             {:send-email send-email}))

  )
