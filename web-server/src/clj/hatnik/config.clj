(ns hatnik.config)

(def config-file "config.clj")

(def config
  (try (read-string (slurp config-file))
       (catch Exception e {})))
