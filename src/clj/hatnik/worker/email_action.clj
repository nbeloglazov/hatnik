(ns hatnik.worker.email-action
  (:require [postal.core :as p]
            [hatnik.config :refer [config]]
            [clojure.string :as cstr]
            [taoensso.timbre :as timbre]))

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
    (try
      (send-email (:address action) subject body)
      nil
      (catch Exception e
        (timbre/error e "Couldn't sent email"
                      "Settings: "(:email config)
                      "Address:" (:address action)
                      "Subject:" subject
                      "Body:" body)
        "Couldn't send email."))))

(comment
  (perform {:template "Library {{library}} released {{version}}"
            :address "nikelandjelo@gmail.com"}
           {}
           {:library "Meee"
            :version "2.2.0"})

  )
