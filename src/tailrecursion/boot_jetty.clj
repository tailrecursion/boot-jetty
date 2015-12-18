(ns tailrecursion.boot-jetty
  {:boot/export-tasks true}
  (:require
   [boot.pod  :as pod]
   [boot.core :as boot]
   [boot.util :as util] ))

(defn- deps []
  (remove pod/dependency-loaded?
          '[[org.eclipse.jetty/jetty-servlet "9.3.1.v20150714"]
            [org.eclipse.jetty/jetty-server  "9.3.1.v20150714"]
            [org.eclipse.jetty/jetty-webapp  "9.3.1.v20150714"]]))

(defn pod-env []
  (-> (boot/get-env)
      (dissoc :source-paths)
      (update :dependencies into (vec (seq (deps))))))

(boot/deftask serve
  "Serve the application, reloading all namspaces with each subsequent invocation."
  [p port        PORT       int       "The port the server will bind to."
   i init-params NAME=VALUE {str str} "The map of webapp context init parameters."]
  (let [webapp (boot/tmp-dir!)
        pod    (atom nil)
        sync!  #(apply boot/sync! webapp (boot/output-dirs %))
        start  (delay (util/info "Starting Jetty on port %s...\n" port)
                      (reset! pod (pod/make-pod (pod-env)))
                      (pod/with-eval-in @pod
                        (require '[tailrecursion.boot-jetty.impl :as impl])
                        (def server (impl/serve ~(.getPath webapp) ~port ~init-params))))
        stop   (delay (util/info "Stopping Jetty...\n")
                      (util/guard (pod/with-eval-in @pod (.stop server))))]
    (boot/cleanup @stop)
    (boot/with-pre-wrap [fs]
      (util/with-let [_ fs] (sync! fs) @start))))
