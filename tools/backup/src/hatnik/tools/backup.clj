(ns hatnik.tools.backup
  (:require [clojure.java
             [shell :refer [sh with-sh-dir]]
             [io :refer [file]]])
  (:import com.google.api.client.googleapis.auth.oauth2.GoogleCredential$Builder
           com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
           com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
           [com.google.api.client.http HttpTransport FileContent]
           com.google.api.client.json.JsonFactory
           com.google.api.client.json.jackson2.JacksonFactory
           com.google.api.services.storage.Storage$Builder
           com.google.api.services.storage.StorageScopes
           com.google.api.services.storage.model.Bucket
           com.google.api.services.storage.model.StorageObject)
  (:gen-class))


(def email-address "666046803864-iknv2uotd0402vsti9ka45t4n9rvros9@developer.gserviceaccount.com")
(def p12-file (file "private-key.p12"))
(def http-transport (GoogleNetHttpTransport/newTrustedTransport))
(def json-factory (JacksonFactory/getDefaultInstance))

(def bucket-name "mongo-backups")
(def app-name "hatnik-ftw")

(defn authorize []
  (.. (GoogleCredential$Builder.)
      (setTransport http-transport)
      (setJsonFactory json-factory)
      (setServiceAccountId email-address)
      (setServiceAccountScopes [StorageScopes/DEVSTORAGE_FULL_CONTROL
                                StorageScopes/DEVSTORAGE_READ_ONLY
                                StorageScopes/DEVSTORAGE_READ_WRITE])
      (setServiceAccountPrivateKeyFromP12File p12-file)
      (build)))

(defn get-storage []
  (let [credential (authorize)]
    (.. (Storage$Builder. http-transport json-factory credential)
        (setApplicationName app-name)
        (build))))

(defn get-bucket []
  (let [client (get-storage)
        get-bucket (.. client buckets (get bucket-name))
        _ (.setProjection get-bucket "full")
        bucket (.execute get-bucket)]
    (println (.getLocation bucket))
    (println (.getTimeCreated bucket))
    (println (.getOwner bucket))))

(defn get-objects []
  (let [client (get-storage)
        items (.. client
                  (objects)
                  (list bucket-name)
                  (execute)
                  (getItems))]
    (println (type (.. client (objects) (list bucket-name))))
    (doseq [item items]
      (println (.getName item) (.getSize item) item))))

(defn upload-zip-file [name zip-file]
  (println "Archive" name)
  (let [client (get-storage)
        object (doto (StorageObject.)
                 (.setName name)
                 (.setContentType "application/zip"))
        content (FileContent. "application/zip" zip-file)]
     (.. client
         (objects)
         (insert bucket-name object content)
         (execute))))

(defn clean []
  (sh "rm" "-r" "dump")
  (sh "rm" "dump.zip"))

(defn create-dump []
  (clean)
  (sh "mongodump")
  (sh "zip" "-r" "dump.zip" "dump"))

(defn archive-name []
  (-> (java.text.SimpleDateFormat. "yyyy-MM-dd_kk-mm")
      (.format (java.util.Date.))
      (str ".zip")))

(defn create-and-upload-dump []
  (with-sh-dir "/tmp"
    (println "Creating dump")
    (create-dump)
    (println "Uploading dump")
    (upload-zip-file (archive-name) (file "/tmp/dump.zip"))
    (println "Cleaning")
    (clean)
    (println "Finished")))

(defn -main [& args]
  (create-and-upload-dump))


(comment

  (get-objects)

  )
