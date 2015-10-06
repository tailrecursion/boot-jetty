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

(defn mkcnf [path]
  (let [body #(first (:content (some (fn [$] (if (= (:tag $) %2) $)) %)))]
    (->> (or path "WEB-INF/web.xml")
         (io/resource)
         (io/input-stream)
         (xml/parse)
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
  (refresh [this pod cnf]) )

(deftype PodHandler [pod cnf]
  Handler
  (isFailed                [_] false)
  (isRunning               [_] false)
  (isStarted               [_] false)
  (isStarting              [_] false)
  (isStopped               [_] false)
  (isStopping              [_] false)
  (addLifeCycleListener    [_ _] nil)
  (removeLifeCycleListener [_ _] nil)
  (destroy                 [_]   nil)
  (getServer               [_]   nil)
  (setServer               [_ _] nil)
  (start                   [_]  (pod/with-call-in @pod (create ~(deref cnf))))
  (stop                    [_]  (pod/with-call-in @pod (destroy)))
  (^void handle            [_ ^String _ ^Request _ ^HttpServletRequest jxreq ^HttpServletResponse jxres]
    (let [serve #(read-string (.invoke @pod "serve" (pr-str (dissoc % :body)) (:body %)))]
      (ring/update-servlet-response jxres (serve (ring/build-request-map jxreq)))) )
  Refreshable
  (refresh [_ p c]
    (reset! cnf c)
    (reset! pod p) ))

;;; impl ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn initialize! [pod port config-path]
  (let [handler #(PodHandler. (atom pod) (atom (mkcnf config-path)))
        refresh #(doto (.getHandler @server)
                       (.stop)
                       (.refresh pod (mkcnf config-path))
                       (.start))
        startup #(doto (Server. port)
                       (.setHandler (handler))
                       (.start) )]
    (if @server (refresh) (reset! server (startup))) ))

(defn terminate! []
  (.stop @server) )
