(ns hatnik.web.client.form.library-input
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [hatnik.web.client.z-actions :as action]
            [hatnik.web.client.utils :as u]))

(defn set-state [owner status version in-progress?]
  (om/set-state! owner :form-status status)
  (om/set-state! owner :version version)
  (om/set-state! owner :request-in-progress? in-progress?))

(defn library-check-handler [reply owner]
  (let [resp (js->clj reply)
        status (if (= "ok" (:result resp))
                 "has-success"
                 "has-error")]
    (set-state owner status (:version resp) false)))

(defn check-library-exists [owner timer library]
  (js/clearTimeout timer)
  (set-state owner "has-warning" nil false)
  (let [timer (js/setTimeout
               (fn []
                 (om/set-state! owner :request-in-progress? true)
                 (action/get-library library #(library-check-handler % owner)))
               1000)]
    (om/set-state! owner :timer timer)))

(defn library-input-component [data owner]
  (reify
    om/IInitState
    (init-state [this]
      {:timer nil
       :form-status (if (= "" (:library data))
                      "has-warning"
                      "has-success")
       :version (:library-version data)
       :request-in-progress? false})

    om/IRenderState
    (render-state [this state]
      (u/form-field {:data data
                     :field :library
                     :id "library-input"
                     :title "Library"
                     :placeholder "e.g. org.clojure/clojure"
                     :type :text
                     :validator #(:form-status state)
                     :on-change #(let [library (u/ev-value %)]
                                   (check-library-exists owner (:timer state) library)
                                   (om/update! data :library library))
                     :feedback (dom/span #js {:className "form-control-feedback"}
                                         (cond (= (:form-status state) "has-error") "Not found"
                                               (= (:form-status state) "has-success") (:version state)
                                               (:request-in-progress? state) (dom/img #js {:src "/img/ajax-loader.gif"})
                                               :else nil))}))))
