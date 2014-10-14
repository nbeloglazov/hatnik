(ns hatnik.web.client.project-list
  (:require goog.net.XhrIo
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [hatnik.web.client.components :as widget]
            [hatnik.web.client.app-state :as state]))

(om/root widget/project-list state/app-state
         {:target (. js/document (getElementById "iProjectList"))}) 

(.send goog.net.XhrIo "/api/current-user" state/update-user-data)
(.send goog.net.XhrIo "/api/projects" state/update-projects-list)


