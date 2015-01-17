(ns hatnik.web.client.form.github-pull-request
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [hatnik.web.client.z-actions :as action]
            [hatnik.web.client.utils :as u]
            [hatnik.schema :as schm])
  (:use [clojure.string :only [split replace]]))

(defn pull-request-operation [data owner]
  (reify
    om/IRender
    (render [this]
      (let [id (:id data)
            oper (:data data)
            div-id (str "gh-pull-req-op-" id)
            content-div-id (str "gh-pull-req-op-col-" id)]
        (dom/div #js {:className "clearfix operation"}
                 (u/form-field {:data oper
                                :field :file
                                :id (str "gh-pull-req-op-file-" id)
                                :title "File"
                                :validator (schm/string-of-length 1 1024)
                                :placeholder "e.g. project.clj"
                                :type :text
                                :popover "path to the file to be edited relative to repo root"})
                 (u/form-field {:data oper
                                :field :regex
                                :id (str "gh-pull-req-op-regex-" id)
                                :title "Regex"
                                :validator (schm/string-of-length 1 128)
                                :placeholder "e.g. {{library}} \"[^\"]+\""
                                :type :text
                                :popover (str "supports template variables like {{library}} and others, "
                                              "uses java regex syntax")})
                 (u/form-field {:data oper
                                :field :replacement
                                :id (str "gh-pull-req-op-repl-" id)
                                :title "Replacement"
                                :validator (schm/string-of-length 1 128)
                                :placeholder "e.g. {{library}} \"{{version}}\""
                                :type :text
                                :popover (str "supports template variables like {{library}} and others, "
                                              "supports backreferences, uses java regex syntax")})
                 (dom/div #js {:className "dropdown pull-right"}
                          (dom/div #js {:className "btn btn-danger"
                                        :onClick #((:delete data) id)}
                                   "Delete")))))))

(defn add-new-operation [data]
  (om/transact! data #(conj % {:file "" :regex "" :replacement ""})))

(defn pull-request-operations-list [data owner]
  (reify
    om/IRender
    (render [this]
      (letfn [(remove-nth [coll ind]
                (keep-indexed (fn [i v]
                                (if (not= i ind) v nil))
                              coll))
              (delete-operation [id]
                (om/transact! data
                  #(vec (remove-nth % id))))]
        (dom/div #js {:className "clearfix"}
                 (apply dom/div #js {:className "operations"}
                        (map-indexed (fn [id operation]
                                       (om/build pull-request-operation
                                                 {:id id
                                                  :data operation
                                                  :delete delete-operation}))
                                     data))
                 (dom/div #js {:className "btn btn-primary pull-right add-operation"
                               :onClick #(add-new-operation data)}
                          "Add"))))))

(defn file-operation-type-selector [data]
  (let [operation-type (:file-operation-type data)]
    (dom/div
     #js {:className "form-group action-type-component has-success"}
     (dom/label
      #js {:htmlFor "file-operation-type"
           :className "control-label col-sm-2 no-padding-right"}
      "Operation")
     (dom/div
      #js {:className "col-sm-10"}
      (apply dom/select #js {:className "form-control"
                             :id "file-operation-type"
                             :defaultValue operation-type
                             :onChange #(om/update! data :file-operation-type (.. % -target -value))}
             (for [[text value] [["Update project.clj" "project.clj"]
                                 ["Manual" "manual"]]]
               (dom/option #js {:className "form-control"
                                :value value
                                :selected (if (= value operation-type)
                                            "selected" nil)}
                           text)))))))

(defn github-pull-request-component [data owner]
  (reify
    om/IRender
    (render [this]
      (dom/div nil
               (u/form-field {:data data
                              :field :gh-repo
                              :id "gh-repo"
                              :title "Repository"
                              :validator schm/GithubRepository
                              :placeholder "user/repo or organization/repo"
                              :type :text})
               (u/form-field {:data data
                              :field :title
                              :id "gh-title"
                              :title "Title & commit"
                              :validator schm/TemplateTitle
                              :type :text
                              :popover "supported variables: {{library}} {{version}} {{previous-version}}"})
               (u/form-field {:data data
                              :field :body
                              :id "gh-body"
                              :title "Body"
                              :validator schm/TemplateBody
                              :type :textarea
                              :popover (str "supported variables: {{library}} {{version}} {{previous-version}} "
                                            "{{results-table}}")})
               (file-operation-type-selector data)
               (when (= (:file-operation-type data) "manual")
                 (om/build pull-request-operations-list (:file-operations data)))))))
