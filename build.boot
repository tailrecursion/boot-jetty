(set-env!
  :resource-paths #{"src"}
  :source-paths   #{"tst"}
  :target-path      "tgt"
  :dependencies  '[[org.clojure/clojure             "1.7.0"  :scope "provided"]
                   [boot/core                       "2.3.0"  :scope "provided"]
                   [adzerk/bootlaces                "0.1.11" :scope "test"]
                   [adzerk/boot-test                "1.0.4"  :scope "test"]
                   [clj-http                        "2.0.0"  :scope "test"]
                   [ring/ring-mock                  "0.3.0"  :scope "test"]
                   [javax.servlet/javax.servlet-api "3.1.0"] ]
 :repositories  [["clojars"       "https://clojars.org/repo/"]
                 ["maven-central" "https://repo1.maven.org/maven2/"] ])
(require
  '[adzerk.bootlaces         :refer :all]
  '[adzerk.boot-test         :refer [test]]
  '[tailrecursion.boot-jetty :refer [serve]] )

(def +version+ "0.1.0-SNAPSHOT")

(bootlaces! +version+)

(replace-task!
  [t test] (fn [& xs] (comp (web) (serve) (apply t xs))) )

(deftask build []
  (comp (test) (build-jar)) )

(deftask develop []
  (comp (watch) (speak) (web) (serve) (test)) )

(task-options!
  pom  {:project     'tailrecursion/boot-jetty
        :version     +version+
        :description "Boot jetty server."
        :url         "https://github.com/tailrecursion/boot-jetty"
        :scm         {:url "https://github.com/tailrecursion/boot-jetty"}
        :license     {"EPL" "http://www.eclipse.org/legal/epl-v10.html"} }
  serve {:port       3005}
  test  {:namespaces #{'tailrecursion.boot-jetty-test}}
  web   {:serve      'tailrecursion.boot-jetty-app/serve} )
