(ns hatnik.web.client.app-init
  (:require goog.net.XhrIo
            [om.core :as om :include-macros true]
            [hatnik.web.client.components :as widget]
            [hatnik.web.client.app-state :as state]))

(om/root widget/project-list state/app-state
         {:target (. js/document (getElementById "iProjectList"))}) 

(.send goog.net.XhrIo "/api/current-user" state/update-user-data)
(.send goog.net.XhrIo "/api/projects" state/update-projects-list)


