(ns hatnik.web.server.handler
  (:require [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [hiccup.core :as hc]
            [clojure.data.json :as json]))

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
    [:script {:src "/gen/js/hatnik.js"}]]))

(defn get-projects [req]
  [{:name "foo"
    :actions [1 2 3 4 5]}
   {:name "baz"
    :actions ["a" 123 23521 2345]}])


(defroutes app-routes
  (GET "/" [] core-page)
  (GET "/projects" req (json/write-str (get-projects req)))
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (handler/site app-routes))
