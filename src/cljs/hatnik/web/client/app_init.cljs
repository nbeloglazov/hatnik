(ns hatnik.web.client.app-init
  (:require [om.core :as om :include-macros true]
            [hatnik.web.client.components :as widget]
            [hatnik.web.client.app-state :as state]))

(om/root widget/app-view state/app-state
         {:target (. js/document (getElementById "iAppView"))})



