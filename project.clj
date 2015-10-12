(defproject hateosql "0.1.0-SNAPSHOT"
  :min-lein-version "2.0.0"
  :plugins [[lein-ring "0.8.11"]
            [lein-cljfmt "0.3.0"]]
  :ring {:handler hateoasql.core/handler}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [liberator "0.13"]
                 [compojure "1.3.4"]
                 [inflections "0.9.14"]
                 [ring/ring-core "1.2.1"]
                 [org.clojure/java.jdbc "0.4.2"]
                 [clojure.jdbc/clojure.jdbc-c3p0 "0.3.1"]
                 [mysql/mysql-connector-java "5.1.21"]])
