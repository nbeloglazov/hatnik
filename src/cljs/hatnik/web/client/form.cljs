(ns hatnik.web.client.form
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [hatnik.web.client.form-components :as widget]
            [hatnik.web.client.app-state :as state]
            [hatnik.web.client.z-actions :as action])
  (:use [jayq.core :only [$]]))

(om/root widget/email-action-form state/app-state
         {:target (. js/document (getElementById "iModal"))})

;; Keyboard actions

;; Hidden modal by ESC
(.keydown ($ js/document) 
           (fn [e]
             (when (= 27 (.-keyCode e))
               (.modal ($ :#iModal) "hide")
               (.modal ($ :#iModalProject) "hide")
               (.modal ($ :#iModalProjectMenu) "hide"))))


(defn update-email-artifact-status [reply]
  (let [data (js->clj reply)]
    (if (= "ok" (get data "result"))
      (.addClass ($ :#artifact-input-group) "has-success")
      (.addClass ($ :#artifact-input-group) "has-error"))))

(def timeout-id (atom nil))

(.keyup
 ($ :#artifact-input)
 (fn [e]
   (.removeClass
    ($ :#artifact-input-group)
    "has-warning has-success has-error")
   (when-let [id @timeout-id]
     (js/clearTimeout id))
   (let [check-library (fn []
                         (reset! timeout-id nil)
                         (action/get-library
                          (.-value (.getElementById js/document "artifact-input"))
                          update-email-artifact-status))]
    (reset! timeout-id
            (js/setTimeout check-library 300)))))
