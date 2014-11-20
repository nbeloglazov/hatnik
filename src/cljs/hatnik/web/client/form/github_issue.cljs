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
               (u/form-field {:data data
                              :field :gh-repo
                              :id "gh-repo"
                              :title "Repository"
                              :validator #(:repo-status state)
                              :placeholder "user/repo or organization/repo"
                              :type :text
                              :on-change #(let [repo (.. % -target -value)]
                                            (github-issue-on-change repo (:timer state) owner)
                                            (om/update! data :gh-repo repo))})
               (u/form-field {:data data
                              :field :title
                              :id "gh-issue-title"
                              :title "Title"
                              :validator schm/TemplateTitle
                              :type :text})
               (u/form-field {:data data
                              :field :body
                              :id "gh-issue-body"
                              :title "Body"
                              :validator schm/TemplateBody
                              :type :textarea})))))
