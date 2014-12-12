(ns ^:selenium
  hatnik.selenium.projects-test
  (:require [hatnik.selenium.core :refer :all]
            [clojure.test :refer :all]))

; Start web server once for all tests
(use-fixtures :once system-fixture)

(defn change-project-name [driver project name]
  (.click (find-element (:element project) ".glyphicon-pencil"))
  (let [input (find-element driver "#iModalProjectMenu input")]
    (wait-until-visible driver input)
    (.clear input)
    (.sendKeys input (into-array [name]))
    (.click (find-element driver
                          "#iModalProjectMenu .modal-footer .btn-primary"))))

(deftest change-name-test
  (let [driver (create-and-login)]
    (change-project-name driver
                         (first (find-projects-on-page driver))
                         "New name")
    (wait-until-projects-match driver
                               [{:name "New name"
                                 :actions []}])
    (.quit driver)))


(comment
  (def driver (create-and-login))

  (let [name "df"]
    )


  (.click (find-element driver ".project-name .btn"))

  )

