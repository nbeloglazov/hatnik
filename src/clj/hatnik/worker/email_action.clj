(ns hatnik.worker.email-action
  (:require [postal.core :as p]
            [taoensso.timbre :as timbre]
            [hatnik.utils :as u]))

(defn perform
  "Sends email to provided user using template from action and
  provided variables."
  [action user variables utils]
  (let [subject (u/fill-template (:subject action) variables)
        body (u/fill-template (:body action) variables)
        send-email (:send-email utils)
        body (if (= (:type action) :html)
               [{:type "text/html; charset=utf-8"
                 :content body}]
               body)]
    (try
      (send-email (:email user) subject body)
      {:result :ok}
      (catch Exception e
        (timbre/error e "Couldn't send email"
                      "Address:" (:email user)
                      "Subject:" subject
                      "Body:" body)
        {:result :error
         :message "Couldn't send email."}))))

(comment

  (require 'hatnik.config 'hatnik.utils)
  (let [config (:email (hatnik.config/get-config))
        send-email (partial hatnik.utils/send-email config)]
    (perform {:body "Library {{library}} released {{version}} Привет"
              :subject "[Hatnik] release {{library}}"}
             {:email "me@nbeloglazov.com"}
             {:library "Meee"
              :version "2.2.0"}
             {:send-email send-email}))
  )
