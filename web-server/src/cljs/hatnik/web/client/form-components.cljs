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


;; For selecting action form header

(def action-forms-headers 
  {:email-action "Add email notification"})

(defn action-form-header [data owner]
  (reify
    om/IRender
    (render [this]
      (dom/h4 #js {:className "modal-title"}
              (get action-forms-headers
                   (:form-type data))))))


;; For selecting action form body

(defn email-action-form-body [data]
  (dom/form 
   #js {:id "email-action-form"}
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
                                   "Library release: {{LIBRARY}} {{VERSION}}")))))

(def action-form-bodys 
  {:email-action email-action-form-body})

(defn action-form-body [data body]
  (reify
    om/IRender
    (render [this]
      (apply 
       (get action-form-bodys (:form-type data))
       []))))

;; For selecting footer

(defn email-action-footer [data]
  (dom/div nil
           (dom/button #js {:className "btn btn-primary"} "Submit")
           (dom/button #js {:className "btn btn-default"} "Test")))

(def action-form-footers
  {:email-action email-action-footer})

(defn action-form-footer [data body]
  (reify
    om/IRender
    (render [this]
      (apply 
       (get action-form-footers (:form-type data))
       []))))
