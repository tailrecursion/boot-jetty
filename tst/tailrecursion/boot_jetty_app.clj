(ns tailrecursion.boot-jetty-app
  (:require [boot.pod :as pod]))

(defn serve [req]
  (println :serve (pod/with-call-in req (println "beer")))
  {:status  200
   :headers {"content-type" "text/plain"}
   :body    "Boot Jetty" })
