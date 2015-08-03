(ns tailrecursion.boot-jetty
  {:boot/export-tasks true}
  (:require
    [boot.pod  :as pod]
    [boot.core :as boot]
    [boot.util :as util] ))

(def ^:private deps
  (delay (remove pod/dependency-loaded?
   '[[org.clojure/data.xml            "0.0.8"]
     [org.eclipse.jetty/jetty-servlet "9.3.1.v20150714"]
     [org.eclipse.jetty/jetty-server  "9.3.1.v20150714"]
     [org.eclipse.jetty/jetty-webapp  "9.3.1.v20150714"] ])))

(boot/deftask serve
  "Serve the application, reloading all namspaces with each subsequent invocation."
  [p port PORT int "The port the server will bind to."]
  (let [pod-env (-> (boot/get-env) (dissoc :source-paths) (update :dependencies into (vec (seq @deps))))
        pod     (future (pod/make-pod pod-env))
        webapp  (boot/tmp-dir!)
        create  (delay (pod/with-call-in @pod (tailrecursion.boot-jetty.server/create! ~(.getPath webapp) ~port)))
        inform #(util/info (str % " Jetty on port %s...\n") port) ]
    (boot/cleanup
      (inform "\nStopping")
      (pod/with-call-in @pod (tailrecursion.boot-jetty.server/destroy!)) )
    (boot/with-pre-wrap fileset
      (if-not (realized? create)
        (do (inform "Starting")
            (deref create) )
        (do (inform "Restarting")
            (pod/with-call-in @pod (tailrecursion.boot-jetty.server/refresh!) )))
      fileset )))
