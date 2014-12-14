(ns hatnik.selenium.core
  (:import [org.openqa.selenium By WebDriver TakesScreenshot OutputType]
           org.openqa.selenium.firefox.FirefoxDriver
           [org.openqa.selenium.support.ui ExpectedConditions WebDriverWait
            ExpectedCondition])
  (:require [clojure.test :refer [is]]
            [hatnik.web.server.handler :refer [map->WebServer]]
            [com.stuartsierra.component :as component]
            [hatnik.test-utils :refer :all]
            [taoensso.timbre :as timbre]
            [clj-http.client :as client]))

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
    (.executeScript driver "window.javascriptErrors = [];" (into-array []))
    (.executeScript driver (str "window.onerror = function(error) {"
                                "  window.javasscriptErrors.push(error);"
                                "};")
                    (into-array []))
    driver))

(def dialog-visible-selector {:project "#iModalProjectMenu .modal-dialog"
                              :action "#iModalAddAction .modal-dialog"})

(defn wait-until-dialog-visible [driver type]
  (.until (WebDriverWait. driver 10)
          (-> type dialog-visible-selector
              By/cssSelector
              ExpectedConditions/visibilityOfElementLocated)))

(def dialog-invisible-selector {:project "#iModalProjectMenu"
                                :action "#iModalAddAction"})

(defn wait-until-dialog-invisible [driver type]
  (.until (WebDriverWait. driver 10)
          (-> type dialog-invisible-selector
              By/cssSelector
              ExpectedConditions/invisibilityOfElementLocated)))

(defn wait-until [driver condition message]
  (doto (WebDriverWait. driver 10)
    (.withMessage message)
    (.until (reify ExpectedCondition
              (apply [this driver]
                (condition driver))))))

(defn wait-until-projects-match [driver projects]
  (wait-until driver
     ; Sometimes DOM update happens while we're collecting
     ; data about projects from page and "DOM element not found"
     ; is thrown. As workaround we simply consider it false and
     ; hope that on next try it will work.
     #(try
        (= projects
           (clean-projects (find-projects-on-page %)))
        (catch Exception e false))
     (str "Wait for projects to be " (pr-str projects))))

(defn set-input-text [driver selector text]
  (let [input (find-element driver selector)]
    (.clear input)
    (.sendKeys input (into-array [text]))))

(defn take-screenshot [driver]
  (let [file (.getScreenshotAs driver OutputType/FILE)
        result (client/post "http://expirebox.com/jqu/"
                            {:multipart [{:name "files[]"
                                          :content file}]
                             :accept :json
                             :as :json})]
    (->> result :body :files first :fileKey
         (format "http://expirebox.com/download/%s.html"))))

(defn fail-report [driver]
  (timbre/error "Screenshot:" (take-screenshot driver))
  (timbre/error "JS errors:" (.executeScript driver "return window.javascriptErrors;"
                                             (into-array []))))

(comment

  (def driver (FirefoxDriver.))


  (take-screenshot driver)

  (create-and-login)
)
