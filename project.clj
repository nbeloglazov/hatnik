(defproject hatnik.web "0.1.0-SNAPSHOT"
  :description "Web app for tracking library releases."
  :url "http://hatnik.com"
  :dependencies [; Clojure
                 [org.clojure/clojure "1.8.0-alpha1"]
                 [ring "1.4.0"]
                 [compojure "1.4.0"]
                 [hiccup "1.0.5"]
                 [ring/ring-json "0.4.0"]
                 [clj-http "2.0.0"]
                 [tentacles "0.4.0"]
                 [com.taoensso/timbre "4.1.4"]
                 [com.novemberain/monger "3.0.0"]
                 [ancient-clj "0.3.11"]
                 [com.draines/postal "1.11.3"]
                 [version-clj "0.1.2"]
                 [clojurewerkz/quartzite "2.0.0"]
                 [prismatic/schema "1.0.1"]
                 [com.stuartsierra/component "0.3.0"]
                 [me.raynes/fs "1.4.6"]
                 [com.googlecode.streamflyer/streamflyer-core "1.1.3"]
                 [commons-io "2.4"]

                 ; ClojureScript
                 [org.clojure/clojurescript "1.7.145"]
                 [jayq "2.5.4"]
                 [org.omcljs/om "1.0.0-alpha24"]]

  :plugins [[lein-cljsbuild "1.1.0"]]

  :main hatnik.system
  :source-paths ["src/clj" "target/gen/clj"]
  :test-paths ["test/clj"]

  :test-selectors {:selenium :selenium
                   :unit (complement :selenium)}

  :clean-targets ^{:protect false} ["resources/public/gen"
                                    "out"]

  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring-mock "0.1.5"]
                        [com.cemerick/piggieback "0.2.1"]
                        [org.seleniumhq.selenium/selenium-java "2.47.1"]
                        [org.seleniumhq.selenium/selenium-remote-driver "2.47.1"]
                        [org.seleniumhq.selenium/selenium-server "2.47.1"]]

         :plugins [[jonase/eastwood "0.2.2"]
                   [com.keminglabs/cljx "0.5.0" :exclusions [org.clojure/clojure]]]

         :cljsbuild
         {:builds
          [{:source-paths ["src/cljs" "dev/cljs" "target/gen/cljs"]
            :compiler
            {:output-to "resources/public/gen/js/hatnik.js"
             :output-dir "out"
             :main hatnik.web.client.app-init
             :optimizations :none
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
        :main hatnik.web.client.app-init
        :externs ["externs/jquery-1.9.js"
                  "externs/hatnik.js"]
        :optimizations :advanced
        :pretty-print false}}]
     }}})
