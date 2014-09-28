(ns hatnik.versions
  (:require [ancient-clj.core :as anc]
            [version-clj.core :as ver-comp]))

(defn- snapshot? [[full parts]]
  (-> full .toLowerCase (.contains "snapshot")))

(defn latest-release [library]
  (->> (anc/versions! library)
       (remove snapshot?)
       last
       first))

(defn first-newer? [version-a version-b]
  (= 1 (ver-comp/version-compare version-a version-b)))

(comment
  (latest-release "org.clojure/clojure")
  (latest-release "org.clojure/clojurescript")

  )
