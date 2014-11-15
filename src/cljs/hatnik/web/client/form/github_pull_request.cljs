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
      {:file-status (if (s/check (schm/string-of-length 1 1024) (:file data))
                      "has-error"
                      "has-success")
       :regex-status (if (s/check (schm/string-of-length 1 128) (:regex data))
                       "has-error"
                       "has-success")
       :replacement-status (if (s/check (schm/string-of-length 1 128) (:replacement data))
                             "has-error"
                             "has-success")})

    om/IRenderState
    (render-state [this state]
      (let [id (:id data)
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
                                          :onClick #((:delete-handler data) id)}
                                     "Delete")))

                  (dom/div
                   #js {:className "panel-collapse collapse in"
                        :id content-div-id}
                   (dom/div #js {:className "panel-body"}
                            (dom/div #js {:className (str "form-group " (:file-status state))}
                                     (dom/label #js {:htmlFor file-id
                                                     :className "control-label"}
                                                "File")
                                     (dom/input #js {:type "text"
                                                     :id file-id
                                                     :value (:file data)
                                                     :placeholder "e.g. project.clj"
                                                     :onChange #(do
                                                                  (update-operation-item data :file (.. % -target -value))
                                                                  (if (s/check (schm/string-of-length 1 1024) (.. % -target -value))
                                                                    (om/set-state! owner :file-status "has-error")
                                                                    (om/set-state! owner :file-status "has-success")))
                                                     :className "form-control"}))

                            (dom/div #js {:className (str "form-group " (:regex-status state))}
                                     (dom/label #js {:htmlFor regex-id
                                                     :className "control-label"}
                                                "Regex")
                                     (dom/input #js {:type "text"
                                                     :id regex-id
                                                     :value (:regex data)
                                                     :placeholder "e.g. {{library}} \"[^\"]+\""
                                                     :onChange #(do
                                                                  (update-operation-item data :regex (.. % -target -value))
                                                                  (if (s/check (schm/string-of-length 1 128) (.. % -target -value))
                                                                    (om/set-state! owner :regex-status "has-error")
                                                                    (om/set-state! owner :regex-status "has-success")))
                                                     :className "form-control"}))

                            (dom/div #js {:className (str "form-group " (:replacement-status state))}
                                     (dom/label #js {:htmlFor replacement-id
                                                     :className "control-label"}
                                                "Replacement")
                                     (dom/input #js {:type "text"
                                                     :id replacement-id
                                                     :value (:replacement data)
                                                     :placeholder "e.g. {{library}} \"{{version}}\""
                                                     :onChange #(do
                                                                  (update-operation-item data :replacement (.. % -target -value))
                                                                  (if (s/check (schm/string-of-length 1 128) (.. % -target -value))
                                                                    (om/set-state! owner :replacement-status "has-error")
                                                                    (om/set-state! owner :replacement-status "has-success")))
                                                     :className "form-control"}))))))))))

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
                                          (dom/h4 nil "Operations"))

                                 (dom/div #js {:className "col-md-6"}
                                          (dom/div #js {:className "btn btn-primary pull-right"
                                                        :onClick #(add-new-operation data)}
                                                      "Add")))

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
      {:repo-status (let [v (:value (:repo data))]
                      (if (or (nil? v) (= "" v))
                        "has-warning"
                        "has-success"))
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
               (dom/div #js {:className (str "form-group " (:repo-status state))}
                        (dom/label #js {:htmlFor "gh-repo"
                                        :className "control-label"}
                                   "Repository")
                        (dom/input #js {:type "text"
                                        :id "gh-repo"
                                        :className "form-control"
                                        :value (:value (:repo data))
                                        :placeholder "user/repository or organization/repository"

                                        :onChange
                                        #(do
                                           ((:handler (:repo data)) (.. % -target -value))
                                           (if (s/check schm/GithubRepository (.. % -target -value))
                                             (om/set-state! owner :repo-status "has-error")
                                             (om/set-state! owner :repo-status "has-success")))}))

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
                                           (dom/div #js {:className (str "form-group " (:title-status state))}
                                                    (dom/label #js {:htmlFor "gh-title"
                                                                    :className "control-label"}
                                                               "Title")
                                                    (dom/input #js {:type "text"
                                                                    :id "gh-title"
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
                                                    (dom/label #js {:htmlFor "gh-body"
                                                                    :className "control-label"}
                                                               "Body")
                                                    (dom/textarea #js {:cols "40"
                                                                       :id "gh-body"
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
                        (dom/label #js {:htmlFor "gh-commit-message"
                                        :className "control-label"}
                                   "Commit message")
                        (dom/input #js {:type "text"
                                        :id "gh-commit-message"
                                        :value (:value (:commit-msg data))
                                        :onChange #(do
                                                     ((:handler (:commit-msg data))
                                                      (.. % -target -value))
                                                     (if (s/check (schm/string-of-length 1 2000) (.. % -target -value))
                                                       (om/set-state! owner :commit-msg-status "has-error")
                                                       (om/set-state! owner :commit-msg-status "has-success")))
                                        :className "form-control"}))

               (dom/div nil
                        (om/build pull-request-operations-list (-> data :operations)))))))
