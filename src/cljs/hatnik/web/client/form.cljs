(ns hatnik.web.client.form
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [hatnik.web.client.app-state :as state]
            [hatnik.web.client.z-actions :as action])
  (:use [jayq.core :only [$]]))

;; Keyboard actions

;; Hidden modal by ESC
(.keydown ($ js/document) 
           (fn [e]
             (when (= 27 (.-keyCode e))
               (.modal ($ :#iModal) "hide")
               (.modal ($ :#iModalProject) "hide")
               (.modal ($ :#iModalProjectMenu) "hide")
               (.modal ($ :#iModalAddAction) "hide"))))
