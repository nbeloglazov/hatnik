(ns hatnik.worker.github-pull-request-action
  (:require [taoensso.timbre :as timbre]
            [me.raynes.fs :as fs]
            [tentacles.repos :as repos]
            [hatnik.utils :as u]
            [clojure.java.io :as io])
  (:import [java.util.regex Pattern Matcher]
           [com.googlecode.streamflyer.regex RegexModifier]
           [com.googlecode.streamflyer.core ModifyingReader]
           [org.apache.commons.io FileUtils]))

(defn ensure-repo-is-forked
  "Checks if given repo is already forked for Hatnik user. If not - forks it"
  [repo utils]
  (timbre/info "Ensuring" repo "is forked"))

(defn clone-repo
  "Clones repo to local temp directory."
  [repo target-dir]
  (timbre/info "Cloning" repo "to" target-dir))

(defn parent?
  "Checks whether given dir is a parent of the file."
  [dir file]
  (.startsWith (.normalize (.toPath file))
               (.toPath dir)))

(defn perform-operation
  "Performs single operation on repository.
  Retturn result of operation, one of :updated, :unmodified,
  :file-not-found, :error"
  [{:keys [file regex replacement]} variables repo-dir]
  (try
    (let [file (fs/file repo-dir file)
          regex (u/fill-template regex
                                 (u/map-value #(Pattern/quote %) variables))
          replacement (u/fill-template replacement
                                       (u/map-value #(Matcher/quoteReplacement %)
                                                    variables))
          modifier (RegexModifier. regex 0 replacement)]
      (if (and (parent? repo-dir file)
               (fs/file? file)
               (fs/exists? file))
        (let [tmp-file (fs/temp-file "hatnik_tmp")]
          (try
            (with-open [reader (ModifyingReader. (io/reader file) modifier)]
              (io/copy reader tmp-file))
            (let [result (if (FileUtils/contentEquals file tmp-file)
                           :unmodified
                           :updated)]
              (io/copy tmp-file file)
              result)
            (finally
              (fs/delete tmp-file))))
        :file-not-found))
    (catch Exception e
      (timbre/error e)
      :error)))

(defn update-files
  "Updates files in cloned repo by performing operations from provided action.
  For each operation returns result, see perform-operation function for
  possible results."
  [action variables repo-dir]
  (timbre/info "Updating files in" repo-dir "using" (:operations action))
  (map #(perform-operation % variables repo-dir) (:operations action)))

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
      (commit-and-push  action repo-dir)
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
