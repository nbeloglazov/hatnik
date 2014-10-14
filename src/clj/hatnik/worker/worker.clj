(ns hatnik.worker.worker
  (:require [hatnik.worker.email-action :as email]
            [hatnik.worker.github-issue-action :as github-issue]
            [hatnik.versions :as ver]
            [hatnik.db.storage :as stg]

            [com.stuartsierra.component :as component]
            [taoensso.timbre :as timbre]

            [clojurewerkz.quartzite.scheduler :as qs]
            [clojurewerkz.quartzite.triggers :as t]
            [clojurewerkz.quartzite.jobs :as j]
            [clojurewerkz.quartzite.conversion :as qc]
            [clojurewerkz.quartzite.schedule.simple :as ss])
  (:import [org.quartz DateBuilder DateBuilder$IntervalUnit]))

(defn perform-action-disabled
  "Version of perform-action which does nothing, only logs actions.
  Useful for development when you don't want to send email accidentally.
  Arguments: see doc for perform-action."
  [action user variables utils]
  (timbre/debug "Performing action for user" user
                " Variables: " variables)
  (timbre/debug "Action disabled. Set :enable-actions to true in config.clj. Action: " action))

(defn perform-action
  "Perform action if library was updated. Or if user requested test action.
  Arguments:
    action - map containgin action data.
    user - map containing user data.
    variables - map of variables that can be substitued in action test,
                for example library name, version, previous version.
    utils - functions that include external communications. For example
            sending emails."
  [action user variables utils]
  (timbre/debug "Performing action for user" user
                " Variables: " variables)
  (case (:type action)
    "email" (email/perform action user variables utils)
    "noop" nil ; doing nothing
    "github-issue" (github-issue/perform action user variables utils)))

(defn check-library-and-perform-actions
  "Retrieves latest version for given library and updates actions if they
  are outdated.
  Arguments:
    library - name of library to check.
    actions - collection of all actions for the library.
    db - storage.
    perform-action - function that peforms actions.
    utils - functions that include external communications. For example
            sending emails."
  [library actions db perform-action utils]
  (timbre/debug "Checking library" library)
  (when-let [ver (ver/latest-release library)]
    (timbre/debug "Latest version" library "is" ver)
    (doseq [action actions
            :when (and (= (:library action) library)
                       (ver/first-newer? ver (:last-processed-version action)))]
      (let [proj (stg/get-project db (:project-id action))
            user (stg/get-user-by-id db (:user-id proj))
            error (perform-action action user
                                  {:library library
                                   :version ver
                                   :previous-version (:last-processed-version action)
                                   :project (:name proj)}
                                  utils)]
        (if-not error
          (do (timbre/debug "Action completed sucessfully. Updating library version in action.")
              (stg/update-action! db (:id user)
                                  (:id action) (assoc action
                                                 :last-processed-version ver))
              (timbre/debug "Action updated."))
          (timbre/warn "Error while performing action: " error "Not updating the action."))))))

(defn update-all-actions
  "Runs update for all actions by checking latest versions of corresponding
  libraries..
  Arguments:
    db - storage.
    perform-action - function that peforms actions.
    utils - functions that include external communications. For example
            sending emails."
  [db perform-action utils]
  (let [actions (stg/get-actions db)
        libraries (group-by :library actions)]
    (timbre/info "Total actions:" (count actions)
                 "Libraries to update:" (keys libraries))
    (doseq [[library actions] libraries]
      (check-library-and-perform-actions library actions db
                                         perform-action utils))
    (timbre/info "Libraries updated")))

(j/defjob UpdateActionsJob [ctx]
  (try
    (timbre/info "Running UpdateActions job")
    (let [data (into {} (.getMergedJobDataMap ctx))]
      (update-all-actions (data "db") (data "perform-action") (data "utils")))
    (catch Exception e
      (timbre/error e "Error while executing UpdateActionsJob"))))


(defrecord Worker [config db perform-action utils]

  component/Lifecycle
  (start [component]
    (timbre/info "Starting Worker component.")
    (timbre/info "Initialising quartz and starting job. Quartz config:"
                 (:quartz config))
    (qs/initialize)
    (qs/start)
    (let [data (org.quartz.JobDataMap. {"db" db
                                        "perform-action" perform-action
                                        "utils" utils})
          job (j/build
               (j/of-type UpdateActionsJob)
               (j/with-identity (j/key "jobs.updateactions.1"))
               (.usingJobData data))
          start-at (DateBuilder/futureDate (-> config
                                               :quartz
                                               :initial-delay-in-seconds)
                                           DateBuilder$IntervalUnit/SECOND)
          trigger (t/build
                   (t/with-identity (t/key "triggers.1"))
                   (t/start-at start-at)
                   (t/with-schedule (ss/schedule
                                     (ss/repeat-forever)
                                     (ss/with-interval-in-seconds
                                       (-> config
                                           :quartz
                                           :interval-in-seconds)))))]
      (qs/schedule job trigger))
    component)

  (stop [component]
    (timbre/info "Stopping Worker component.")
    (qs/delete-job (j/key "jobs.updateactions.1"))
    (qs/shutdown true)
    component))

(comment

  (def worker (map->Worker {:config (hatnik.config/get-config)}))

  (component/start worker)

  (component/stop worker)

  )
