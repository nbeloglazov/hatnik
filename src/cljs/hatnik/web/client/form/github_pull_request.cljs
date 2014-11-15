(ns hatnik.web.client.form.github-pull-request
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [hatnik.web.client.z-actions :as action]
            [hatnik.schema :as schm]
            [schema.core :as s])
  (:use [clojure.string :only [split replace]]))

(defn update-operation-item [data key new-val]
  ((:handler data)
   (:id data)
   (assoc {:file (:file data)
           :regex (:regex data)
           :replacement (:replacement data)}
     key new-val)))

(defn pull-request-operation [data owner]
  (reify
    om/IInitState
    (init-state [this]
      {:file-status (if (s/check (schm/string-of-length 1 2000) (:file data))
                      "has-error"
                      "has-success")
       :regex-status (if (s/check (schm/string-of-length 1 2000) (:regex data))
                       "has-error"
                       "has-success")
       :replacement-status (if (s/check (schm/string-of-length 1 2000) (:replacement data))
                             "has-error"
                             "has-success")
       })
    
    om/IRenderState
    (render-state [this state]
      (dom/div
       #js {:className "panel-group"
            :id (str "gh-pull-req-op-" (:id data))}
       (dom/div #js {:className "panel panel-default"}
                (dom/div
                 #js {:className "panel-heading clearfix"}
                 (dom/h4 #js {:className "panel-title pull-left"}
                         (dom/a #js {:data-toggle "collapse"
                                     :data-parent (str "#gh-pull-req-op-" (:id data))
                                     :href (str "#gh-pull-req-op-col-" (:id data))}
                                (str "Operation No. " (+ 1 (:id data)))))

                 (dom/div #js {:className "dropdown pull-right"}
                          (dom/div #js {:className "btn btn-danger"
                                        :onClick #((:delete-handler data) (:id data))}
                                   "Delete")))
                
                (dom/div
                 #js {:className "panel-collapse collapse in"
                      :id (str "gh-pull-req-op-col-" (:id data))}
                 (dom/div #js {:className "panel-body"}
                          (dom/div #js {:className (str "form-group " (:file-status state))}
                                   (dom/label nil "file")
                                   (dom/input #js {:type "text"
                                                   :value (:file data)
                                                   :onChange #(do
                                                                (update-operation-item data :file (.. % -target -value))
                                                                (if (s/check (schm/string-of-length 1 1024) (.. % -target -value))
                                                                  (om/set-state! owner :file-status "has-error")
                                                                  (om/set-state! owner :file-status "has-success")))
                                                   :className "form-control"}))
                          
                          (dom/div #js {:className (str "form-group " (:regex-status state))}
                                   (dom/label nil "regex")
                                   (dom/input #js {:type "text"
                                                   :value (:regex data)
                                                   :onChange #(do
                                                                (update-operation-item data :regex (.. % -target -value))
                                                                (if (s/check (schm/string-of-length 1 128) (.. % -target -value))
                                                                  (om/set-state! owner :regex-status "has-error")
                                                                  (om/set-state! owner :regex-status "has-success")))
                                                   :className "form-control"}))
                          
                          (dom/div #js {:className (str "form-group " (:replacement-status state))}
                                   (dom/label nil "replacement")
                                   (dom/input #js {:type "text"
                                                   :value (:replacement data)
                                                   :onChange #(do
                                                                (update-operation-item data :replacement (.. % -target -value))
                                                                (if (s/check (schm/string-of-length 1 128) (.. % -target -value))
                                                                  (om/set-state! owner :replacement-status "has-error")
                                                                  (om/set-state! owner :replacement-status "has-success")))
                                                   :className "form-control"})))))))))

(defn add-new-operation [data]
  ((:handler data) (conj (:value data) {:file "" :regex "" :replacement ""})))

(defn pull-request-operations-list [data owner]
  (reify
    om/IRender
    (render [this]
      (dom/div nil
               (dom/div #js {:className "form-group"}
                        (dom/div #js {:className "row"}
                                 (dom/div #js {:className "col-md-6"}
                                          (dom/h4 nil "Operations list"))

                                 (dom/div #js {:className "col-md-6"}
                                          (dom/div #js {:className "btn btn-primary pull-right"
                                                        :onClick #(add-new-operation data)}
                                                      "Add operation")))
                        
                        (apply dom/div nil
                               (map-indexed #(om/build pull-request-operation
                                                       (merge {:id %1
                                                               :handler (fn [id val]
                                                                          ((:handler data)
                                                                           (assoc (:value data) id val)))
                                                               :delete-handler (fn [id]
                                                                                 ((:handler data)
                                                                                  (mapv second
                                                                                        (filter (fn [pr] (not= (first pr) id))
                                                                                                (map-indexed (fn [i e] (list i e)) (:value data))))))}
                                                              %2))
                                            (:value data))))))))

(defn github-pull-request-component [data owner]
  (reify
    om/IInitState
    (init-state [this] 
      {:form-status "has-success"
       :body-status (if (s/check schm/TemplateBody (:value (:pull-body data)))
                      "has-error"
                      "has-success")
       :title-status (if (s/check schm/TemplateTitle (:value (:pull-title data)))
                       "has-error"
                       "has-success")
       :commit-msg-status (if (s/check (schm/string-of-length 1 2000) (:value (:commit-msg data)))
                            "has-error"
                            "has-success")})

    om/IRenderState
    (render-state [this state]
      (dom/div nil
               (dom/div #js {:className (str "form-group " (:form-status state))}
                        (dom/label nil "GitHub repository")
                        (dom/input #js {:type "text"
                                        :className "form-control"
                                        :value (:value (:repo data))
                                        :placeholder "user/repository or organization/repository"

                                        :onChange 
                                        #(do
                                           ((:handler (:repo data)) (.. % -target -value))
                                           (if (s/check schm/GithubRepository (.. % -target -value))
                                             (om/set-state! owner :form-status "has-error")
                                             (om/set-state! owner :form-status "has-success")))
                                        }))

               (dom/div
                #js {:className "panel-group" :id "github-pull-req-text"}
                (dom/div #js {:className "panel panel-default"}
                         (dom/div #js {:className "panel-heading"}
                                  (dom/h4 #js {:className "panel-title"}
                                          (dom/a #js {:data-toggle "collapse"
                                                      :data-parent "#github-pull-req-text"
                                                      :href "#github-pull-req-text-body"}
                                                 "GitHub pull request body")))
                         
                         (dom/div #js {:className "panel-collapse collapse"
                                       :id "github-pull-req-text-body"}
                                  (dom/div #js {:className "panel-body"}
                                           (dom/div #js {:className (str "form-group " (:title-status state))}
                                                    (dom/label nil "Title")
                                                    (dom/input #js {:type "text"
                                                                    :value (:value (:pull-title data))
                                                                    :onChange
                                                                    #(do
                                                                       ((:handler (:pull-title data))
                                                                        (.. % -target -value))
                                                                       (if (s/check schm/TemplateTitle (.. % -target -value))
                                                                         (om/set-state! owner :title-status "has-error")
                                                                         (om/set-state! owner :title-status "has-success")))
                                                                    :className "form-control"}))

                                           (dom/div #js {:className (str "form-group " (:body-status state))}
                                                    (dom/label nil "Body")
                                                    (dom/textarea #js {:cols "40"
                                                                       :value (:value (:pull-body data))
                                                                       :onChange
                                                                       #(do
                                                                          ((:handler (:pull-body data))
                                                                           (.. % -target -value))
                                                                          (if (s/check schm/TemplateBody (.. % -target -value))
                                                                            (om/set-state! owner :body-status "has-error")
                                                                            (om/set-state! owner :body-status "has-success")))
                                                                       :className "form-control"}))))))

               (dom/div #js {:className (str "form-group " (:commit-msg-status state))}
                        (dom/label nil "Commit message")
                        (dom/input #js {:value (:value (:commit-msg data))
                                        :onChange #(do
                                                     ((:handler (:commit-msg data))
                                                      (.. % -target -value))
                                                     (if (s/check (schm/string-of-length 1 2000) (.. % -target -value))
                                                       (om/set-state! owner :commit-msg-status "has-error")
                                                       (om/set-state! owner :commit-msg-status "has-success")))
                                        :className "form-control"}))

               (dom/div nil
                        (om/build pull-request-operations-list (-> data :operations)))))))
