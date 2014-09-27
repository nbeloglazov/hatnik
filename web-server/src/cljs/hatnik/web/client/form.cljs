(ns hatnik.web.client.form
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [hatnik.web.client.form-components :as widget]
            [hatnik.web.client.app-state :as state])
  (:use [jayq.core :only [$]]))

(om/root widget/form-view state/app-state
         {:target (. js/document (getElementById "iModalContent"))})

