(ns hatnik.web.client.form.github-issue
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [hatnik.web.client.z-actions :as action]
            [hatnik.web.client.utils :as u]
            [hatnik.schema :as schm])
  (:use [clojure.string :only [split replace]]))

(defn set-repo-status [owner status]
  (om/set-state! owner :repo-status status))

(defn github-repos-handler [reply owner repo]
  (let [rest (js->clj reply)
        exists? (some #(= repo (get % "name")) rest)]
    (set-repo-status owner
                     (if exists? "has-success" "has-error"))))

(defn github-issue-on-change [gh-repo timer owner]
  (js/clearTimeout timer)
  (set-repo-status owner "has-warning")
  (let [[user repo] (split gh-repo "/")]
    (when-not (or (nil? repo) (nil? user)
                  (= "" user) (= "" repo))
      (let [timer (js/setTimeout
                   (fn []
                     (action/get-github-repos
                      user
                      #(github-repos-handler % owner repo)
                      #(set-repo-status owner "has-error")))
                   1000)]
        (om/set-state! owner :timer timer)))))

(defn github-issue-component [data owner]
  (reify
    om/IInitState
    (init-state [this]
      {:repo-status
       (if (empty? (:gh-repo data))
         "has-warning"
         "has-success")
       :timer nil})
    om/IRenderState
    (render-state [this state]
      (dom/div nil
               (dom/div #js {:className (str "form-group " (:repo-status state))}
                        (dom/label #js {:htmlFor "gh-repo"
                                        :className "control-label"} "Repository")
                        (dom/input #js {:type "text"
                                        :className "form-control"
                                        :id "gh-repo"
                                        :value (:gh-repo data)
                                        :placeholder "user/repository or organization/repository"

                                        :onChange
                                        #(let [repo (.. % -target -value)]
                                           (github-issue-on-change repo (:timer state) owner)
                                           (om/update! data :gh-repo repo))}))

               (dom/div #js {:className (str "form-group "
                                             (u/validate schm/TemplateTitle (:title data)))}
                        (dom/label #js {:htmlFor "gh-issue-title"
                                        :className "control-label"} "Title")
                        (dom/input #js {:type "text"
                                        :className "form-control"
                                        :id "gh-issue-title"
                                        :value (:title data)
                                        :onChange #(om/update! data :title (.. % -target -value))}))
               (dom/div #js {:className (str "form-group "
                                             (u/validate schm/TemplateBody (:body data)))}
                        (dom/label #js {:htmlFor "gh-issue-body"
                                        :className "control-label"} "Body")
                        (dom/textarea #js {:cols "40"
                                           :className "form-control"
                                           :id "gh-issue-body"
                                           :value (:body data)
                                           :onChange #(om/update! data :body (.. % -target -value))}))))))
