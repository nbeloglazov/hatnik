(defproject hatnik/tools.backup "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [com.google.apis/google-api-services-storage "v1beta2-rev43-1.18.0-rc"]
                 [com.google.http-client/google-http-client-jackson2 "1.17.0-rc"]
                 [com.google.oauth-client/google-oauth-client-jetty "1.17.0-rc"]]

  :main hatnik.tools.backup
  :aot :all)
