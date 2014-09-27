(ns hatnik.web.client.actions
  (:require goog.net.XhrIo
            [jayq.core :as jq])
  (:use [jayq.core :only [$]]))

(defn callback [reply]
  (js/alert reply))

(defn send-new-project-request []
  (let [name (.-value (.getElementById js/document "project-name-input"))]
    (.log js/console (clj->js {:name name}))
    (if (= "" name)
      (js/alert "Project name must be not empty!")
      (jq/xhr ["POST" "/api/projects"] 
              {:name name}
              callback))))
