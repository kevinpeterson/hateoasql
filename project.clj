(defproject hateosql "0.1.0-SNAPSHOT"
  :plugins [[lein-ring "0.8.11"]
            [lein-cljfmt "0.3.0"]]
  :ring {:handler hateoasql.core/handler}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [liberator "0.13"]
                 [compojure "1.3.4"]
                 [ring/ring-core "1.2.1"]
                 [org.clojure/java.jdbc "0.4.2"]
                 [mysql/mysql-connector-java "5.1.21"]])
