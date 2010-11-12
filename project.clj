(defproject mississippi "0.0.1-SNAPSHOT"
  :description "validations"
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [compojure "0.5.2"]
                 [ring/ring-core "0.3.1"]
                 [ring/ring-jetty-adapter "0.3.1"]]
  :dev-dependencies [[compojure "0.5.2"]
                     [ring/ring-core "0.3.1"]
                     [ring/ring-jetty-adapter "0.3.1"]
                     [ring/ring-devel "0.3.1"]
                     [lein-run "1.0.0"]]

  :run-aliases {:server [mississippi.web start-server]})
