(ns tailrecursion.boot-jetty.server
  (:require
    [tailrecursion.boot-jetty.handler :as webapp] )
  (:import
    [org.eclipse.jetty.server Server] ))

(def server (atom nil))

(defn create! [path port]
  (let [webapp (webapp/create path)]
    (reset! server (doto (Server. port) (.setHandler webapp) (.start)) )))

(defn refresh! []
  (webapp/refresh (.getHandler @server)) )

(defn destroy! []
  (.stop @server) )
