(defproject hatnik.web "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :dependencies [; Clojure
                 [org.clojure/clojure "1.6.0"]
                 [ring "1.3.1"]
                 [compojure "1.1.8"]
                 [ring/ring-json "0.3.1"]
                 [clj-http "1.0.0"]
                 [tentacles "0.2.5"]
                 [com.taoensso/timbre "3.3.1"]
                 [com.novemberain/monger "2.0.0"]
                 [ancient-clj "0.1.10"]
                 [com.draines/postal "1.11.1"]
                 [version-clj "0.1.0"]
                 [clojurewerkz/quartzite "1.3.0"]

                 ; ClojureScript
                 [org.clojure/clojurescript "0.0-2356"]
                 [jayq "2.5.2"]
                 [om "0.7.3"]]

  :plugins [[lein-ring "0.8.11"]
            [lein-cljsbuild "1.0.3"]]

  :ring {:handler hatnik.web.server.handler/app
         :init hatnik.web.server.handler/initialise}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring-mock "0.1.5"]]}}

  :source-paths ["src/clj"]
  :test-paths ["test/clj"]

  :cljsbuild
  {:builds
   [{:source-paths ["src/cljs"]
     :compiler
     {:output-to "resources/public/gen/js/hatnik.js"
      :optimizations :whitespace
      :pretty-print true}}]
   })
