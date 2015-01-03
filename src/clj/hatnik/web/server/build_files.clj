(ns hatnik.web.server.build-files
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [hatnik.versions :as ver])
  (:import java.io.PushbackReader))

; Code shamelessly based on rodnaph/clj-deps and hashobject/jarkeeper.com

(defn read-project-clj
  "Reads project.clj file and return map containing project.clj options."
  [url]
  (->> url io/reader PushbackReader. edn/read (drop 3) (apply hash-map)))

(defn extract
  "Extract libraries from given project map."
  [project]
  (concat
   (:dependencies project)
   (:dev-dependencies project)
   (:plugins project)))

(defn profile-dependencies
  "Iterates through all profiles from project map and returns their deps."
  [project]
  (mapcat
   (comp extract second)
   (:profiles project)))

(defn all-dependencies
  "Returns all dependencies used in project map. Leaves only unique deps."
  [project]
  (->> (concat
        (extract project)
        (profile-dependencies project))
       (map first)
       (map str)
       distinct))

(defn actions-from-build-file
  "Given build-file url builds list of actions for each library used in
  build file."
  [build-file]
  (let [deps (all-dependencies (read-project-clj build-file))]
    (->> deps
         (map (fn [library]
                {:library library
                 :last-processed-version (ver/latest-release library)
                 :type "build-file"}))
         (filter :last-processed-version)
         doall)))

(comment

  (actions-from-build-file "dev/build-files/project.clj")
  
  )
