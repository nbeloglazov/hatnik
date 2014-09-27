(ns hatnik.web.server.handler
  (:require [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [hiccup.core :as hc]
            [clojure.data.json :as json]
            [hatnik.web.server.example-data :as ex-data]))

(defn page-html-head []
  (hc/html
   [:head
    [:title "Hatnik"]
    [:link {:rel "stylesheet" :href "/css/bootstrap.min.css"}]]))


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

    [:script {:src "/js/jquery-2.1.1.min.js"}]    
    [:script {:src "/js/bootstrap.min.js"}]
    [:script {:src "http://fb.me/react-0.11.1.js"}]
    [:script {:src "/gen/js/hatnik.js"}]]))


(defroutes app-routes
  (GET "/" [] core-page)
  (GET "/projects" req (json/write-str ex-data/project-response))
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (handler/site app-routes))
