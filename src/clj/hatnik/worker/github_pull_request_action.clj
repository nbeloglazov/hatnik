(ns hatnik.worker.github-pull-request-action
  (:require [taoensso.timbre :as timbre]
            [me.raynes.fs :as fs]
            [tentacles.repos :as repos]))

(defn ensure-repo-is-forked
  "Checks if given repo is already forked for Hatnik user. If not - forks it"
  [repo utils]
  (timbre/info "Ensuring" repo "is forked"))

(defn clone-repo
  "Clones repo to local temp directory."
  [repo target-dir]
  (timbre/info "Cloning" repo "to" target-dir))

(defn update-files
  "Updates files in cloned repo by performing operations from provided action."
  [action variables repo-dir]
  (timbre/info "Updating files in" repo-dir "using" (:operations action)))

(defn commit-and-push
  "Commits all changes and pushes to github."
  [action repo-dir]
  (timbre/info "Commiting, pushing" repo-dir))

(defn open-pull-request
  "Opens pull request based on changed files."
  [action repo]
  (timbre/info "Opening pull request to" repo))

(defn perform
  "Modifies github repo and opens pull request."
  [action user variables utils]
  (let [repo-dir (fs/temp-dir "hatnik-pull-request-")
        repo (:repo action)]
    (try
      (ensure-repo-is-forked repo utils)
      (clone-repo repo repo-dir)
      (update-files action variables repo-dir)
      (commit-push-cleanup  action repo-dir)
      (open-pull-request action repo)
      nil
      (catch Exception e
        (timbre/error e "Error in pull-request action. Action: " action "Variables: " variables)
        :error)
      (finally
        (fs/delete-dir repo-dir)))))


(comment

  (perform {:project-id "sdf"
            :library "quil"
            :type "github-pull-request"
            :repo "nbeloglazov/hatnik-test-lib"
            :title "Hello {{library}}"
            :body "Hello"
            :commit-message "Commit"
            :operations [{:file "some/file"
                          :regex "regex"
                          :replacement "replacement"}]}
           {:github-login "Me"}
           {:library "quil"
            :version "2.3.4"
            :previous-version "1.2.3"}
           {})

  (def opts {:oauth-token (:hatnik-github-token (read-string (slurp "config.clj")))})

  (repos/repos opts)

  (repos/create-fork "nbeloglazov" "hatnik-test-lib" opts)

  )
