(ns hatnik.web.server.handler
  (:require clojure.pprint
            [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [hatnik.web.server.example-data :as ex-data]
            [hatnik.web.server.renderer :as renderer]
            [ring.util.response :as resp]
            [ring.middleware.json :as json]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.stacktrace :as stacktrace]))

(defroutes app-routes
  (GET "/" [] renderer/core-page)
  (GET "/projects" [] (resp/response ex-data/project-response))
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
