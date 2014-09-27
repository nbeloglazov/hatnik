(ns hatnik.config)

(def default-config
  {:log-level :info
   :db :memory})

(def config-file "config.clj")

(def config
  (merge
   default-config
   (try (read-string (slurp config-file))
        (catch Exception e {}))))
