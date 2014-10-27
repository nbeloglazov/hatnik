(ns hatnik.web.client.form.github-issue
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

(defn github-issue-component [data owner]
  (reify
    om/IInitState
    (init-state [this]
      {:form-status 
       (let [v (:value (:repo data))]
         (if (or (nil? v) (= "" v))
           "has-warning"
           "has-success"))
       :title-status "has-success"
       :body-status "has-success"
       :timer nil
       })
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
               (dom/div #js {:className (str "form-group " (:title-status state))}
                        (dom/label nil "Issue title")
                        (dom/input #js {:type "text"
                                        :className "form-control"
                                        :value (:value (:title data))
                                        :onChange #(do
                                                     ((:handler (:title data)) (.. % -target -value))
                                                     (om/set-state! owner :title-status
                                                                    (if (s/check schm/TemplateTitle (.. % -target -value))
                                                                      "has-error"
                                                                      "has-success")))}))
               (dom/div #js {:className (str "form-group " (:body-status state))}
                        (dom/label nil "Issue body")
                        (dom/textarea #js {:cols "40"
                                           :className "form-control"
                                           :value (:value (:body data))
                                           :onChange #(do
                                                        ((:handler (:body data)) (.. % -target -value))
                                                        (om/set-state! owner :body-status
                                                                       (if (s/check schm/TemplateBody (.. % -target -value))
                                                                         "has-error"
                                                                         "has-success")))}))))))
