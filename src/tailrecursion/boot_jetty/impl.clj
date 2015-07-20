(ns tailrecursion.boot-jetty.impl
  (:import
    [org.eclipse.jetty.server  Server]
    [org.eclipse.jetty.servlet ServletContextHandler ServletHolder]
    [org.eclipse.jetty.webapp  WebAppContext] ))

(defn serve [webapp port]
  (let [ctx (doto (WebAppContext.) (.setContextPath "/") (.setResourceBase webapp))]
    (doto (Server. port) (.setHandler ctx) (.start)) ))
