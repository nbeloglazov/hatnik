(defproject hatnik.web "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2356"]
                 [compojure "1.1.8"]]

  :plugins [[lein-ring "0.8.11"]
            [lein-cljsbuild "1.0.3"]]

  :ring {:handler hatnik.web.server.handler/app}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring-mock "0.1.5"]]}}

  :source-paths ["src/clj"]

  :cljsbuild 
  {:builds
   [{:source-paths ["src/cljs"]
     :compiler 
     {:output-to "resources/public/gen/js/hatnik.js"
      :optimizations :whitespace
      :pretty-print true}}]   
   })
