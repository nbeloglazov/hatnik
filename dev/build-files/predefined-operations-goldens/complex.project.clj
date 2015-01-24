(defproject complex "1.0.0"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [quil    		  "4.5.6"    :classifier "jdk15"]
                 [net.sf.ehcache/ehcache "2.3.1" :extension "pom"]
                 [log4j "1.2.15" :exclusions [[javax.mail/mail :extension "jar"]
                                              [javax.jms/jms :classifier "*"]
                                              com.sun.jdmk/jmxtools
                                              com.sun.jmx/jmxri]]
                 [org.lwjgl.lwjgl/lwjgl "2.8.5"]
                 [org.lwjgl.lwjgl/lwjgl-platform "2.8.5"
                  :classifier "natives-osx"
                  ;; LWJGL stores natives in the root of the jar; this
                  ;; :native-prefix will extract them.
                  :native-prefix ""]]

  :plugins [[lein-pprint "1.1.1"]
            [lein-assoc "0.1.0" :middleware false]
            [s3-wagon-private "1.1.1" :hooks false]]

  :profiles {:debug {:debug true
                     :injections [(prn (into {} (System/getProperties)))]}
             :1.4 {:dependencies [[org.clojure/clojure "1.4.0"]]}
             :1.5 {:dependencies [[org.clojure/clojure "1.5.0"]]}
             ;; activated by default
             :dev {:resource-paths ["dummy-data"]
                   :dependencies [[clj-stacktrace "0.2.4"]]}
             ;; activated automatically during uberjar
             :uberjar {:aot :all}
             ;; activated automatically in repl task
             :repl {:plugins [[cider/cider-nrepl "0.7.1"]]}})
