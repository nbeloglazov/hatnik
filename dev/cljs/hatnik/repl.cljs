(ns hatnik.repl
  (:require [clojure.browser.repl :as repl]))

(repl/connect "http://localhost:9000/repl")

; for CIDER
(comment
  ; first - run cider-jack-in
  ; Note: You must put [cider/cider-nrepl "0.8.0-SNAPSHOT"] to your lein profile first

  ; second - run inside the repl

  (require 'cljs.repl.browser)
  (cemerick.piggieback/cljs-repl
   :repl-env (cljs.repl.browser/repl-env :port 9000))

  ; third - open site in the your browser and start working
  )
