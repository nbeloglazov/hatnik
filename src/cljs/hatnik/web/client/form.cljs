(ns hatnik.web.client.form
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [hatnik.web.client.form-components :as widget]
            [hatnik.web.client.app-state :as state]
            [hatnik.web.client.z-actions :as action])
  (:use [jayq.core :only [$]]))

(om/root widget/action-form-header state/app-state
         {:target (. js/document (getElementById "iActionFormHeader"))})

(om/root widget/action-form-body state/app-state
         {:target (. js/document (getElementById "iActionFormBody"))})

(om/root widget/action-form-footer state/app-state
         {:target (. js/document (getElementById "iActionFormFooter"))})


;; Keyboard actions

;; Hidden modal by ESC
(.keydown ($ js/document) 
           (fn [e]
             (when (= 27 (.-keyCode e))
               (.modal ($ :#iModal) "hide")
               (.modal ($ :#iModalProject) "hide")
               (.modal ($ :#iModalProjectMenu) "hide"))))

