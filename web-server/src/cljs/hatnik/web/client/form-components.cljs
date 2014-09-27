(ns hatnik.web.client.form-components
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(defn email-action-form [data]
  (dom/div nil
           "Add email action"))

(defn project-adding-from [data]
  (dom/div nil
           "Add a new project"))

(defn form-view [data owner]
  (reify
    om/IRender
    (render [this]
      (dom/div nil "Hello in FORM!"))))
