(defproject hatnik.web "0.1.0-SNAPSHOT"
  :description "Web app for tracking library releases."
  :url "http://hatnik.com"
  :dependencies [; Clojure
                 [org.clojure/clojure "1.6.0"]
                 [ring "1.3.1"]
                 [compojure "1.2.0"]
                 [ring/ring-json "0.3.1"]
                 [clj-http "1.0.0"]
                 [tentacles "0.2.5"]
                 [com.taoensso/timbre "3.3.1"]
                 [com.novemberain/monger "2.0.0"]
                 [ancient-clj "0.2.0"]
                 [com.draines/postal "1.11.1"]
                 [version-clj "0.1.0"]
                 [clojurewerkz/quartzite "1.3.0"]
                 [prismatic/schema "0.3.1"]
                 [com.stuartsierra/component "0.2.2"]

                 ; ClojureScript
                 [org.clojure/clojurescript "0.0-2356"]
                 [jayq "2.5.2"]
                 [om "0.7.3"]]

  :plugins [[lein-ring "0.8.11"]
            [lein-cljsbuild "1.0.3"]]

  :main hatnik.system
  :source-paths ["src/clj"]
  :test-paths ["test/clj"]

  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring-mock "0.1.5"]
                        [com.cemerick/piggieback "0.1.3"]]

         :plugins [[jonase/eastwood "0.1.4"]]

         :cljsbuild
         {:builds
          [{:source-paths ["src/cljs" "dev/cljs"]
            :compiler
            {:output-to "resources/public/gen/js/hatnik.js"
             :optimizations :simple
             :pretty-print true}}]
          }}
   :release
   {:cljsbuild
    {:builds
     [{:source-paths ["src/cljs"]
       :compiler
       {:output-to "resources/public/gen/js/hatnik.js"
        :optimizations :advanced
        :externs ["externs/jquery-1.9.js"
                  "externs/hatnik.js"
                  "react/externs/react.js"]
        :pretty-print false}}]
     }}})
