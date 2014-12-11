(defproject hatnik/tools.backup "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [com.google.apis/google-api-services-storage "v1-rev22-1.19.0"]
                 [com.google.http-client/google-http-client-jackson2 "1.19.0"]
                 [com.google.oauth-client/google-oauth-client-jetty "1.19.0"]]

  :main hatnik.tools.backup
  :aot :all)
