(ns hatnik.web.client.user
  (:require goog.net.XhrIo
            [hatnik.web.client.app-state :as state]))

(.send goog.net.XhrIo "/api/current-user" state/update-user-data)

