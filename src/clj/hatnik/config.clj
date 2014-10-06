(ns hatnik.config
  (:require [taoensso.timbre :as timbre]))

(def default-config
  {:log-level :info
   :db :memory
   :worker-server {:host "localhost"
                   :port 5734}

    :web {:port 8080}

   :enable-actions true

   :quartz {:initial-delay-in-seconds 60
            :interval-in-seconds (* 60 10)}})

(def config-file "config.clj")

(defn get-config []
  (merge
   default-config
   (try (read-string (slurp config-file))
        (catch Exception e
          (timbre/error "Error while parsing config")
          {}))))
