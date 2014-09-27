(ns hatnik.web.server.renderer
  (:require [hiccup.core :as hc]))

(defn page-html-head []
  (hc/html
   [:head
    [:title "Hatnik"]
    [:link {:rel "stylesheet" :href "/css/bootstrap.min.css"}]
    [:link {:rel "stylesheet" :href "/css/hatnik.css"}]]))


(defn page-header [])

(defn core-page [req]
  (hc/html
   (page-html-head)
   
   [:body
    (page-header)

    [:div.container
     [:div.page-header
      [:h2 "Project list"]]

     [:div#iProjectList.panel-group "Loading..."]]

    [:div#iModal.modal.fade
     [:div#iModalDialog.modal-dialog]]

    [:script {:src "/js/jquery-2.1.1.min.js"}]    
    [:script {:src "/js/bootstrap.min.js"}]
    [:script {:src "http://fb.me/react-0.11.1.js"}]
    [:script {:src "/gen/js/hatnik.js"}]]))
