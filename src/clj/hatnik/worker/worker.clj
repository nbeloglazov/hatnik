(ns hatnik.worker.worker
  (:require [hatnik.worker.email-action :as email]
            [hatnik.worker.github-issue-action :as github-issue]
            [hatnik.worker.github-pull-request-action :as github-pull-request]
            [hatnik.versions :as ver]
            [hatnik.db.storage :as stg]
            [hatnik.worker.build-file-worker :as bf]

            [com.stuartsierra.component :as component]
            [taoensso.timbre :as timbre]
            [clojure.string :as cstr]

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
  (timbre/debug "Action disabled. Set :enable-actions to true in config.clj. Action: " action)
  {:result :ok})

(defn perform-action
  "Perform action if library was updated. Or if user requested test action.
  Arguments:
    action - map containgin action data.
    user - map containing user data.
    variables - map of variables that can be substitued in action test,
                for example library name, version, previous version.
    utils - functions that include external communications. For example
            sending emails.

  Returns a map containing :result (:ok or :error) and optional :message
  if result is :error"
  [action user variables utils]
  (timbre/debug "Performing action for user" user
                " Variables: " variables)
  (case (:type action)
    "email" (email/perform action user variables utils)
    "noop" {:result :ok} ; doing nothing
    "github-issue" (github-issue/perform action user variables utils)
    "github-pull-request" (github-pull-request/perform action user
                                                       variables utils)))

(defn build-variables-map
  "Builds map of variables that can be used in templates via {{variable}} syntax."
  [project action library-version]
  (let [library-name (-> action :library :name)
        [group-id artifact-id] (cstr/split library-name #"/")]
    {:library library-name
     :group-id group-id
     ; some libraries have both artifact and group ids same. E.g. quil
     :artifact-id (or artifact-id group-id)
     :version library-version
     :previous-version (:last-processed-version action)
     :project (:name project)}))

(defn check-library-and-perform-actions
  "Retrieves latest version for given library and updates actions if they
  are outdated.
  Arguments:
    library - library to check.
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
      (try
        (let [proj (stg/get-project db (:project-id action))
              user (stg/get-user-by-id db (:user-id proj))
              action-to-perform (if (= (:type proj) "build-file")
                                  (:action proj)
                                  action)
              result (perform-action action-to-perform user
                                     (build-variables-map proj action ver)
                                     utils)]
          (if (= (:result result) :ok)
            (do (timbre/debug "Action completed sucessfully. Updating library version in action.")
                (stg/update-action! db (:id user)
                                    (:id action) (assoc action
                                                   :last-processed-version ver))
                (timbre/debug "Action updated."))
            (timbre/warn "Error while performing action: " (:message result) "Not updating the action.")))
        (catch Exception e
          (timbre/error "Error in action" (:id action))
          (throw e))))))

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
                 ", total libraries:" (count libraries))
    (doseq [[library actions] libraries]
      (check-library-and-perform-actions library actions db
                                         perform-action utils))
    (timbre/info "Libraries updated")))

(j/defjob UpdateActionsJob [ctx]
  (try
    (timbre/info "Running UpdateActions job")
    (let [data (.. ctx getScheduler getContext)]
      (update-all-actions (get data "db")
                          (get data "perform-action")
                          (get data "utils")))
    (catch Exception e
      (timbre/error e "Error while executing UpdateActionsJob"))))

(defn schedule-job [config scheduler job name]
  (let [job (j/build
             (j/of-type job)
             (j/with-identity (j/key name)))
        start-at (DateBuilder/futureDate (-> config
                                             :quartz
                                             :initial-delay-in-seconds)
                                         DateBuilder$IntervalUnit/SECOND)
        trigger (t/build
                 (t/with-identity (t/key (str "trigger." name)))
                 (t/start-at start-at)
                 (t/with-schedule (ss/schedule
                                   (ss/repeat-forever)
                                   (ss/with-interval-in-seconds
                                     (-> config
                                         :quartz
                                         :interval-in-seconds)))))]
    (qs/schedule scheduler job trigger)))

(defrecord Worker [config db perform-action utils]

  component/Lifecycle
  (start [component]
    (timbre/info "Starting Worker component.")
    (timbre/info "Initialising quartz and starting job. Quartz config:"
                 (:quartz config))
    (let [scheduler (-> (qs/initialize) qs/start)
          jobs (-> config :quartz :jobs)]
      (.putAll (.getContext scheduler)
               {"db" db
                "perform-action" perform-action
                "utils" utils})
      (when (contains? jobs :update-actions)
        (timbre/info "Scheduling UpdateActionsJob")
        (schedule-job config
                      scheduler
                      UpdateActionsJob
                      "jobs.updateactions.1"))
      (when (contains? jobs :sync-build-file-actions)
        (timbre/info "Scheduling SyncBuildFileActionsJob")
        (schedule-job config
                      scheduler
                      hatnik.worker.build_file_worker.SyncBuildFileActionsJob
                      "jobs.syncbuildfileactions.1"))
      (assoc component :scheduler scheduler)))

  (stop [component]
    (timbre/info "Stopping Worker component.")
    (when-let [scheduler (:scheduler component)]
      (let [jobs (-> config :quartz :jobs)]
        (when (contains? jobs :update-actions)
          (qs/delete-job scheduler (j/key "jobs.updateactions.1")))
        (when (contains? jobs :sync-build-file-actions)
          (qs/delete-job scheduler (j/key "jobs.syncbuildfileactions.1"))))
      (qs/shutdown scheduler true))
    (assoc component :scheduler nil)))

(comment

  (def worker (-> (map->Worker {:config (hatnik.config/get-config)})
                  component/start))

  (component/stop worker)

  )

