(ns hatnik.web.client.form
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [hatnik.web.client.form-components :as widget]
            [hatnik.web.client.app-state :as state])
  (:use [jayq.core :only [$]]))

(om/root widget/action-form-header state/app-state
         {:target (. js/document (getElementById "iActionFormHeader"))})

(om/root widget/action-form-body state/app-state
         {:target (. js/document (getElementById "iActionFormBody"))})

(om/root widget/action-form-footer state/app-state
         {:target (. js/document (getElementById "iActionFormFooter"))})

