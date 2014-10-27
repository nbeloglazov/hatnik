(ns hatnik.web.client.form.artifact-input
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [hatnik.web.client.z-actions :as action]
            [hatnik.schema :as schm]
            [schema.core :as s]))

(defn library-check-handler [reply callback]
  (let [resp (js->clj reply)]
    (if (= "ok" (get resp "result"))
      (callback "has-success" nil)
      (callback "has-error" nil))))

(defn check-input-value [data-handler check-handler timer new-val]
  (js/clearTimeout timer)  
  (check-handler 
   "has-warning"
   (js/setTimeout 
    (fn []
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
       :value (:value data)})

    om/IRenderState
    (render-state [this state]
      (dom/div #js {:className (str "form-group " (:form-status state))
                    :id "artifact-input-group"}
               (dom/label #js {:htmlFor "artifact-input"} "Library")
               (dom/input #js {:type "text"
                               :className "form-control"
                               :placeholder "e.g. org.clojure/clojure"
                               :onChange #(check-input-value 
                                           (:handler data) 
                                           (fn [x timer] 
                                             (om/set-state! owner :form-status x)
                                             (om/set-state! owner :timer timer))
                                           (:timer state)
                                           (.. % -target -value))
                               :value (:value data)})))))
