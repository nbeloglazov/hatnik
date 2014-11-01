(ns hatnik.web.client.form.github-pull-request
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [hatnik.web.client.z-actions :as action]
            [hatnik.schema :as schm]
            [schema.core :as s])
  (:use [clojure.string :only [split replace]]))

(defn github-issue-on-change [gh-repo timer form-handler form-status-handler error-handler]
  (js/clearTimeout timer)  
  
  (let [pr (split gh-repo "/")
        user (first pr)
        repo (second pr)]
    (form-handler 
     (if (or (nil? repo)
             (nil? user)
             (= "" user)
             (= "" repo))
       (do
         (form-status-handler "has-warning")
         nil)

       (js/setTimeout
        (fn [] 
          (action/get-github-repos 
           user 
           (fn [reply] 
             (let [rest (js->clj reply)
                   result (->> rest
                               (map #(get % "name"))
                               (filter #(= % repo)))]
               (if (first result)
                 (form-status-handler "has-success")
                 (form-status-handler "has-error"))))
           error-handler))
        1000)))))


(defn update-operation-item [data key new-val]
  ((:handler data)
   (:id data)
   (assoc {:file (:file data)
           :regex (:regex data)
           :replace (:replace data)}
     key new-val)))

(defn pull-request-operation [data owner]
  (reify
    om/IRender
    (render [this]
      (dom/div
       #js {:className "panel-group"
            :id (str "gh-pull-req-op-" (:id data))}
       (dom/div #js {:className "panel panel-default"}
                (dom/div
                 #js {:className "panel-heading"}
                 (dom/h4 #js {:className "panel-title"}
                         (dom/a #js {:data-toggle "collapse"
                                     :data-parent (str "#gh-pull-req-op-" (:id data))
                                     :href (str "#gh-pull-req-op-col-" (:id data))}
                                (str "Operation №" (+ 1 (:id data))))))
                
                (dom/div
                 #js {:className "panel-collapse collapse in"
                      :id (str "gh-pull-req-op-col-" (:id data))}
                 (dom/div #js {:className "panel-body"}
                          (dom/div #js {:className "form-group"}
                                   (dom/label nil "file")
                                   (dom/input #js {:type "text"
                                                   :value (:file data)
                                                   :onChange #(update-operation-item data :file (.. % -target -value))
                                                   :className "form-control"}))
                          (dom/div #js {:className "form-group"}
                                   (dom/label nil "regex")
                                   (dom/input #js {:type "text"
                                                   :value (:regex data)
                                                   :onChange #(update-operation-item data :regex (.. % -target -value))
                                                   :className "form-control"}))
                          (dom/div #js {:className "form-group"}
                                   (dom/label nil "replacement")
                                   (dom/input #js {:type "text"
                                                   :value (:replacement data)
                                                   :onChange #(update-operation-item
                                                               data :replacement (.. % -target -value))
                                                   :className "form-control"})))))))))

(defn add-new-operation [data]
  ((:handler data) (conj (:value data) {:file "" :regex "" :replace ""})))

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
                                                                           (assoc (:value data) id val)))}
                                                              %2))
                                            (:value data))))))))

(defn github-pull-request-component [data owner]
  (reify
    om/IInitState
    (init-state [this] 
      {})

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
                                        #(github-issue-on-change
                                          (.. % -target -value)
                                          (:timer state)
                                          (fn [t]
                                            ((:handler (:repo data)) (.. % -target -value))
                                            (om/set-state! owner :timer t))
                                          (fn [st] (om/set-state! owner :form-status st))
                                          (fn [st] (om/set-state! owner :form-status "has-error")))
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
                                           (dom/div #js {:className "form-group"}
                                                    (dom/label nil "Title")
                                                    (dom/input #js {:type "text"
                                                                    :value (:value (:pull-title data))
                                                                    :onChange
                                                                    #((:handler (:pull-title data))
                                                                      (.. % -target -value))
                                                                    :className "form-control"}))

                                           (dom/div #js {:className "form-group"}
                                                    (dom/label nil "Body")
                                                    (dom/textarea #js {:cols "40"
                                                                       :value (:value (:pull-body data))
                                                                       :onChange
                                                                       #((:handler (:pull-body data))
                                                                         (.. % -target -value))
                                                                       :className "form-control"}))))))

               (dom/div #js {:className "form-group"}
                        (dom/label nil "Commit message")
                        (dom/input #js {:value (:value (:commit-msg data))
                                        :onChange #((:handler (:commit-msg data))
                                                    (.. % -target -value))
                                        :className "form-control"}))

               (dom/div nil
                        (om/build pull-request-operations-list (-> data :operations)))))))
