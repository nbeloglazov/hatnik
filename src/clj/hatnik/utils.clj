(ns hatnik.utils
  (:require [postal.core :as p]
            [ring.util.response :as resp]
            [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders.postal :refer [make-postal-appender]]))

(defn send-email [config to subject body]
  (p/send-message (select-keys config
                               [:host :user :ssl :pass])
                  {:from (:from config)
                   :to to
                   :subject subject
                   :body body}))

(defn notify-about-errors-via-email [config]
  (let [email-config (select-keys (:email config)
                                  [:host :user :ssl :pass])]
    (timbre/info "Errors will be send to" (:send-errors-to config))
    (timbre/set-config!
     [:appenders :postal]
     (make-postal-appender
      {:enabled? true
       :min-level :error
       :rate-limit [1 (* 60 1000 2)]
       :async? true}
      {:postal-config
       (with-meta {:from (-> config :email :from)
                   :to (:send-errors-to config)}
         email-config)}))))

(defn wrap-exceptions [handler]
  (fn [req]
    (try
      (handler req)
      (catch Exception e
        (timbre/error e)
        (resp/response {:result :error
                        :message "Something bad happened on server"})))))
