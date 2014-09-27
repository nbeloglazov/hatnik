(ns hatnik.versions
  (:require [ancient-clj.core :as anc]))

(defn- snapshot? [[full parts]]
  (-> full .toLowerCase (.contains "snapshot")))

(defn latest-release [library]
  (->> (anc/versions! library)
       (remove snapshot?)
       last
       first))

(comment
  (latest-release "org.clojure/clojure")
  (latest-release "org.clojure/clojurescript"))
