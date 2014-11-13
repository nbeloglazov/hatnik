(ns hatnik.web.client.form.artifact-input
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [hatnik.web.client.z-actions :as action]
            [hatnik.schema :as schm]
            [schema.core :as s]))

(defn set-state [owner status version in-progress?]
  (om/set-state! owner :form-status status)
  (om/set-state! owner :version version)
  (om/set-state! owner :request-in-progress? in-progress?))

(defn library-check-handler [reply owner]
  (let [resp (js->clj reply)
        status (if (= "ok" (get resp "result"))
                 "has-success"
                 "has-error")]
    (set-state owner status (get resp "version") false)))

(defn check-library-exists [owner timer library]
  (js/clearTimeout timer)
  (set-state owner "has-warning" nil false)
  (let [timer (js/setTimeout
               (fn []
                 (om/set-state! owner :request-in-progress? true)
                 (action/get-library library #(library-check-handler % owner)))
               1000)]
    (om/set-state! owner :timer timer)))

(defn artifact-input-component [data owner]
  (reify
    om/IInitState
    (init-state [this]
      {:timer nil
       :form-status (if (= "" (:value data))
                      "has-warning"
                      "has-success")
       :value (:value data)
       :version (:version data)
       :request-in-progress? false})

    om/IRenderState
    (render-state [this state]
      (dom/div #js {:className (str "form-group " (:form-status state))
                    :id "artifact-input-group"}
        (dom/label #js {:htmlFor "artifact-input"
                        :className "control-label"} "Library")
        (dom/input #js {:type "text"
                        :id "artifact-input"
                        :className "form-control"
                        :placeholder "e.g. org.clojure/clojure"
                        :onChange #(let [library (.. % -target -value)]
                                     (check-library-exists owner (:timer state) library)
                                     ((:handler data) library))
                        :value (:value data)})
               (dom/span #js {:className "form-control-feedback"}
                 (cond (= (:form-status state) "has-error") "Not found"
                       (= (:form-status state) "has-success") (:version state)
                       (:request-in-progress? state) (dom/img #js {:src "/img/ajax-loader.gif"})
                       :else nil))))))
