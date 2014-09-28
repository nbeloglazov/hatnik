(ns hatnik.worker.worker
  (:require [hatnik.worker.email-action :as email]
            [hatnik.versions :as ver]
            [taoensso.timbre :as timbre]
            [hatnik.db.storage :as stg]))

(defn perform-action [action user variables]
  (timbre/info "Performing action for user" user
               " Variables: " variables)
  (case (:type action)
    "email" (email/perform action user variables)))

(defn check-library-and-perform-actions [library actions]
  (timbre/info "Checking library" library)
  (let [ver (ver/latest-release library)]
    (timbre/info "Latest version" library "is" ver)
    (doseq [action actions
            :when (and (= (:library action) library)
                       (ver/first-newer? ver (:last-processed-version action)))
            :let [proj (stg/get-project @stg/storage (:project-id action))
                  user (stg/get-user-by-id @stg/storage (:user-id proj))]]
      
      )))
