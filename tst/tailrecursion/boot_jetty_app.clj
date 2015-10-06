(ns tailrecursion.boot-jetty-app
  (:require [boot.pod :as pod]))

(defn serve [req]
  {:status  200
   :headers {"content-type" "text/plain"}
   :body    "Boot Jetty" })
