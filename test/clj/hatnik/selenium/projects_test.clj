(ns ^:selenium
  hatnik.selenium.projects-test
  (:require [hatnik.selenium.core :refer :all]
            [clojure.test :refer :all]
            [hatnik.test-utils :refer :all]
            [taoensso.timbre :as timbre]))

; Start web server once for all tests
(use-fixtures :once system-fixture)

(defn set-project-name-field [driver name]
  (set-input-text driver "#iModalProjectMenu input" name))

(defn open-project-dialog [driver project]
  (.click (find-element (:element project) ".glyphicon-pencil"))
  (wait-until-dialog-visible driver :project))

(defn change-project-name [driver project name]
  (open-project-dialog driver project)

  (set-project-name-field driver name)
  (.click (find-element driver
                        "#iModalProjectMenu .modal-footer .btn-primary"))
  (wait-until-dialog-invisible driver :project))

(defn delete-project [driver project]
  (open-project-dialog driver project)
  (.click (find-element driver
                        "#iModalProjectMenu .modal-footer .btn-danger"))
  (wait-until-dialog-invisible driver :project))

(defn create-project [driver name]
  (.click (find-element driver "#add-project"))
  (wait-until-dialog-visible driver :project)
  (set-project-name-field driver name)
  (.click (find-element driver
                        "#iModalProjectMenu .modal-footer .btn-primary"))
  (wait-until-dialog-invisible driver :project))

(deftest create-change-delete-test
  (let [driver (create-and-login)]
    (try
      ; Change project name to "New name"
      (change-project-name driver
                           (first (find-projects-on-page driver))
                           "New name")
      (wait-until-projects-match driver
                                 [{:name "New name"
                                   :actions []}])

      ; Add new project "Project Foo"
      (create-project driver "Project Foo")
      (wait-until-projects-match driver
                                 [{:name "New name"
                                   :actions []}
                                  {:name "Project Foo"
                                   :actions []}])

      ; Rename "Project Foo" to "Project Bar"
      (change-project-name driver
                           (second (find-projects-on-page driver))
                           "Project Bar")
      (wait-until-projects-match driver
                                 [{:name "New name"
                                   :actions []}
                                  {:name "Project Bar"
                                   :actions []}])

      ; Delete first project.
      (delete-project driver
                      (first (find-projects-on-page driver)))
      (wait-until-projects-match driver
                                 [{:name "Project Bar"
                                   :actions []}])

      ; Delete remaining project.
      (delete-project driver
                      (first (find-projects-on-page driver)))
      (wait-until-projects-match driver
                                 [])
      (catch Exception e
        (timbre/error "Screenshot:" (take-screenshot driver))
        (throw e))
      (finally
        (.quit driver)))))


(comment

  (start-test-system)

  (create-change-delete-test)

  (def driver (create-and-login))

  (create-project driver "Hfff")

  )

