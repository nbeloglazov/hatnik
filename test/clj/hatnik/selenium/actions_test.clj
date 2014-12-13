(ns ^:selenium
  hatnik.selenium.actions-test
  (:require [hatnik.selenium.core :refer :all]
            [clojure.test :refer :all]
            [hatnik.test-utils :refer :all]))

; Start web server once for all tests
(use-fixtures :once system-fixture)

(defn open-add-action-dialog [driver project]
  (.click (find-element (:element project) ".add-action"))
  (wait-until-dialog-visible driver :action))

(defn open-edit-action-dialog [driver action]
  (.click (:element action))
  (wait-until-dialog-visible driver :action))

(defn create-action-simple [driver project library]
  (open-add-action-dialog driver project)
  (set-input-text driver "#library-input" library)
  (.click (find-element driver "#iModalAddAction .modal-footer .btn-primary"))
  (wait-until-dialog-invisible driver :action))

(defn delete-action [driver action]
  (open-edit-action-dialog driver action)
  (.click (find-element driver "#iModalAddAction .modal-header .btn-danger"))
  (wait-until-dialog-invisible driver :action))

(deftest create-delete-test
  (let [driver (create-and-login)]
    (try
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
                                     :actions []}])
        )
      (finally
        (.quit driver)))))


(comment

  (start-test-system)

  (create-delete-test)

  (def driver (create-and-login))

  (add-action-simple driver (first (find-projects-on-page driver))
                     "ring")

  )
