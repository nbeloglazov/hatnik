(ns hatnik.web.server.example-data)

(def project-response
  {:result "ok"
   :projects [{:id 1
               :name "Foo"
               :actions [{:id 100
                          :group "org.clojure"
                          :artifact "clojure"
                          :last-processed-version "1.6.0"
                          :type "email"
                          :address "email@example.com"
                          :template "Library release: {{LIBRARY}} {{VERSION}}"
                          :disabled false}]}
              {:id 2
               :name "Baz"
               :actions [{:id 200
                          :group ""
                          :artifact "quil"
                          :last-processed-version "2.2.0"
                          :type "email"
                          :address "email@example.com"
                          :template "Library release: {{LIBRARY}} {{VERSION}}"
                          :disabled true}]}
              ]})
