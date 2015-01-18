(ns hatnik.selenium.core
  (:import [org.openqa.selenium By WebDriver TakesScreenshot OutputType]
           org.openqa.selenium.firefox.FirefoxDriver
           [org.openqa.selenium.remote DesiredCapabilities CapabilityType]
           [org.openqa.selenium.logging LogType LoggingPreferences]
           [org.openqa.selenium.support.ui ExpectedConditions WebDriverWait
            ExpectedCondition]
           java.util.logging.Level)
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

(def selenium-log-types [LogType/BROWSER LogType/DRIVER])

(defn create-driver []
  (let [logging (LoggingPreferences.)]
    (doseq [log-type selenium-log-types]
      (.enable logging log-type Level/SEVERE))
    (FirefoxDriver. (doto (DesiredCapabilities/firefox)
                      (.setCapability CapabilityType/LOGGING_PREFS logging)))))

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
  (let [action-divs (find-elements element ".action")
        build-file (if-let [el (first (find-elements element
                                                     ".project-name small"))]
                     (.getText el)
                     nil)
        last-action-text (-> action-divs last .getText)
        base {:name (.getText (find-element element ".project-name span"))
              :element element}]
    (if build-file
      (assert (not (.contains last-action-text "Add action")))
      (assert (.contains last-action-text "Add action")))
    (if build-file
      (assoc base
             :build-file build-file
             :actions (doall (map element->action action-divs)))
      (assoc  base
              :actions (doall (map element->action (butlast action-divs)))))))

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
                (select-keys [:name :actions :build-file])))]
    (map clean-project projects)))

(defn wait-until [driver condition message]
  (doto (WebDriverWait. driver 10)
    (.withMessage message)
    (.until (reify ExpectedCondition
              (apply [this driver]
                (condition driver))))))

(defn create-and-login []
  (let [driver (create-driver)]
    (login driver)
    ; Check that initially only "Default" project is present
    ; with no actions.
    (wait-until driver #(= [{:name "Default"
                             :actions []}]
                           (clean-projects (find-projects-on-page %)))
        "Initial state not valid. 'Default' project should be created.")
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
              ExpectedConditions/invisibilityOfElementLocated))
  ; Usually we're waiting until dialog becomes invisible as a sign
  ; of completed action. But it's not reliable as some om/react stuff
  ; might be async. So let's just wait for a little more longer explicitly.
  (Thread/sleep 1000))

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

(defn set-input-text-from-map
  "Given map id->value sets value to all <input> fields for corresponding
  ids."
  [driver map]
  (doseq [[id text] map]
    (set-input-text driver (str "#" (name id)) text)))

(defn take-screenshot [driver]
  (let [file (.getScreenshotAs driver OutputType/FILE)
        result (client/post "http://expirebox.com/jqu/"
                            {:multipart [{:name "files[]"
                                          :content file}]
                             :accept :json
                             :as :json})]
    (->> result :body :files first :fileKey
         (format "http://expirebox.com/download/%s.html"))))

(defn print-logs [driver type]
  (let [entries (.. driver manage logs (get type))]
    (when-not (empty? entries)
      (println)
      (println "######### Webdriver" type "log #########")
      (doseq [entry entries]
        (println (str entry)))
      (println))))

(defn run-with-driver [test-fn]
  (let [driver (create-and-login)]
    (try
      (test-fn driver)
      (catch Exception e
        (timbre/error "Screenshot:" (take-screenshot driver))
        (throw e))
      (finally
        (doseq [log-type selenium-log-types]
          (print-logs driver log-type))
        (.quit driver)))))

(defn action-params
  "Assumes that action form is opened. Extracts all fields from the form
  and returns it as a map. Also works for build-file project which has
  action form embedded."
  [driver]
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
                               :gh-title :gh-body :file-operation-type
                               :build-file]
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

(defn set-select-value
  "Hacky way to change value of <select> element.
  For some reason proper way by using selenium Select object
  and calling .selectByValue() on it doesn't work on travis."
  [driver id value]
  (.executeScript driver
                  (str "$('#" id "').val('" value "');"
                       "(function() { "
                       "  var evt = document.createEvent('HTMLEvents');"
                       "  evt.initEvent('change', true, true);"
                       "  $('#" id "')[0].dispatchEvent(evt);"
                       "})();")
                  (into-array []))
  (Thread/sleep 500))

(defn change-action-type [driver type]
  (set-select-value driver "action-type" type))

(comment

  (def driver (create-driver))


  (.get driver "http://nbeloglazov.com")

  (take-screenshot driver)

  (create-and-login)
)
