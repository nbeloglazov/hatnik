(ns hatnik.web.server.handler
  (:require [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [hatnik.web.server.renderer :as renderer]
            [hatnik.web.server.projects :refer [projects-api]]
            [hatnik.web.server.actions :refer [actions-api]]
            [hatnik.web.server.login :refer [login-api]]
            [ring.middleware.json :as json]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.stacktrace :as stacktrace]))

(defroutes app-routes
  (GET "/" [] renderer/core-page)
  (context "/api" []
           (context "/projects" [] projects-api)
           (context "/actions" [] actions-api)
           login-api)
  (route/resources "/")
  (route/not-found "Not Found"))

(defn dump-request [handler]
  (fn [req]
    (clojure.pprint/pprint req)
    (handler req)))

(def app
  (-> #'app-routes
      dump-request
      handler/site
      (json/wrap-json-body {:keywords? true})
      json/wrap-json-response
      stacktrace/wrap-stacktrace))

(comment

   (def server (run-jetty #(app %) {:port 8080 :join? false}))

   (.stop server)
)
