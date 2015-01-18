(ns ^:selenium
  hatnik.selenium.build-file-projects-test
  (:require [hatnik.selenium.core :refer :all]
            [clojure.test :refer :all]
            [hatnik.test-utils :refer :all]
            [hatnik.selenium.projects-test :as p]))

; Start web server once for all tests
(use-fixtures :once system-fixture)


(deftest project-based-on-github-repo-test
  (run-with-driver
   (fn [driver]
     ; Delete default project
     (p/delete-project driver (first (find-projects-on-page driver)))

     ; Create new project that based on nbeloglazov/hatnik-test-lib repo.
     ; Use pull request action.
     (.click (find-element driver "#add-project"))
     (wait-until-dialog-visible driver :project)
     (set-input-text-from-map driver
                              {:project-name "Using repo"
                               :build-file "nbeloglazov/hatnik-test-lib"})
     (change-action-type driver "github-pull-request")
     (set-input-text-from-map driver
                              {:gh-title "Pull request title"
                               :gh-body "Pull request body"})
     (.click (find-element driver
                           "#iModalProjectMenu .modal-footer .btn-primary"))

     ; Server should parse project.clj from repo and add 2 libraries:
     ; clojure and clojurescript.
     (wait-until-projects-match driver
                                [{:name "Using repo"
                                  :build-file "nbeloglazov/hatnik-test-lib"
                                  :actions [{:library "org.clojure/clojure"
                                             :type "pull request"}
                                            {:library "org.clojure/clojurescript"
                                             :type "pull request"}]}])

     ; Open project form and verify that all fields were set correctly.
     (p/open-project-dialog driver (first (find-projects-on-page driver)))
     (assert (= (action-params driver)
                {:action-type "github-pull-request"
                 :gh-repo "nbeloglazov/hatnik-test-lib"
                 :gh-title "Pull request title"
                 :gh-body "Pull request body"
                 :file-operation-type "project.clj"
                 :build-file "nbeloglazov/hatnik-test-lib"})))))

(def build-file-url "https://raw.githubusercontent.com/nbeloglazov/hatnik-test-lib/master/project.clj")

(deftest project-based-on-build-file-url-test
  (run-with-driver
   (fn [driver]
     ; Delete default project
     (p/delete-project driver (first (find-projects-on-page driver)))

     ; Create new project that based on nbeloglazov/hatnik-test-lib repo.
     ; Use pull request action.
     (.click (find-element driver "#add-project"))
     (wait-until-dialog-visible driver :project)
     (set-input-text-from-map driver
                              {:project-name "Using url"
                               :build-file build-file-url
                               :email-subject "Email subject"
                               :email-body "Email body"})
     (.click (find-element driver
                           "#iModalProjectMenu .modal-footer .btn-primary"))

     ; Server should parse project.clj provided in url and add 2 libraries:
     ; clojure and clojurescript.
     (wait-until-projects-match driver
                                [{:name "Using url"
                                  :build-file build-file-url
                                  :actions [{:library "org.clojure/clojure"
                                             :type "email"}
                                            {:library "org.clojure/clojurescript"
                                             :type "email"}]}])

     ; Open project form and verify that all fields were set correctly.
     (p/open-project-dialog driver (first (find-projects-on-page driver)))
     (assert (= (action-params driver)
                {:action-type "email"
                 :email-subject "Email subject"
                 :email-body "Email body"
                 :build-file build-file-url})))))

(comment

  (def system (start-test-system))

  (com.stuartsierra.component/stop system)


  (def driver (create-and-login))

  )
