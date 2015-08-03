(ns tailrecursion.boot-jetty.handler
  (:import
    [java.io                          StringReader File InputStream FileInputStream]
    [javax.servlet                    ServletConfig ServletContext ServletContextEvent ServletException]
    [javax.servlet.http               HttpServletRequest HttpServletResponse]
    [org.eclipse.jetty.server         Request Server]
    [org.eclipse.jetty.server.handler AbstractHandler]
    [org.eclipse.jetty.servlet        ServletContextHandler ServletHolder]
    [org.eclipse.jetty.webapp         WebAppContext]
    [org.eclipse.jetty.util.component LifeCycle$Listener] )
  (:require
    [clojure.data.xml :as xml]
    [clojure.java.io  :as io]
    [clojure.string   :as str] ))

;;; private ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def server     (atom nil))
(def serve-fn   (atom nil))
(def destroy-fn (atom nil))

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

;;; handler implementation ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftype Handler [_]
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
          ((get-servlet-fn n) config) )
        (if-let [n  (:serve config)]
          (if-let [f (get-servlet-fn n)]
            (reset! serve-fn f) )
          (throw (ServletException. "The required serve function could not be found in web.xml.")) )
        (if-let [n (:destroy config)]
          (reset! destroy-fn (get-servlet-fn n)))  )))
  (stop [_]
    (prn :stop)
    nil )
  (destroy [_]
    nil )
  (^void handle [_ ^String target ^Request base-request ^HttpServletRequest request ^HttpServletResponse response]
    (if-let [req (get-request-map request)]
      (set-response-map response (@serve-fn req)) ))
  (^Server getServer [_]
    @server )
  (^void setServer [_ ^Server server]
    nil ))

(defn create [path]
  (Handler. path) )

(defn refresh [handler] nil)
