(ns hatnik.web.client.form-components
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(defn email-action-form [data]
  (dom/div 
   #js {:className "modal-content"}
   (dom/div #js {:className "modal-header"}
            (dom/h4 #js {:className "modal-title"}
                    "Add email notification"))
   (dom/div 
    #js {:className "modal-body"}
    (dom/form 
     #js {:className "email-action-form"}
     (dom/div #js {:className "form-group"}
              (dom/label #js {:for "artifact-input"} "Artifact")
              (dom/input #js {:type "text"
                              :className "form-control"
                              :id "artifact-input"
                              :placeholder "Artifact name"}))
     (dom/div #js {:className "form-group"}
              (dom/label #js {:for "emain-input"} "Email")
              (dom/input #js {:type "email"
                              :className "form-control"
                              :id "emain-input"
                              :placeholder "Artifact"})
     (dom/div #js {:className "form-group"}
              (dom/label #js {:for "emain-body-input"} "Email body")
              (dom/textarea #js {:cols "40"
                                 :className "form-control"
                                 :id "emain-body-input"}
                                 "Library release: {{LIBRARY}} {{VERSION}}"))
     )))
   (dom/div 
    #js {:className "modal-footer"}
    (dom/button #js {:className "btn btn-primary"} "Submit")
    (dom/button #js {:className "btn btn-default"} "Test"))))

(defn project-adding-from [data]
  (dom/div 
   #js {:className "modal-content"}
   (dom/div #js {:className "modal-header"}
            (dom/h4 #js {:className "modal-title"}
                    "Add project"))))

(defn form-view [data owner]
  (reify
    om/IRender
    (render [this]
      (if (= :email-action (:form-type data))
        (email-action-form data)
        (project-adding-from data)))))
