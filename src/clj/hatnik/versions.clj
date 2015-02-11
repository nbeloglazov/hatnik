(ns hatnik.versions
  (:require [ancient-clj.core :as anc]
            [version-clj.core :as ver-comp]))

(defn- snapshot? [version]
  (-> version .toLowerCase (.contains "snapshot")))

(defn latest-release-jvm [library-name]
  (->> (anc/versions! library-name)
       (map :version-string)
       (remove snapshot?)
       first))

(defn latest-release [library]
  (case (:type library)
    "jvm" (latest-release-jvm (:name library))))

(defn first-newer? [version-a version-b]
  (= 1 (ver-comp/version-compare version-a version-b)))

(comment
  (latest-release "org.clojure/clojure")
  (latest-release "org.clojure/clojurescript")

  )
