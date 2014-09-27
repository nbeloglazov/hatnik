(ns hatnik.web.server.handler
  (:require clojure.pprint
            [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [taoensso.timbre :as timbre]

            [hatnik.web.server.renderer :as renderer]
            [hatnik.web.server.projects :refer [projects-api]]
            [hatnik.web.server.actions :refer [actions-api]]
            [hatnik.web.server.login :refer [login-api]]
            [hatnik.config :refer [config]]

            [ring.util.response :as resp]
            [ring.middleware.json :as json]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.stacktrace :as stacktrace]))

(timbre/set-level! (:log-level config))

(defn library-version [group artifact]
  (resp/response
   {:result :ok
    :version "1.6.0"}))

(defroutes app-routes
  (GET "/" [] renderer/core-page)
  (context "/api" []
           (context "/projects" [] projects-api)
           (context "/actions" [] actions-api)
           login-api
           (GET "/library-version" [group artifact] (library-version group artifact)))
  (route/resources "/")
  (route/not-found "Not Found"))

(defn dump-request [handler]
  (fn [req]
    (timbre/debug "Request:" req)
    (let [resp (handler req)]
      (timbre/debug "Response:" resp)
      resp)))

(def app
  (-> #'app-routes
      dump-request
      (handler/site {:session {:cookie-attrs {:max-age 3600
                                              :http-only true}}})
      (json/wrap-json-body {:keywords? true})
      json/wrap-json-response
      stacktrace/wrap-stacktrace))

(comment

   (def server (run-jetty #(app %) {:port 8080 :join? false}))

   (.stop server)
)
