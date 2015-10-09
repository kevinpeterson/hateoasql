(ns hateoasql.dao
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.string :as s]
            [clojure.core.reducers :as r]))


(let [db-host "localhost"
      db-port 3306
      db-name "brite"]

  (def db {:classname "com.mysql.jdbc.Driver" ; must be in classpath
           :subprotocol "mysql"
           :subname (str "//" db-host ":" db-port "/" db-name)
           ; Any additional keys are passed to the driver
           ; as driver-specific properties.
           :user "root"
           :password ""}))

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

(def fk-map
  (let [fk-map (map (fn [table]
                      (get-foriegn-keys table)) tablenames)]
    (remove nil? (flatten fk-map))))

(def relationship-maps (let [entries (map (fn [entry]
                                            {:source (get entry :pktable_name)
                                             :source_col (get entry :pkcolumn_name)
                                             :target (get entry :fktable_name)
                                             :target_col (get entry :fkcolumn_name)}
                                            ) fk-map)]

                         (defn reduce-entries [key] (r/reduce (fn [newmap entry]
                                                                (if (not (contains? newmap (get entry key)))
                                                                  (assoc newmap (get entry key) [entry])
                                                                  (assoc newmap (get entry key) (conj (get newmap (get entry key)) entry)))
                                                                ) {} entries))


                         {:source (reduce-entries :source) :target (reduce-entries :target)}
                         ))



(defn create-by-id-sql [table] (str "select * from " table " where id = ?"))

(defn create-by-fk-sql [table]
  (let [fks (get (get relationship-maps :target) table)]
    (into {} (map (fn [item] {(get item :source) (str "select * from " (get item :target) " where " (get item :target_col) " = ?")}) fks))))

(defn create-select-all-sql [table] (str "select * from " table))

(defn get-pk-value [resource instance]
  (get instance (keyword (get primary-keys resource))))

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
    (println query)
    (jdbc/query db [query id])))



; Returns null..
;(defn fkin [table]
;  (jdbc/with-db-metadata [md db]
;                         (jdbc/metadata-result (.getExportedKeys md nil nil table))))
