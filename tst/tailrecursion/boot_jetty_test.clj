(ns tailrecursion.boot-jetty-test
  (:require
    [org.httpkit.client :as http]
    [clojure.test :refer :all] ))

;;; private ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

#_(def request
  (let [body (proxy [javax.servlet.ServletInputStream]   [])
        cert (proxy [java.security.cert.X509Certificate] []) ]
    {:server-port         8080
     :server-name         "foobar"
     :remote-addr         "127.0.0.1"
     :uri                 "/foo"
     :query-string        "a=b"
     :scheme              :http
     :request-method       :get
     :headers              {"X-Client" ["Foo", "Bar"], "X-Server" ["Baz"]}
     :content-type         "text/plain"
     :content-length       10
     :character-encoding   "UTF-8"
     :servlet-context-path "/foo"
     :ssl-client-cert      cert
     :body                 body }))

(def uri "http://localhost:3005/")

;;; tests ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

#_(post (str uri "" (name symbol)) {:form-params map :insecure? true})

(deftest test-get
  (let [res @(http/get uri)]
    (prn :res res)
    #_(is (= 200 (res :status)))
    #_(is (= (get-in res [:headers "content-type"]) "text/plain"))
    #_(is (= (res :body) "Boot Jetty")) ))
