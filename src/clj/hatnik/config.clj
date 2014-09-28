(ns hatnik.config
  (:require [taoensso.timbre :as timbre]))

(def default-config
  {:log-level :info
   :db :memory
   :worker-server {:host "localhost"
                   :port 5734}})

(def config-file "config.clj")

(def config
  (merge
   default-config
   (try (read-string (slurp config-file))
        (catch Exception e
          (timbre/error "Error while parsing config")
          {}))))
