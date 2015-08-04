(ns tailrecursion.boot-jetty.server
  (:import
    [java.io                          StringReader File InputStream FileInputStream]
    [javax.servlet                    ServletConfig ServletContext ServletContextEvent ServletException]
    [javax.servlet.http               HttpServletRequest HttpServletResponse]
    [org.eclipse.jetty.server         Request Server]
    [org.eclipse.jetty.server.handler AbstractHandler]
    [org.eclipse.jetty.util.component LifeCycle$Listener] )
  (:require
    [boot.core        :as boot]
    [boot.pod         :as pod]
    [clojure.data.xml :as xml]
    [clojure.java.io  :as io]
    [clojure.string   :as str] ))

;;; private ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def server     (atom nil))
(def serve-fn   (atom nil))
(def destroy-fn (atom nil))

;;; web.xml ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- get-config-map [web-resource]
  (let [body #(first (:content (some (fn [$] (if (= (:tag $) %2) $)) %)))]
    (with-open [web (io/input-stream web-resource)]
      (->> (xml/parse web)
        (:content)
        (filter #(= (:tag %) :servlet))
        (map :content)
        (some #(if (= (body % :servlet-name) "boot-webapp") %))
        (filter #(= (:tag %) :init-param))
        (map :content)
        (map #(vector (keyword (body % :param-name)) (symbol (body % :param-value))))
        (into {}) ))))

;;; request & response maps ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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
             (str/join ",") )))
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

;;; handler ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defprotocol IRefreshable
  (refresh [this]) )

(deftype Handler [pod-svc path]
  org.eclipse.jetty.server.Handler
  (isFailed [_]
    false )
  (isRunning [_]
    false )
  (isStarted [_]
    false )
  (isStarting [_]
    false )
  (isStopped [_]
    false )
  (isStopping [_]
    false )
  (addLifeCycleListener [_ listener]
    (prn :add-called) )
  (removeLifeCycleListener [_ listener]
    (prn :rem-called) )
  (start [_]
    (when-let [web-rsc (io/resource "WEB-INF/web.xml")]
      (let [config (get-config-map web-rsc) ]
        (if-let [n (:create config)]
          (pod/call-in* (pod-svc)
            `(~(symbol n) ~config)) )
        (if-let [f  (:serve config)]
          (if-let [n (symbol f)]
            (reset! serve-fn n) )
          (throw (ServletException. "The required serve function could not be found in web.xml.")) )
        (if-let [n (:destroy config)]
          (reset! destroy-fn (symbol n)))  )))
  (stop [_]
    (if-let [destroy @destroy-fn]
      (pod/call-in* (pod-svc) `(~destroy)) ))
  (destroy [_]
    nil )
  (^void handle [_ ^String target ^Request base-request ^HttpServletRequest request ^HttpServletResponse response]
    (when-let [req (dissoc (get-request-map request) :body)]
      (set-response-map response (pod/call-in* (pod-svc) `(~(deref serve-fn) ~req)) )))
  (getServer [_]
    @server )
  (setServer [_ srv]
    (reset! server srv) )
  IRefreshable
  (refresh [_]
    (pod-svc :refresh) ))

;;; server ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn create! [env path port]
  (let [pod-env (-> env (assoc :resource-paths #{path}))
        pod-svc (pod/pod-pool pod-env)
        handler (Handler. pod-svc path) ]
    (reset! server (doto (Server. port) (.setHandler handler) (.start)) )))

(defn refresh! []
  (.refresh (.getHandler @server)) )

(defn destroy! []
  (.stop @server) )
