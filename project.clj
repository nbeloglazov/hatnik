(defproject hatnik.web "0.1.0-SNAPSHOT"
  :description "Web app for tracking library releases."
  :url "http://hatnik.com"
  :dependencies [; Clojure
                 [org.clojure/clojure "1.7.0-alpha4"]
                 [ring "1.3.2"]
                 [compojure "1.2.1"]
                 [hiccup "1.0.5"]
                 [ring/ring-json "0.3.1"]
                 [clj-http "1.0.1"]
                 [tentacles "0.2.5"]
                 [com.taoensso/timbre "3.3.1"]
                 [com.novemberain/monger "2.0.0"]
                 [ancient-clj "0.2.1"]
                 [com.draines/postal "1.11.3"]
                 [version-clj "0.1.0"]
                 [clojurewerkz/quartzite "1.3.0"]
                 [prismatic/schema "0.3.3"]
                 [com.stuartsierra/component "0.2.2"]
                 [me.raynes/fs "1.4.5"]
                 [com.googlecode.streamflyer/streamflyer-core "1.1.2"]
                 [commons-io "2.4"]

                 ; ClojureScript
                 [org.clojure/clojurescript "0.0-2356"]
                 [jayq "2.5.2"]
                 [om "0.7.3"]]

  :plugins [[lein-cljsbuild "1.0.3"]]

  :main hatnik.system
  :source-paths ["src/clj" "target/gen/clj"]
  :test-paths ["test/clj"]

  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring-mock "0.1.5"]
                        [com.cemerick/piggieback "0.1.3"]]

         :plugins [[jonase/eastwood "0.1.4"]
                   [com.keminglabs/cljx "0.4.0"]]

         :cljsbuild
         {:builds
          [{:source-paths ["src/cljs" "dev/cljs" "target/gen/cljs"]
            :compiler
            {:output-to "resources/public/gen/js/hatnik.js"
             :optimizations :simple
             :pretty-print true}}]
          }
         :cljx {:builds [{:source-paths ["src/cljx"]
                 :output-path "target/gen/clj"
                 :rules :clj}

                {:source-paths ["src/cljx"]
                 :output-path "target/gen/cljs"
                 :rules :cljs}]}}
   :release
   {:cljsbuild
    {:builds
     [{:source-paths ["src/cljs" "target/gen/cljs"]
       :compiler
       {:output-to "resources/public/gen/js/hatnik.js"
        :optimizations :advanced
        :externs ["externs/jquery-1.9.js"
                  "externs/hatnik.js"
                  "react/externs/react.js"]
        :pretty-print false}}]
     }}})
