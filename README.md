# boot-jetty
a simple webserver for boot

[](dependency)
```clojure
[tailrecursion/boot-jetty "0.1.2"] ;; latest release
```
[](/dependency)

## overview
this task creates and serves an internal distribution from the fileset's
output directories (resources and assets).  the distribution mirrors the
contents of the environment's target directory, but is served from jetty's
classpath as an exploded war file.

without additional configuration, this task functions as a static webserver
useful for client-side development.  to host a service, simply compose it
with boot's [web][web] task, which adds the [clojure-adapter-servlet][srv]
shim and a [web.xml][dsc] file to the distribution.

## use cases

sample task with boot-jetty as a dev server for a hoplon application
deployed to S3:

```clojure
(deftask develop []
  (comp (watch) (speak) (reload) (cljs-repl) (hoplon) (cljs) (serve :port 8000)))
```

sample task with boot-jetty as a clojure dev server:

```clojure
(deftask develop []
  (comp (watch) (speak) (web) (serve :port 8000)))
```

sample task with boot-jetty as a dev server for an angular js application
deployed to S3:

```clojure
(deftask develop []
  (comp (watch) (speak) (reload) (js) (less) (serve :port 8000)))
```

sample task with boot-jetty bootstrapping a spring framework service:

```clojure
(deftask develop []
  (comp (watch) (speak) (aot) (javac) (serve :port 8000)))
```

## windows users

If you get problems with locked files that can't be deleted, please try adding
these init parameters:

```clojure
(deftask develop []
  (comp ... (serve :init-params {"useFileMappedBuffer" "false"})))
```

[web]: https://github.com/boot-clj/boot/blob/master/boot/core/src/boot/task/built_in.clj#L499-L531
[srv]: https://github.com/tailrecursion/clojure-adapter-servlet
[dsc]: https://cloud.google.com/appengine/docs/java/config/webxml
