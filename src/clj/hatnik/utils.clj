(ns hatnik.utils
  (:require [postal.core :as p]
            [ring.util.response :as resp]
            [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders.postal :refer [make-postal-appender]]
            [tentacles
             [issues :as issues]
             [repos :as repos]
             [pulls :as pulls]]
            [clojure.string :as cstr]))

(defn send-email
  "Sends email to given address with subject and body.
    config - configs for email connection, should contain :host, :user, :pass
    to - address to whom send email
    subject - email subject
    body - email body. Can be a string or a collection of maps.
           Check postal documentation."
  [config to subject body]
  (p/send-message (select-keys config
                               [:host :user :ssl :pass])
                  {:from (:from config)
                   :to to
                   :subject subject
                   :body (if (string? body)
                           [{:type "text/plain; charset=utf-8"
                             :content body}]
                           body)}))

(defn map-value
  "Iterates through the map and applies given function to each value,
  return updated map."
  [f mp]
  (into {}
        (for [[key value] mp]
          [key (f value)])))

(defn create-github-issue
  "Creates github issue using provided token and data.
    github-token - token of Hatnik github user
    issue - data about issue. Should contain following fields:
           :user, :repo, :title, :body"
  [github-token {:keys [user repo title body] :as issue}]
  (issues/create-issue user repo title
                       {:body body
                        :oauth-token github-token}))

(defn fork-github-repo
  "Forks github repo. Returns clone url.
    github-token - token of Hathink github user.
    user - owner of the repo we want to fork.
    repo - repo we want to fork."
  [github-token user repo]
  (let [url (->> {:oauth-token github-token}
                 (repos/create-fork user repo)
                 (:clone_url))]
    (cstr/replace url #"(https?://)" (str "$1" github-token "@"))))

(defn create-github-pull-request
  "Creates github pull request, all commits must be pushed to
  server to be ready for pull-requesting.
    github-token - token of Hathink github user.
    data - map with pull request settings. Should contain
           following fields: :user, :repo, :title, :body, :branch"
  [github-token {:keys [user repo title body branch] :as data}]
  (pulls/create-pull user repo title "master" (str "hatnik:" branch)
                     {:oauth-token github-token
                      :body body}))

(defn split-repo
  "Splits github repo string returning user and repo."
  [repo]
  (cstr/split repo #"/"))

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

(defn notify-about-errors-via-email
  "Adds appender to timbre which sends emails to all addresses specified
  in :send-errors-to config parameter. Only messages with level :error and
  higher are sent."
  [config]
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

(defn wrap-exceptions
  "Wraps ring handler to catch all exceptions. Exceptions are caught and
  logged. Instead of error json response is returned."
  [handler]
  (fn [req]
    (try
      (handler req)
      (catch Exception e
        (timbre/error e)
        (resp/response {:result :error
                        :message "Something bad happened on server"})))))
