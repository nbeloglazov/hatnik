(ns hatnik.worker.github-pull-request-action-test
  (:require [me.raynes.fs :as fs]
            [clojure.test :refer :all]
            [hatnik.worker.github-pull-request-action :refer :all]
            [clojure.java.io :as io]))

(def initial-state
  {"file1.txt" (str "library-1 0.1.2\n"
                    "library-2 2.3.4")
   "some/dir/hello.txt" "Using library-3 of version 5.5-alpha!"})

(defn set-state [dir state]
  (fs/delete-dir dir)
  (doseq [[file content] state
          :let [file (io/file dir file)]]
    (fs/mkdirs (.getParentFile file))
    (spit file content)))

(defn get-state [dir]
  (->> (file-seq dir)
       (filter #(.isFile %))
       (map #(let [path (.getAbsolutePath %)]
               [(subs path (inc (count (.getAbsolutePath dir))))
                (slurp %)]))
       (into {})))

(deftest update-files-test
  (let [dir (fs/temp-dir "hatnik-github-pull-request-test")
        temp-file (fs/temp-file "hatnik-temp-file")]
    (try
      (set-state dir initial-state)
      (spit temp-file "inaccessible")

      ; Update library-1 to version 2.2.2
      (let [variables {:library "library-1"
                       :version "2.2.2"
                       :previous-version "0.1.2"}
            operations [{:file "file1.txt"
                         :regex "{{library}} [0-9.\\w]+"
                         :replacement "{{library}} {{version}}"}
                        {:file "some/dir/hello.txt"
                         :regex "{{library}} something"
                         :replacement "helllo"}]]
        (assert (= [:updated :unmodified]
               (update-files {:operations operations}
                             variables
                             dir)))
        (assert (= {"file1.txt" (str "library-1 2.2.2\n"
                                 "library-2 2.3.4")
                "some/dir/hello.txt" "Using library-3 of version 5.5-alpha!"}
               (get-state dir))))

      ; Update library-2 to version 1.0.0
      (let [variables {:library "library-2"
                       :version "3.0.0"
                       :previous-version "2.3.4"}
            operations [{:file "file1.txt"
                         :regex "({{library}}) [0-9.\\w]+"
                         :replacement "$1 {{version}}"}]]
        (assert (= [:updated]
               (update-files {:operations operations}
                             variables
                             dir)))
        (assert (= {"file1.txt" (str "library-1 2.2.2\n"
                                 "library-2 3.0.0")
                "some/dir/hello.txt" "Using library-3 of version 5.5-alpha!"}
               (get-state dir))))

      ; Update library-3 to version 6.0
      (let [variables {:library "library-3"
                       :version "6.0"
                       :previous-version "5.5-alpha"}
            operations [{:file "some/dir/hello.txt"
                         :regex "({{library}} of version) [^!]+"
                         :replacement "$1 {{version}}"}]]
        (assert (= [:updated]
               (update-files {:operations operations}
                             variables
                             dir)))
        (assert (= {"file1.txt" (str "library-1 2.2.2\n"
                                 "library-2 3.0.0")
                "some/dir/hello.txt" "Using library-3 of version 6.0!"}
               (get-state dir))))

      ; Try updating not existing files or files outside repo.
      (let [variables {:library "library-3"
                       :version "6.0"
                       :previous-version "5.5-alpha"}
            operations [{:file (str "../" (.getName temp-file))
                         :regex "inaccessible"
                         :replacement "hacked"}
                        {:file "randomfile.txt"
                         :regex ".*"
                         :replacement "hmm"}]]
        (assert (= [:file-not-found :file-not-found]
               (update-files {:operations operations}
                             variables
                             dir)))
        (assert (= {"file1.txt" (str "library-1 2.2.2\n"
                                 "library-2 3.0.0")
                "some/dir/hello.txt" "Using library-3 of version 6.0!"}
               (get-state dir)))
        (assert (= "inaccessible" (slurp temp-file))))
      (finally
        (fs/delete-dir dir)
        (fs/delete temp-file)))))

(defn run-predefined-operation-test [file operation-type variables]
  (let [dir (fs/temp-dir "hatnik-github-pull-request-test")
        original-file (fs/file "dev/build-files/" file)
        modified-file (fs/file dir operation-type)
        golden-file (fs/file "dev/build-files/predefined-operations-goldens"
                             file)]
    (try
      (fs/copy original-file modified-file)
      (let [operations (predefined-operations-mapping operation-type)
            results (update-files {:operations operations}
                                  variables
                                  dir)]
        (is (= results [:updated]))
        (is (org.apache.commons.io.FileUtils/contentEquals
             modified-file golden-file)))
      (finally
        (fs/delete-dir dir)))))


(deftest predefined-operation-project-clj-test
  (run-predefined-operation-test "complex.project.clj" "project.clj"
                                 {:library "quil"
                                  :version "4.5.6"}))


(comment
  (predefined-operation-project-clj-test)

  )


