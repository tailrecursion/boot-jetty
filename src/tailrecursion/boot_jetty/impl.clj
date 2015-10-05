(ns tailrecursion.boot-jetty.impl
  (:import
    [javax.servlet.http       HttpServletRequest HttpServletResponse]
    [org.eclipse.jetty.server Handler Request Server] )
  (:require
    [boot.pod          :as pod]
    [clojure.data.xml  :as xml]
    [clojure.java.io   :as io]
    [ring.util.servlet :as ring] ))

;;; private ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def server (atom nil))

(defn mkconfig [web]
  (let [body #(first (:content (some (fn [$] (if (= (:tag $) %2) $)) %)))]
    (->> (xml/parse web)
      (:content)
      (filter #(= (:tag %) :servlet))
      (map :content)
      (some #(if (= (body % :servlet-name) "boot-webapp") %))
      (filter #(= (:tag %) :init-param))
      (map :content)
      (map #(vector (keyword (body % :param-name)) (symbol (body % :param-value))))
      (into {}) )))

;;; handler ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defprotocol Refreshable
  (refresh [this pod]) )

(deftype PodHandler [pod conf]
  Handler
  (isFailed                [_]
    false )
  (isRunning               [_]
    false )
  (isStarted               [_]
    false )
  (isStarting              [_]
    false )
  (isStopped               [_]
    false )
  (isStopping              [_]
    false )
  (addLifeCycleListener    [_ _]
    (prn :add-called) )
  (removeLifeCycleListener [_ _]
    (prn :rem-called) )
  (start                   [_]
    (pod/with-call-in @pod (create ~conf)) )
  (stop                    [_]
    (pod/with-call-in @pod (destroy)) )
  (destroy                 [_]
    nil )
  (^void handle [_ ^String _ ^Request _ ^HttpServletRequest request ^HttpServletResponse response]
    (if-let [req (ring/build-request-map request)]
      (ring/update-servlet-response response
        (read-string (.invoke @pod "serve" (pr-str (dissoc req :body)) (:body req))) )))
  (getServer [_]
    @server )
  (setServer [_ srv]
    (reset! server srv) )
  Refreshable
  (refresh [_ p]
    (reset! pod p) ))

(defn serve [pod port conf-path]
  (let [config  (mkconfig (io/input-stream (io/resource (or conf-path "WEB-INF/web.xml"))))
        handler (PodHandler. (atom pod) config) ]
    (doto (Server. port)
          (.setHandler handler)
          (.start) )))

;;; server ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn initialize! [pod port conf-path]
  (if @server
    (doto (.getHandler @server) (.refresh pod) (.start)) ;; todo: remove
    (reset! server (serve pod port conf-path)) ))

(defn terminate! []
  (.stop @server) )
