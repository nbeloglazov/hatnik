(ns hatnik.web.client.form.artifact-input
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [hatnik.web.client.z-actions :as action]
            [hatnik.schema :as schm]
            [schema.core :as s]))

(defn library-check-handler [reply callback]
  (let [resp (js->clj reply)]
    (if (= "ok" (get resp "result"))
      (callback "has-success" (get resp "version") nil)
      (callback "has-error" nil nil))))

(defn check-input-value [data-handler check-handler started-handler timer new-val]
  (js/clearTimeout timer)
  (check-handler
   "has-warning"
   nil
   (js/setTimeout
    (fn []
      (started-handler)
      (action/get-library new-val #(library-check-handler % check-handler)))
    1000))
  (data-handler new-val))

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
                        :onChange #(check-input-value
                                    (:handler data)
                                    (fn [result version timer]
                                      (om/set-state! owner :form-status result)
                                      (om/set-state! owner :version version)
                                      (om/set-state! owner :timer timer)
                                      (om/set-state! owner :request-in-progress? false))
                                    (fn []
                                      (om/set-state! owner :request-in-progress? true))
                                    (:timer state)
                                    (.. % -target -value))
                        :value (:value data)})
               (dom/span #js {:className "form-control-feedback"}
                 (cond (= (:form-status state) "has-error") "Not found"
                       (= (:form-status state) "has-success") (:version state)
                       (:request-in-progress? state) (dom/img #js {:src "/img/ajax-loader.gif"})
                       :else nil))))))
