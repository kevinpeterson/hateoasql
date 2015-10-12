(ns hateoasql.dao
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.string :as s]
            [jdbc.pool.c3p0 :as pool]
            [clojure.core.reducers :as r]))

(def db-uri
  (let [url (or (System/getenv "DATABASE_URL") (System/getenv "CLEARDB_DATABASE_URL"))]
    (if-not (nil? url)
    (java.net.URI. url))))

(def user-and-password
  (if-not (nil? db-uri)
    (if (nil? (.getUserInfo db-uri))
      nil (clojure.string/split (.getUserInfo db-uri) #":"))))

(def db
  (pool/make-datasource-spec
    (if (nil? db-uri)
      (let [db-host (or (System/getenv "DB_HOST")
                        "localhost")
            db-driver (or (System/getenv "DB_DRIVER")
                          "com.mysql.jdbc.Driver")
            db-port (or (System/getenv "DB_PORT")
                        3306)
            db-name (System/getenv "DB")
            db-type (or (System/getenv "DB_TYPE")
                        "mysql")
            db-user (System/getenv "DB_USER")
            db-password (System/getenv "DB_PASSWORD")]
        {:classname db-driver
       :subprotocol db-type
       :subname (str "//" db-host ":" db-port "/" db-name)
       :user db-user
       :password db-password}
      )

        {:classname   "com.mysql.jdbc.Driver"
         :subprotocol (or "mysql" (System/getenv "DB_TYPE"))
         :user        (or (get user-and-password 0) (System/getenv "DB_USER"))
         :password    (or (get user-and-password 1) (System/getenv "DB_PASSWORD"))
         :subname     (if (= -1 (.getPort db-uri))
                        (format "//%s%s" (.getHost db-uri) (.getPath db-uri))
                        (format "//%s:%s%s" (.getHost db-uri) (.getPort db-uri) (.getPath db-uri)))

         :test-connection-on-borrow true
         :test-connection-on-return true
         :max-connection-lifetime 10 * 1000
         :test-connection-query "SELECT 1"}
        )))

(defn get-foriegn-keys [table]
  (jdbc/with-db-metadata [md db]
    (jdbc/metadata-result (.getImportedKeys md nil nil table))))

(def tablenames
  (jdbc/with-db-metadata [md db]
    (let [tables (jdbc/metadata-result (.getTables md nil nil nil (into-array ["TABLE" "VIEW"])))]
      (map (fn [entry] (get entry :table_name)) tables))))

(def primary-keys
  (into {} (remove nil? (map (fn [table] (jdbc/with-db-metadata [md db]
                                           (let [result (jdbc/metadata-result (.getPrimaryKeys md nil nil table))]
                                             (if (= 1 (count result))
                                               {table (get (first result) :column_name)})))) tablenames))))
(def tables-with-primary-key
  (filter (fn [table] (not (nil? (get primary-keys table)))) tablenames))

(def fk-map
  (let [fk-map (map (fn [table]
                      (get-foriegn-keys table)) tablenames)]
    (remove nil? (flatten fk-map))))

(def relationship-maps (let [entries (map (fn [entry]
                                            {:source (get entry :pktable_name)
                                             :source_col (get entry :pkcolumn_name)
                                             :target (get entry :fktable_name)
                                             :target_col (get entry :fkcolumn_name)}) fk-map)]

                         (defn reduce-entries [key] (r/reduce (fn [newmap entry]
                                                                (if (not (contains? newmap (get entry key)))
                                                                  (assoc newmap (get entry key) [entry])
                                                                  (assoc newmap (get entry key) (conj (get newmap (get entry key)) entry)))) {} entries)) {:source (reduce-entries :source) :target (reduce-entries :target)}))

(defn create-by-id-sql [table] (str "select * from " table " where " (get primary-keys table) " = ?"))

(defn create-by-fk-sql [table]
  (let [fks (get (get relationship-maps :target) table)]
    (into {} (map (fn [item] {(get item :source) (str "select * from " (get item :target) " where " (get item :target_col) " = ?")}) fks))))

(defn create-select-all-sql [table] (str "select * from " table))

(defn get-pk-value [resource instance]
  (let [pk (get primary-keys resource)]
    (if-not (nil? pk)
    (get instance (keyword (s/lower-case pk))))))

(def by-id-queries (into {} (map (fn [tablename] {tablename (create-by-id-sql tablename)}) tablenames)))

(def by-fk-queries (into {} (map (fn [tablename] {tablename (create-by-fk-sql tablename)}) tablenames)))

(def by-select-all-queries (into {} (map (fn [tablename] {tablename (create-select-all-sql tablename)}) tablenames)))

(defn get-by-id [resource id]
  (let [instance (jdbc/query db [(get by-id-queries resource) id])]
    (if (= 1 (count instance))
      (first instance)
      nil)))

(defn get-all [resource]
  (jdbc/query db [(get by-select-all-queries resource)]))

(defn get-all-sub [resource id subresource]
  (let [query (get (get by-fk-queries subresource) resource)]
    (jdbc/query db [query id])))
