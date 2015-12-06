(ns tailrecursion.boot-jetty
  {:boot/export-tasks true}
  (:require
   [boot.pod  :as pod]
   [boot.core :as boot]
   [boot.util :as util] ))

(def ^:private deps
  (delay (remove pod/dependency-loaded?
   '[[org.eclipse.jetty/jetty-servlet "9.3.1.v20150714"]
     [org.eclipse.jetty/jetty-server  "9.3.1.v20150714"]
     [org.eclipse.jetty/jetty-webapp  "9.3.1.v20150714"] ])))

(boot/deftask serve
  "Serve the application, reloading all namspaces with each subsequent invocation."
  [p port PORT int "The port the server will bind to."]
  (let [pod-env (-> (boot/get-env) (dissoc :source-paths) (update :dependencies into (vec (seq @deps))))
        pod     (future (pod/make-pod pod-env))
        webapp  (boot/tmp-dir!)
        serve   (delay (pod/with-call-in @pod (tailrecursion.boot-jetty.impl/serve ~(.getPath webapp) ~port))) ]
    (util/info "Starting Jetty on port %s...\n" port)
    (boot/with-pre-wrap fileset
      (apply boot/sync! webapp (boot/output-dirs fileset))
      @serve
     fileset )))
