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
            content-div-id (str "gh-pull-req-op-col-" id)
            file-id (str "gh-pull-req-op-file-" id)
            regex-id (str "gh-pull-req-op-regex-" id)
            replacement-id (str "gh-pull-req-op-repl-" id)]
        (dom/div
         #js {:className "panel-group"
              :id id}
         (dom/div #js {:className "panel panel-default"}
                  (dom/div
                   #js {:className "panel-heading clearfix"}
                   (dom/h4 #js {:className "panel-title pull-left"}
                           (dom/a #js {:data-toggle "collapse"
                                       :data-parent (str "#" div-id)
                                       :href (str "#" content-div-id)}
                                  (str "Operation No. " (+ 1 id))))

                   (dom/div #js {:className "dropdown pull-right"}
                            (dom/div #js {:className "btn btn-danger"
                                          :onClick #((:delete data) id)}
                                     "Delete")))

                  (dom/div
                   #js {:className "panel-collapse collapse in"
                        :id content-div-id}
                   (dom/div #js {:className "panel-body"}
                            (dom/div #js {:className (str "form-group "
                                                          (u/validate (schm/string-of-length 1 1024) (:file oper)))}
                                     (dom/label #js {:htmlFor file-id
                                                     :className "control-label"}
                                                "File")
                                     (dom/input #js {:type "text"
                                                     :id file-id
                                                     :value (:file oper)
                                                     :placeholder "e.g. project.clj"
                                                     :onChange #(om/update! oper :file (.. % -target -value))
                                                     :className "form-control"}))

                            (dom/div #js {:className (str "form-group "
                                                          (u/validate (schm/string-of-length 1 128) (:regex oper)))}
                                     (dom/label #js {:htmlFor regex-id
                                                     :className "control-label"}
                                                "Regex")
                                     (dom/input #js {:type "text"
                                                     :id regex-id
                                                     :value (:regex oper)
                                                     :placeholder "e.g. {{library}} \"[^\"]+\""
                                                     :onChange #(om/update! oper :regex (.. % -target -value))
                                                     :className "form-control"}))

                            (dom/div #js {:className (str "form-group "
                                                          (u/validate (schm/string-of-length 1 128) (:replacement oper)))}
                                     (dom/label #js {:htmlFor replacement-id
                                                     :className "control-label"}
                                                "Replacement")
                                     (dom/input #js {:type "text"
                                                     :id replacement-id
                                                     :value (:replacement oper)
                                                     :placeholder "e.g. {{library}} \"{{version}}\""
                                                     :onChange #(om/update! oper :replacement (.. % -target -value))
                                                     :className "form-control"}))))))))))

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
       (dom/div nil
                (dom/div #js {:className "form-group"}
                         (dom/div #js {:className "row"}
                                  (dom/div #js {:className "col-md-6"}
                                           (dom/h4 nil "Operations"))

                                  (dom/div #js {:className "col-md-6"}
                                           (dom/div #js {:className "btn btn-primary pull-right"
                                                         :onClick #(add-new-operation data)}
                                                    "Add")))
                         (apply dom/div nil
                                (map-indexed (fn [id operation]
                                               (om/build pull-request-operation
                                                         {:id id
                                                          :data operation
                                                          :delete delete-operation}))
                                             data))))))))

(defn github-pull-request-component [data owner]
  (reify
    om/IRender
    (render [this]
      (dom/div nil
               (dom/div #js {:className (str "form-group "
                                             (u/validate schm/GithubRepository (:gh-repo data)))}
                        (dom/label #js {:htmlFor "gh-repo"
                                        :className "control-label"}
                                   "Repository")
                        (dom/input #js {:type "text"
                                        :id "gh-repo"
                                        :className "form-control"
                                        :value (:gh-repo data)
                                        :placeholder "user/repository or organization/repository"

                                        :onChange
                                        #(om/update! data :gh-repo (.. % -target -value))}))

               (dom/div
                #js {:className "panel-group" :id "github-pull-req-text"}
                (dom/div #js {:className "panel panel-default"}
                         (dom/div #js {:className "panel-heading"}
                                  (dom/h4 #js {:className "panel-title"}
                                          (dom/a #js {:data-toggle "collapse"
                                                      :data-parent "#github-pull-req-text"
                                                      :href "#github-pull-req-text-body"}
                                                 "Title and body")))

                         (dom/div #js {:className "panel-collapse collapse"
                                       :id "github-pull-req-text-body"}
                                  (dom/div #js {:className "panel-body"}
                                           (dom/div #js {:className (str "form-group "
                                                                         (u/validate schm/TemplateTitle (:title data)))}
                                                    (dom/label #js {:htmlFor "gh-title"
                                                                    :className "control-label"}
                                                               "Title")
                                                    (dom/input #js {:type "text"
                                                                    :id "gh-title"
                                                                    :value (:title data)
                                                                    :onChange
                                                                    #(om/update! data :title (.. % -target -value))
                                                                    :className "form-control"}))

                                           (dom/div #js {:className (str "form-group "
                                                                         (u/validate schm/TemplateBody (:body data)))}
                                                    (dom/label #js {:htmlFor "gh-body"
                                                                    :className "control-label"}
                                                               "Body")
                                                    (dom/textarea #js {:cols "40"
                                                                       :id "gh-body"
                                                                       :value (:body data)
                                                                       :onChange
                                                                       #(om/update! data :body (.. % -target -value))
                                                                       :className "form-control"}))))))

               (dom/div #js {:className (str "form-group "
                                             (u/validate (schm/string-of-length 1 2000) (:commit-message data)))}
                        (dom/label #js {:htmlFor "gh-commit-message"
                                        :className "control-label"}
                                   "Commit message")
                        (dom/input #js {:type "text"
                                        :id "gh-commit-message"
                                        :value (:commit-message data)
                                        :onChange #(om/update! data :commit-message (.. % -target -value))
                                        :className "form-control"}))

               (dom/div nil
                        (om/build pull-request-operations-list (:file-operations data)))))))
