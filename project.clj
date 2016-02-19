(defproject cia "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [clj-time "0.9.0"] ; required due to bug in lein-ring
                 [metosin/schema-tools "0.7.0"]
                 [metosin/compojure-api "1.0.0"]
                 [ring-middleware-format "0.7.0"]]
  :ring {:handler cia.handler/app
         :init cia.init/init-store
         :nrepl {:start? true}}
  :uberjar-name "server.jar"
  :profiles {:dev {:dependencies [[clj-http "2.0.1"]
                                  [cheshire "5.5.0"]
                                  [javax.servlet/servlet-api "2.5"]
                                  [ring/ring-jetty-adapter "1.4.0"]]
                   :plugins [[lein-ring "0.9.6"]]}})
