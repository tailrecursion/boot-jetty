(ns tailrecursion.boot-jetty
  {:boot/export-tasks true}
  (:require
    [boot.pod  :as pod]
    [boot.core :as boot]
    [boot.util :as util] ))

(def ^:private srv-deps
  (delay (remove pod/dependency-loaded?
   '[[org.clojure/data.xml           "0.0.8"]
     [org.eclipse.jetty/jetty-server "9.3.4.RC0"]
     [ring/ring-servlet              "1.4.0"] ])))

(defn shim [pod]
  (pod/with-eval-in pod
    (let [conf (atom nil)]
      (defn create [config]
        (reset! conf config)
        (if-let [create-fn (:create @conf)]
          (boot.pod/eval-fn-call [create-fn config]) ))
      (defn serve [map stm]
        (if-let [serve-fn (:serve @conf)]
          (pr-str (boot.pod/eval-fn-call [serve-fn (assoc (read-string map) :body stm)]))
          (throw (Exception. "The required serve function could not be found in web.xml.")) ))
      (defn destroy []
        (when-let [destroy-fn (:destroy @conf)]
          (boot.pod/eval-fn-call [destroy-fn]) )))))

(boot/deftask serve
  "Serve the application, refreshing the application with each subsequent invocation."
  [p port PORT int "The port the server will bind to."
   c conf PATH str "The path to the web.xml file" ]
  (let [app-dir (boot/tmp-dir!)
        message #(util/info "%s Jetty on port %s...\n" % port)
        rmpaths #(dissoc % :asset-paths :source-paths :resource-paths :target-path)
        app-env (-> (boot/get-env) (rmpaths) (assoc :resource-paths #{(.getPath app-dir)}))
        app-pod (delay (pod/pod-pool app-env :init shim))
        srv-env (-> (boot/get-env) (update :dependencies into (vec (seq @srv-deps))))
        srv-pod (delay (pod/make-pod srv-env)) ]
    (boot/cleanup
      (message "\nStopping")
      (pod/with-call-in @srv-pod
        (tailrecursion.boot-jetty.impl/terminate!) ))
    (boot/with-pre-wrap fileset
      (message (if (realized? srv-pod) "Refreshing" "Starting"))
      (apply boot/sync! app-dir (boot/output-dirs fileset))
      (.invoke @srv-pod
        "tailrecursion.boot-jetty.impl/initialize!" (@app-pod :refresh) port conf)
      fileset )))
