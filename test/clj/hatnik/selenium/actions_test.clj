(ns ^:selenium
  hatnik.selenium.actions-test
  (:require [hatnik.selenium.core :refer :all]
            [clojure.test :refer :all]
            [hatnik.test-utils :refer :all])
  (:import org.openqa.selenium.support.ui.Select))

; Start web server once for all tests
(use-fixtures :once system-fixture)

(defn open-add-action-dialog [driver project]
  (.click (find-element (:element project) ".add-action"))
  (wait-until-dialog-visible driver :action))

(defn open-edit-action-dialog [driver action]
  (.click (:element action))
  (wait-until-dialog-visible driver :action))

(defn apply-changes
  "Clicks Create or Update button on action dialog depending on context."
  [driver]
  (.click (find-element driver "#iModalAddAction .modal-footer .btn-primary"))
  (wait-until-dialog-invisible driver :action))

(defn create-action-simple [driver project library]
  (open-add-action-dialog driver project)
  (set-input-text driver "#library-input" library)
  (apply-changes driver))

(defn delete-action [driver action]
  (open-edit-action-dialog driver action)
  (.click (find-element driver "#iModalAddAction .modal-header .btn-danger"))
  (wait-until-dialog-invisible driver :action))

(defn action-params [driver]
  (let [element-value #(if-let [el (->> %
                                        name
                                        (str "#")
                                        (find-elements driver)
                                        first)]
                         (.getAttribute el "value")
                         nil)
        params (into {}
                     (for [id [:library-input :action-type :email-subject :email-body
                               :gh-repo :gh-issue-title :gh-issue-body
                               :gh-title :gh-body]
                           :let [value (element-value id)]
                           :when value]
                       [id value]))
        gh-pr-operations (map-indexed (fn [ind operation]
                                        {:file (element-value (str "gh-pull-req-op-file-" ind))
                                         :regex (element-value (str "gh-pull-req-op-regex-" ind))
                                         :replacement (element-value (str "gh-pull-req-op-repl-" ind))})
                                      (find-elements driver ".operations .operation"))]
    (if (empty? gh-pr-operations)
      params
      (assoc params
        :gh-pr-operations gh-pr-operations))))

(deftest create-delete-test []
  (run-with-driver
   (fn [driver]
     (let [[project] (find-projects-on-page driver)]

       ; Create action for quil
       (create-action-simple driver project "quil")
       (wait-until-projects-match driver
                                  [{:name "Default"
                                    :actions [{:library "quil"
                                               :type "email"}]}])

       ; Create action for org.clojure/clojure
       (create-action-simple driver project "org.clojure/clojure")
       (wait-until-projects-match driver
                                  [{:name "Default"
                                    :actions [{:library "quil"
                                               :type "email"}
                                              {:library "org.clojure/clojure"
                                               :type "email"}]}])

       ; Delete quil action.
       (delete-action driver
                      (-> driver find-projects-on-page first :actions first))
       (wait-until-projects-match driver
                                  [{:name "Default"
                                    :actions [{:library "org.clojure/clojure"
                                               :type "email"}]}])

                                        ; Delete remaining action.
       (delete-action driver
                      (-> driver find-projects-on-page first :actions first))
       (wait-until-projects-match driver
                                  [{:name "Default"
                                    :actions []}])))))

(defn change-action-type [driver type]
  ; Hacky way to change action type.
  ; For some reason proper way by using selenium Select object
  ; and calling .selectByValue() on it doesn't work on travis.
  (.executeScript driver
                  (str "$('#action-type').val('" type "');"
                       "(function() { "
                       "  var evt = document.createEvent('HTMLEvents');"
                       "  evt.initEvent('change', true, true);"
                       "  $('#action-type')[0].dispatchEvent(evt);"
                       "})();")
                  (into-array []))
  (Thread/sleep 500))

(defn set-input-text-from-map [driver map]
  (doseq [[id text] map]
    (set-input-text driver (str "#" (name id)) text)))

(deftest email-action-test
  (run-with-driver
   (fn [driver]

     ; Create action
     (let [[project] (find-projects-on-page driver)]
       (open-add-action-dialog driver project)
       (change-action-type driver "email")
       (set-input-text-from-map driver
                                {:library-input "quil"
                                 :email-subject "Email subject"
                                 :email-body "Email body"})
       (apply-changes driver))

     ; Check that action has expected fields
     (let [[project] (find-projects-on-page driver)
           action (first (:actions project))]
       (open-edit-action-dialog driver action)
       (is (= (action-params driver)
              {:library-input "quil"
               :action-type "email"
               :email-subject "Email subject"
               :email-body "Email body"})))

     ; Update action
     (set-input-text-from-map driver
                              {:library-input "ring"
                               :email-subject "New subject"
                               :email-body "New body"})
     (apply-changes driver)

     ; Check updated action
     (let [[project] (find-projects-on-page driver)
           action (first (:actions project))]
       (open-edit-action-dialog driver action)
       (is (= (action-params driver)
              {:library-input "ring"
               :action-type "email"
               :email-subject "New subject"
               :email-body "New body"}))))))

(deftest github-issue-action-test
  (run-with-driver
   (fn [driver]

     ; Create action
     (let [[project] (find-projects-on-page driver)]
       (open-add-action-dialog driver project)
       (change-action-type driver "github-issue")
       (set-input-text-from-map driver
                                {:library-input "quil"
                                 :gh-repo "nbeloglazov/hatnik"
                                 :gh-issue-title "Issue title"
                                 :gh-issue-body "Issue body"})
       (apply-changes driver))

     ; Check that action has expected fields
     (let [[project] (find-projects-on-page driver)
           action (first (:actions project))]
       (open-edit-action-dialog driver action)
       (is (= (action-params driver)
              {:library-input "quil"
               :action-type "github-issue"
               :gh-repo "nbeloglazov/hatnik"
               :gh-issue-title "Issue title"
               :gh-issue-body "Issue body"})))

     ; Update action
     (set-input-text-from-map driver
                              {:library-input "ring"
                               :gh-repo "quil/quil"
                               :gh-issue-title "New title"
                               :gh-issue-body "New body"})
     (apply-changes driver)

     ; Check updated action
     (let [[project] (find-projects-on-page driver)
           action (first (:actions project))]
       (open-edit-action-dialog driver action)
       (is (= (action-params driver)
              {:library-input "ring"
               :action-type "github-issue"
               :gh-repo "quil/quil"
               :gh-issue-title "New title"
               :gh-issue-body "New body"}))))))

(deftest github-pull-request-action-test
  (run-with-driver
   (fn [driver]

     ; Create action
     (let [[project] (find-projects-on-page driver)]
       (open-add-action-dialog driver project)
       (change-action-type driver "github-pull-request")
       (.click (find-element driver ".add-operation"))
       (set-input-text-from-map driver
                                {:library-input "quil"
                                 :gh-repo "nbeloglazov/hatnik"
                                 :gh-title "Issue title"
                                 :gh-body "Issue body"
                                 :gh-pull-req-op-file-0 "file1"
                                 :gh-pull-req-op-regex-0 "regex1"
                                 :gh-pull-req-op-repl-0 "replacement1"
                                 :gh-pull-req-op-file-1 "file2"
                                 :gh-pull-req-op-regex-1 "regex2"
                                 :gh-pull-req-op-repl-1 "replacement2"})
       (apply-changes driver))

     ; Check that action has expected fields
     (let [[project] (find-projects-on-page driver)
           action (first (:actions project))]
       (open-edit-action-dialog driver action)
       (is (= (action-params driver)
              {:library-input "quil"
               :action-type "github-pull-request"
               :gh-repo "nbeloglazov/hatnik"
               :gh-title "Issue title"
               :gh-body "Issue body"
               :gh-pr-operations
               [{:file "file1" :regex "regex1" :replacement "replacement1"}
                {:file "file2" :regex "regex2" :replacement "replacement2"}]})))

     ; Update action: change most fields and remove one file operation
     (set-input-text-from-map driver
                              {:library-input "ring"
                               :gh-repo "quil/quil"
                               :gh-title "New title"
                               :gh-body "New body"
                               :gh-pull-req-op-file-1 "new file2"
                               :gh-pull-req-op-regex-1 "new regex2"
                               :gh-pull-req-op-repl-1 "new replacement2"})
     (-> (find-elements driver ".operations .operation .btn")
         first
         (.click))
     (apply-changes driver)

     ; Check updated action
     (let [[project] (find-projects-on-page driver)
           action (first (:actions project))]
       (open-edit-action-dialog driver action)
       (is (= (action-params driver)
              {:library-input "ring"
               :action-type "github-pull-request"
               :gh-repo "quil/quil"
               :gh-title "New title"
               :gh-body "New body"
               :gh-pr-operations
               [{:file "new file2" :regex "new regex2"
                 :replacement "new replacement2"}]}))))))

(deftest change-action-type-test
  (run-with-driver
   (fn [driver]

     ; Create email action
     (let [[project] (find-projects-on-page driver)]
       (open-add-action-dialog driver project)
       (change-action-type driver "email")
       (set-input-text-from-map driver
                                {:library-input "quil"
                                 :email-subject "Email subject"
                                 :email-body "Email body"})
       (apply-changes driver))

     ; Check that action has expected fields
     (let [[project] (find-projects-on-page driver)
           action (first (:actions project))]
       (open-edit-action-dialog driver action)
       (is (= (action-params driver)
              {:library-input "quil"
               :action-type "email"
               :email-subject "Email subject"
               :email-body "Email body"})))

     ; Change action type to github-issue and set only repository
     (change-action-type driver "github-issue")
     (set-input-text-from-map driver
                              {:gh-repo "nbeloglazov/hatnik"})
     (apply-changes driver)

     ; Check that action now is github-issue. Fields values should
     ; stay the same, only names changed (email subject -> issue title,
     ; email body -> issue body).
     (let [[project] (find-projects-on-page driver)
           action (first (:actions project))]
       (open-edit-action-dialog driver action)
       (is (= (action-params driver)
              {:library-input "quil"
               :action-type "github-issue"
               :gh-repo "nbeloglazov/hatnik"
               :gh-issue-title "Email subject"
               :gh-issue-body "Email body"}))))))

(comment

  (def system (start-test-system))

  (com.stuartsierra.component/stop system)

  (create-delete-test)

  (def driver (create-and-login))

  (open-add-action-dialog driver (first (find-projects-on-page driver)))

  (change-action-type driver "github-pull-request")



  (add-action-simple driver (first (find-projects-on-page driver))
                     "ring")

  )
