(ns hatnik.web.server.build-files
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [hatnik.versions :as ver]
            [hatnik.schema :as schema]
            [schema.core :as s])
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


(defn build-file->url
  "Converts build file to URL if it is correct. If not, return nil."
  [build-file]
  (cond (nil? (s/check schema/GithubRepository build-file))
        (str "https://raw.githubusercontent.com/" build-file "/master/project.clj")

        (re-matches #"^http(s)?://.*" build-file)
        build-file

        :default
        nil))

(defn actions-from-build-file
  "Given build-file url builds list of actions for each library used in
  build file."
  [build-file]
  (if-let [url (build-file->url build-file)]
    (try
      (let [deps (all-dependencies (read-project-clj url))]
        (->> deps
             (map (fn [library]
                    {:library library
                     :last-processed-version (ver/latest-release library)
                     :type "build-file"}))
             (filter :last-processed-version)
             doall))
      (catch Exception e
        []))
    []))

(comment


  (actions-from-build-file "nbeloglazov/hatnik")
  
  )
