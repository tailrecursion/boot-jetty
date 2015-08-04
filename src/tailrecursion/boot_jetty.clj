(ns tailrecursion.boot-jetty
  {:boot/export-tasks true}
  (:require
    [boot.pod  :as pod]
    [boot.core :as boot]
    [boot.util :as util] ))

(def ^:private deps
  (delay (remove pod/dependency-loaded?
   '[[org.clojure/data.xml           "0.0.8"]
     [org.eclipse.jetty/jetty-server "9.3.1.v20150714"] ])))

(def ^:private paths)

(boot/deftask serve
  "Serve the application, reloading all namspaces with each subsequent invocation."
  [p port PORT int "The port the server will bind to."]
  (let [message #(util/info (str % " Jetty on port %s...\n") port)
        rmpaths #(dissoc % :asset-paths :source-paths :resource-paths :target-path)
        srv-env (-> (boot/get-env) (rmpaths) (update :dependencies into (vec (seq @deps))))
        app-env (-> (boot/get-env) (rmpaths))
        srv-pod (future (pod/make-pod srv-env))
        app-dir (boot/tmp-dir!)
        create  (delay (pod/with-call-in @srv-pod
                  (tailrecursion.boot-jetty.server/create!
                    ~app-env ~(.getPath app-dir) ~port )))]
    (boot/cleanup
      (message "\nStopping")
      (pod/with-call-in @srv-pod (tailrecursion.boot-jetty.server/destroy!)) )
    (boot/with-pre-wrap fileset
      (apply boot/sync! app-dir (boot/output-dirs fileset))
      (if-not (realized? create)
        (do (message "Starting")
            (deref create) )
        (do (message "Restarting")
            (pod/with-call-in @srv-pod (tailrecursion.boot-jetty.server/refresh!) )))
      fileset )))


      ; (def ^:private srv-deps
      ;   '[[boot/core                       "2.1.2"]
      ;     [org.clojure/data.xml            "0.0.8"]
      ;     [org.eclipse.jetty/jetty-servlet "9.3.1.v20150714"]
      ;     [org.eclipse.jetty/jetty-server  "9.3.1.v20150714"]
      ;     [org.eclipse.jetty/jetty-app-dir  "9.3.1.v20150714"] ])
      ;
      (def ^:private reps
        [["clojars"       "https://clojars.org/repo/"]
         ["maven-central" "https://repo1.maven.org/maven2/"]] )
      ;
      ; (boot/deftask serve
      ;   "Serve the application, reloading all namspaces with each subsequent invocation."
      ;   [p port PORT int "The port the server will bind to."]
      ;   (let [message  #(util/info (str % " Jetty on port %s...\n") port)
      ;       ;  app-deps (:dependencies   (boot/get-env))
      ;         app-env  (boot/get-env) #_(hash-map :dependencies app-deps :repositories reps :watcher-debounce 10)
      ;         srv-env  (-> (boot/get-env) (assoc :dependencies srv-deps))
      ;         srv-pod  (future (pod/make-pod srv-env))
      ;         app-dir  (boot/tmp-dir!)
      ;         create   (delay (pod/with-call-in @srv-pod
      ;                     (tailrecursion.boot-jetty.server/create! app-env
      ;                       ~(.getPath app-dir) ~port))) ]
      ;     (boot/cleanup
      ;       (message "\nStopping")
      ;       (pod/with-call-in @srv-pod (tailrecursion.boot-jetty.server/destroy!)) )
      ;     (boot/with-pre-wrap fileset
      ;       (apply boot/sync! app-dir (boot/output-dirs fileset))
      ;       (if-not (realized? create)
      ;         (do (message "Starting")
      ;             (deref create) )
      ;         (do (message "Restarting")
      ;             (pod/with-call-in @srv-pod (tailrecursion.boot-jetty.server/refresh!) )))
      ;       fileset )))
