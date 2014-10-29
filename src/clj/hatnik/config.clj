(ns hatnik.config)

(defn get-config []
  (merge
   (read-string (slurp "config.default.clj"))
   (try (read-string (slurp "config.clj"))
        (catch Exception e
          (println "Error in config.clj")
          (.printStackTrace e)
          {}))))
