(ns tailrecursion.boot-jetty.impl
  (:import
    [org.eclipse.jetty.server  Server]
    [org.eclipse.jetty.servlet ServletContextHandler ServletHolder]
    [org.eclipse.jetty.webapp  WebAppContext] ))

(defn serve [webapp port init-params]
  (let [ctx (doto (WebAppContext.) (.setContextPath "/") (.setResourceBase webapp))]
    (doseq [[k v] init-params] (.setInitParameter ctx k v))
    (doto (Server. port) (.setHandler ctx) (.start))))
