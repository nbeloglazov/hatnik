(ns hatnik.config)

(def default-config
  {:log-level :info})

(def config-file "config.clj")

(def config
  (try (read-string (slurp config-file))
       (catch Exception e default-config)))
