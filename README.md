# boot-jetty
a simple webserver for boot

## overview
this task creates and serves an internal distribution from the fileset's output directories (resources and assets).  the distribution mirrors the contents of the environment's target directory, but is served from jetty's classpath as an exploded war file.

without additional configuration, this task functions as a static webserver useful for client-side development.  to host a service, simply compose it with boot's `(web)` task, which adds the [clojure-servlet-adapter][adapter] shim and a web.xml file to the distribution.

## use cases

sample task with boot-jetty as a dev server for a hoplon application deployed to S3:
```clojure

(deftask develop []
  (comp (watch) (speak) (reload) (cljs-repl) (hoplon) (cljs) (serve)) )
```

sample task with boot-jetty as a clojure dev server:
```clojure

(deftask develop []
  (comp (watch) (speak) (web) (serve)) )
```

sample task with boot-jetty as a dev server for an angular js application deployed to S3:
```clojure

(deftask develop []
  (comp (watch) (speak) (reload) (js) (less) (serve)) )
```

sample task with boot-jetty bootstrapping a spring framework service:
```clojure

(deftask develop []
  (comp (watch) (speak) (aot) (javac) (serve)) )
```
