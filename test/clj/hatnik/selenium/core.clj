(ns hatnik.selenium.core
  (:import [org.openqa.selenium By WebDriver]
           org.openqa.selenium.firefox.FirefoxDriver
           [org.openqa.selenium.support.ui ExpectedConditions WebDriverWait
            ExpectedCondition])
  (:require [clojure.test :refer [is]]
            [hatnik.web.server.handler :refer [map->WebServer]]
            [com.stuartsierra.component :as component]
            [hatnik.test-utils :refer :all]
            [taoensso.timbre :as timbre]))

(let [id (atom 0)]
  (defn generate-user-email []
    (str "email-" (swap! id inc)
         "@example.com")))

(defn create-driver []
  (FirefoxDriver.))

(defn find-element [driver-or-element selector]
  (.findElement driver-or-element
                (By/cssSelector selector)))

(defn find-elements [driver-or-element selector]
  (.findElements driver-or-element
                (By/cssSelector selector)))

(defn element->action [element]
  {:library (.getText (find-element element ".library-name"))
   :type (.getText (find-element element ".action-name"))
   :element element})

(defn element->project [element]
  (let [action-divs (find-elements element ".action")]
    (is (-> action-divs last .getText (.contains "Add action")))
    {:name (.getText (find-element element ".project-name"))
     :actions (doall (map element->action (butlast action-divs)))
     :element element}))

(defn find-projects-on-page [driver]
  (doall (map element->project
              (find-elements driver "#iProjectList > *"))))

(defn login [driver]
  (doto driver
    (.get (str "http://localhost:"
               test-web-port
               "/api/force-login?email="
               (generate-user-email)
               "&skip-dummy-data=true"))
    (.get (str "http://localhost:" test-web-port))))

(defn clean-projects [projects]
  (letfn [(clean-action [action]
            (select-keys action [:type :library]))
          (clean-project [project]
            (-> project
                (update-in [:actions] #(map clean-action %))
                (select-keys [:name :actions])))]
    (map clean-project projects)))

(defn create-and-login []
  (let [driver (create-driver)]
    (login driver)
    ; Check that initially only "Default" project is present
    ; with no actions.
    (is (= [{:name "Default"
             :actions []}]
           (clean-projects (find-projects-on-page driver)))
        "Initial state not valid.")
    driver))

(defn wait-until-visible [driver element]
  (.until (WebDriverWait. driver 10)
          (ExpectedConditions/visibilityOf element)))

(defn wait-until [driver condition]
  (.until (WebDriverWait. driver 10)
          (reify ExpectedCondition
            (apply [this driver]
              (condition driver)))))

(defn wait-until-projects-match [driver projects]
  (wait-until driver
     #(= projects
         (clean-projects (find-projects-on-page %)))))

(comment

  (def driver (FirefoxDriver.))

  (create-and-login)
)
