(ns hatnik.worker.build-file-worker
  (:require [hatnik.db.storage :as stg]
            [hatnik.web.server.build-files :as bf]
            [taoensso.timbre :as timbre]
            [clojurewerkz.quartzite.jobs :as j]
            [hatnik.versions :as ver]))

(defn sync-build-file-project [db id old-actions]
  (let [{:keys [user-id build-file]} (stg/get-project db id)
        cur-actions (bf/actions-from-build-file build-file)
        cur-libraries (set (map :library cur-actions))
        old-libraries (set (map :library old-actions))]
    (timbre/debug "Syncing project" id "build-file" build-file
                  "old-actions" old-actions
                  "cur-actions" cur-actions)

    ; Delete actions that no longer present in build file.
    (doseq [action old-actions
            :when (not (contains? cur-libraries (:library action)))]
      (timbre/debug "Deleting action" (:library action))
      (stg/delete-action! db user-id (:id action)))

    ; Create new actions that were added to build-file since last sync.
    (doseq [action cur-actions
            :when (not (contains? old-libraries (:library action)))]
      (timbre/debug "Creating action" (:library action))
      (stg/create-action! db user-id (assoc action :project-id id)))))

(defn sync-build-file-actions [db]
  (let [actions-by-proj-id (->> (stg/get-actions db)
                                (filter #(= (:type %) "build-file"))
                                (group-by :project-id))]
    (timbre/debug "Total build-file projects" (count actions-by-proj-id))
    (doseq [[id actions] actions-by-proj-id]
      (sync-build-file-project db id actions))))

(j/defjob SyncBuildFileActionsJob [ctx]
  (try
    (timbre/info "Running SyncBuildFileActions job")
    (let [data (into {} (.getMergedJobDataMap ctx))]
      (sync-build-file-actions (data "db")))
    (catch Exception e
      (timbre/error e "Error while executing SyncBuildFileActions job"))))
