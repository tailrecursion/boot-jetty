(ns tailrecursion.boot-jetty.handler
  (:import
    [java.io                          File InputStream FileInputStream]
    [javax.servlet                    ServletConfig ServletContext ServletContextEvent ServletException]
    [javax.servlet.http               HttpServletRequest HttpServletResponse]
    [org.eclipse.jetty.server         Request Server]
    [org.eclipse.jetty.server.handler AbstractHandler]
    [org.eclipse.jetty.servlet        ServletContextHandler ServletHolder]
    [org.eclipse.jetty.webapp         WebAppContext]
    [org.eclipse.jetty.util.component LifeCycle$Listener] )
  (:require
    [clojure.java.io :as io]
    [clojure.string :as string] ))

;;; private ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def server     (atom nil))
(def serve-fn   (atom nil))
(def destroy-fn (atom nil))

(defn- get-params-map [^ServletConfig config]
  (into {} (for [param (enumeration-seq (.getInitParameterNames config))
    :let [value (.getInitParameter config param)]]
    [(keyword param) value] )))

(defn- get-config-map [^ServletConfig config]
    {:name        (.getServletName config)
     :init-params (get-params-map  config) })

(defn- get-servlet-fn [fn-name]
  (let [[n s] (map symbol ((juxt namespace name) (symbol fn-name)))]
    (require n)
    (or (ns-resolve (the-ns n) s)
      (throw (ServletException.
        (str "The function " fn-name " specified by the web.xml configuration cannot be resolved.") )))))

(defn- get-content-length
  [^HttpServletRequest request]
  (let [length (.getContentLength request)]
    (if (>= length 0) length) ))

(defn- get-client-cert
  [^HttpServletRequest request]
  (first (.getAttribute request "javax.servlet.request.X509Certificate")) )

(defn- get-headers
  [^HttpServletRequest request]
  (reduce
    (fn [headers, ^String name]
      (assoc headers
        (.toLowerCase name)
        (->> (.getHeaders request name)
             (enumeration-seq)
             (string/join ",") )))
    {}
    (enumeration-seq (.getHeaderNames request) )))

(defn- get-request-map
  [^HttpServletRequest request]
  {:server-port        (.getServerPort request)
   :server-name        (.getServerName request)
   :remote-addr        (.getRemoteAddr request)
   :uri                (.getRequestURI request)
   :query-string       (.getQueryString request)
   :scheme             (keyword (.getScheme request))
   :request-method     (keyword (.toLowerCase (.getMethod request)))
   :headers            (get-headers request)
   :content-type       (.getContentType request)
   :content-length     (get-content-length request)
   :character-encoding (.getCharacterEncoding request)
   :ssl-client-cert    (get-client-cert request)
   :body               (.getInputStream request) })

(defn- set-status
  [^HttpServletResponse response, status]
  (.setStatus response status))

(defn- set-headers
  [^HttpServletResponse response, headers]
  (doseq [[key val-or-vals] headers]
    (if (string? val-or-vals)
      (.setHeader response key val-or-vals)
      (doseq [val val-or-vals]
        (.addHeader response key val) )))
  (when-let [content-type (get headers "Content-Type")]
    (.setContentType response content-type) ))

(defn- set-body
  [^HttpServletResponse response, body]
  (cond
    (string? body)
      (with-open [writer (.getWriter response)]
        (.print writer body))
    (seq? body)
      (with-open [writer (.getWriter response)]
        (doseq [chunk body]
          (.print writer (str chunk))))
    (instance? InputStream body)
      (with-open [^InputStream b body]
        (io/copy b (.getOutputStream response) ))
    (instance? File body)
      (let [^File f body]
        (with-open [stream (FileInputStream. f)]
          (set-body response stream) ))
    (nil? body)
      nil
    :else
      (throw (Exception. ^String (format "Unrecognized body: %s" body)) )))

(defn- set-response-map
  {:arglists '([response response-map])}
  [^HttpServletResponse response, {:keys [status headers body]}]
  (when-not response
    (throw (Exception. "Null response given.")) )
  (when status
    (set-status response status) )
  (doto response
    (set-headers headers)
    (set-body body) ))

;;; handler implementation ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def server (atom nil))

(deftype Handler [_]
  org.eclipse.jetty.server.Handler
  (isFailed [_]
    false )
  (isRunning [_]
    true )
  (isStarted [_]
    true )
  (isStarting [_]
    true )
  (isStopped [_]
    true )
  (isStopping [_]
    true )
  (addLifeCycleListener [_ listener]
    (prn :add-called) )
  (removeLifeCycleListener [_ listener]
    (prn :rem-called) )
  (start [_]
    nil )
  (stop [_]
    nil )
  (destroy [_]
    nil )
  (^void handle [_ ^String target ^Request base-request ^HttpServletRequest request ^HttpServletResponse response]
    (if-let [req (get-request-map request)]
      (set-response-map response {:status 200 :body "hello world"}) ))
  (^Server getServer [_]
    @server )
  (^void setServer [_ ^Server server]
    nil ))

(defn create [path]
  (Handler. path) )

(defn refresh [handler] nil)
