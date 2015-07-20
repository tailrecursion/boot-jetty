(set-env!
  :resource-paths #{"src"}
  :target-path      "tgt"
  :dependencies  '[[org.clojure/clojure "1.7.0"     :scope "provided"]
                   [boot/core           "2.1.2"     :scope "provided"]
                   [adzerk/bootlaces    "0.1.11"    :scope "test"] ])

 (require '[adzerk.bootlaces :refer :all])

 (def +version+ "0.1.0-SNAPSHOT")

 (bootlaces! +version+)

 (task-options!
  pom  {:project     'tailrecursion/boot-jetty
        :version     +version+
        :description "Boot jetty server."
        :url         "https://github.com/tailrecursion/boot-jetty"
        :scm         {:url "https://github.com/tailrecursion/boot-jetty"}
        :license     {"EPL" "http://www.eclipse.org/legal/epl-v10.html"} })
