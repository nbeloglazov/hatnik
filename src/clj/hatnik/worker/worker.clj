(ns hatnik.worker.worker
  (:require [hatnik.worker.email-action :as email]
            [hatnik.versions :as ver]
            [hatnik.config :refer [config]]
            [taoensso.timbre :as timbre]
            [hatnik.db.storage :as stg]))

(defn perform-action [action user variables]
  (timbre/info "Performing action for user" user
               " Variables: " variables)
  (if (:enable-actions config)
    (case (:type action)
      "email" (email/perform action user variables))
    (timbre/info "Action disabled. Set :enable-actions to true in config.clj. Action: " action)))

(defn check-library-and-perform-actions [library actions]
  (timbre/info "Checking library" library)
  (let [ver (ver/latest-release library)]
    (timbre/info "Latest version" library "is" ver)
    (doseq [action actions
            :when (and (= (:library action) library)
                       (not= ver (:last-processed-version action)))]
      (let [proj (stg/get-project @stg/storage (:project-id action))
            user (stg/get-user-by-id @stg/storage (:user-id proj))
            error (perform-action action user
                                  {:library library
                                   :version ver
                                   :previous-version (:last-processed-version action)
                                   :project (:name proj)})]
        (if-not error
          (do (timbre/info "Action completed sucessfully. Updating library version in action.")
              (stg/update-action! @stg/storage (:id user)
                                  (:id action) (assoc action
                                                 :last-processed-version ver))
              (timbre/info "Action updated."))
          (timbre/warn "Error while performing action: " error "Not updating the action."))))))

(defn update-all-actions []
  (timbre/info "Running checking for all actions.")
  (let [actions (stg/get-actions @stg/storage)
        libraries (group-by :library actions)]
    (timbre/info "Total actions:" (count actions)
                 "Libraries to update:" (keys libraries))
    (doseq [[library actions] libraries]
      (check-library-and-perform-actions library actions))))
