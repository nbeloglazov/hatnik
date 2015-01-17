(ns hatnik.worker.github-pull-request-action
  (:require [taoensso.timbre :as timbre]
            [me.raynes.fs :as fs]
            [tentacles.repos :as repos]
            [hatnik.utils :as u]
            [hatnik.worker.email-action :as email-action]
            [clojure.java.io :as io]
            [clojure.java.shell :refer [sh with-sh-dir]]
            [clojure.string :refer [join]]
            [hiccup.core :refer [html]]
            [hiccup.util :refer [escape-html]])
  (:import [java.util.regex Pattern Matcher]
           [com.googlecode.streamflyer.regex RegexModifier]
           [com.googlecode.streamflyer.core ModifyingReader]
           [org.apache.commons.io FileUtils]))

(defn ensure-repo-is-forked
  "Checks if given repo is already forked for Hatnik user. If not - forks it.
  Returns git url for cloning the fork."
  [repo utils]
  (timbre/info "Ensuring" repo "is forked")
  (let [[user repo] (u/split-repo repo)]
    ((:fork-github-repo utils) user repo)))

(defn clone-repo
  "Creates git repo in given directory and Clones master branch of
  provided repo to master branch of the directory. Clones using depth=1."
  [repo target-dir]
  (timbre/info "Cloning" repo "to" target-dir)
  (with-sh-dir target-dir
    (sh "git" "init")
    (sh "git" "remote" "add" "upstream" (str "https://github.com/" repo ".git"))
    (sh "git" "pull" "upstream" "master:master" "--depth=20")))

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

(def predefined-operations-mapping
  {"project.clj" [{:file "project.clj"
                   :regex "{{library}} \"[^\"]+\""
                   :replacement "{{library}} \"{{version}}\""}]})

(defn expand-predefined-operations
  "If provided operation is on of predefined - return a map corresponding
  to that operation."
  [operations]
  (if (string? operations)
    (predefined-operations-mapping operations)
    operations))

(defn update-files
  "Updates files in cloned repo by performing operations from provided action.
  For each operation returns result, see perform-operation function for
  possible results."
  [action variables repo-dir]
  (timbre/info "Updating files in" repo-dir "using" (:operations action))
  (->> (:operations action)
       (map #(perform-operation % variables repo-dir))
       (doall)))

(defn build-results-table
  "Creates html table that shows results of each operation.
  It has 4 columns: file, regex, replacemenet, result."
  [operations results]
  (let [cell-style "border: 1px solid black; padding: 5px;"
        to-row (fn [tag values]
                 [:tr
                  (map #(vector tag {:style cell-style} (escape-html %))
                       values)])
        result-to-text {:updated "Updated"
                        :unmodified "Not changed"
                        :error "Error"
                        :file-not-found "File not found"}]
   (html [:table {:style "border: 1px solid black; border-collapse: collapse;"}
          (to-row :th ["File" "Regex" "Replacement" "Result"])
          (map (fn [{:keys [file regex replacement]} result]
                 (to-row :td
                         [file regex replacement (result-to-text result)]))
               operations results)])))

(defn commit-and-push
  "Commits all changes and pushes to fork repo to under the given branch."
  [action variables repo-dir branch fork-url]
  (timbre/info "Commiting, pushing" repo-dir "to remote branch" branch
               "fork" fork-url)
  (let [message (-> (:title action)
                    (u/fill-template variables))]
    (with-sh-dir repo-dir
      (sh "git" "commit" "-am" message)
      (sh "git" "remote" "add" "origin" fork-url)
      (sh "git" "push" "origin" (str "master:" branch)))))

(defn open-pull-request
  "Opens pull request based on changed files."
  [action orig-user variables branch utils]
  (timbre/info "Opening pull request to" (:repo action) "from branch" branch)
  (let [[user repo] (u/split-repo (:repo action))]
    ((:create-github-pull-request utils)
     {:user user
      :repo repo
      :title (u/fill-template (:title action) variables)
      :body (str (u/fill-template (:body action) variables)
                 "\n\nThis pull request is created on behalf of @"
                 (:github-login orig-user))
      :branch branch})))

(def email-body
  (->> [(str "No files were changed. You can find results of executing "
             "file-change operations below.")
        "Library: {{library}}, version {{version}}, previous version {{previous-version}}"
        "Operations results:"
        "{{results-table}}"
        ""
        "Hatnik Team"]
       (map #(str "<p>" % "</p>"))
       (join \newline)))

(defn send-email
  "Sends an email instead of pull request because no files
  were modified. Probably operations are not configures correctly.
  Notify the user so she is aware.."
  [action user variables utils]
  (let [subject "[Hatnik] Pull request to {{repo}} failed"
        variables (assoc variables
                    :repo (:repo action))
        action {:subject subject
                :body email-body
                :type :html}]
    (email-action/perform action user variables utils)))

(defn add-results-table
  "Adds results table to the variables map."
  [variables action results]
  (let [results-table (build-results-table (:operations action)
                                           results)]
    (assoc variables :results-table results-table)))

(defn perform
  "Modifies github repo and opens pull request."
  [action user variables utils]
  (let [repo-dir (fs/temp-dir "hatnik-pull-request-")
        action (update action :operations expand-predefined-operations)
        repo (:repo action)
        branch (str "branch-" (.getTime (java.util.Date.)))]
    (try
      (let [fork-url (ensure-repo-is-forked repo utils)
            _ (clone-repo repo repo-dir)
            results (update-files action variables repo-dir)
            variables (add-results-table variables action results)]
        (if (some #(= :updated %) results)
          (do (commit-and-push action variables repo-dir branch fork-url)
              (open-pull-request action user variables branch utils)
              {:result :ok})
          (let [result (send-email action user variables utils)]
            (if (= (:result result) :ok)
              {:result :ok
               :message (str "Couldn't create pull request. Email with "
                             "details has been sent. Check your inbox.")
               ; We want to show this as error for user when she tests pull request
               ; action. We can't mark the whole response with :result :error as
               ; it is normal workflow and not exceptional situation.
               :result-for-user :error}
              {:result :error
               :message "Server error. Couldn't create pull request."}))))
      (catch Exception e
        (timbre/error e "Error in pull-request action. Action: " action "Variables: " variables)
        {:result :error
         :message (.getMessage e)})
      (finally
        (fs/delete-dir repo-dir)))))


(comment

  (def token (:hatnik-github-token (read-string (slurp "config.clj"))))

  (perform {:project-id "sdf"
            :library "quil"
            :type "github-pull-request"
            :repo "nbeloglazov/hatnik-test-lib"
            :title "Update {{library}} to {{version}}"
            :body "Results \n {{results-table}}"
            :operations [{:file "some/file"
                          :regex "regex"
                          :replacement "replacement"}
                         {:file "project.clj"
                          :regex "{{library}} \"[^\"]+\""
                          :replacement "{{library}} \"{{version}}\""}
                         {:file "README.md"
                          :regex "hello"
                          :replacement "world"}]}
           {:github-login "nbeloglazov"
            :email "me@nbeloglazov.com"}
           {:library "org.clojure/clojure"
            :version "1.8.0"
            :previous-version "1.6.0"}
           {:fork-github-repo (partial u/fork-github-repo token)
            :create-github-pull-request (partial u/create-github-pull-request token)
            :send-email (partial u/send-email (-> "config.clj" slurp read-string :email))})

  (def opts {:oauth-token token})

  (let [url (u/fork-github-repo (:oauth-token opts) "nbeloglazov" "hatnik-test-lib")
        tmp (fs/temp-dir "hello")]
    (clone-repo url tmp))

  (repos/repos opts)

  (repos/create-fork "nbeloglazov" "hatnik-test-lib" opts)

  (u/fork-github-repo token "nbeloglazov" "hatnik-test-lib")

  )
