(ns hateoasql.core
  (:require [clojure.data.json :as json]
            [inflections.core :as inflec]
            [hateoasql.dao :as dao]
            [clojure.core.reducers :as r]
            [liberator.core :refer [resource defresource]]
            [ring.middleware.params :refer [wrap-params]]
            [compojure.core :refer [defroutes ANY]]
            [clojure.string :as s]))

(def ^:const serveraddress (or (System/getenv "BASE_URL") "http://localhost:3000/"))

(def table-restresource-map
  (let [dbtorest (into {} (map (fn [table] {table (s/lower-case (inflec/plural table))}) dao/tablenames))]
    {:dbtorest dbtorest
     :resttodb (into {} (map (fn [entry] {(second entry) (first entry)}) dbtorest))
     }
    ))

(defn to-rest [name] (get (get table-restresource-map :dbtorest) name))

(defn to-db [name] (get (get table-restresource-map :resttodb) name))

(extend-type java.sql.Timestamp
  json/JSONWriter
  (-write [date out]
    (json/-write (str date) out)))

(extend-type java.sql.Date
  json/JSONWriter
  (-write [date out]
    (json/-write (str date) out)))

(defn handle-resource-link [item instance]
  (println item)
  (if-not (nil? item)
    {(get item :source)
     (str serveraddress
          (to-rest (get item :source)) "/"
          (get instance (keyword (s/lower-case (get item :target_col)))))}))

(defn handle-resources-link [item id]
  (if-not (nil? item)
    {(get item :target)
     (str serveraddress
          (to-rest (get item :source)) "/" id "/"
          (to-rest (get item :target)))}))

(defn keys-to-links
  ([keys resource instance id]
   (let [links {:self (str serveraddress (to-rest resource) "/" id)}]
     (merge
      links
      (into {} (map (fn [item] (handle-resource-link item instance)) (get (get keys :target) resource)))
      (into {} (map (fn [item] (handle-resources-link item id)) (get (get keys :source) resource))))))
  ([keys resource resourceid subresource subresourceid instance]
   (if (nil? subresourceid)
     (let [links {}]
       (merge
        links
        (into {} (map (fn [item] (handle-resource-link item instance)) (get (get keys :target) subresource)))))
     (let [links {:self (str serveraddress (to-rest subresource) "/" subresourceid)}]
       (merge
        links
        (into {} (map (fn [item] (handle-resource-link item instance)) (get (get keys :target) subresource)))
        (into {} (map (fn [item] (handle-resources-link item subresourceid)) (get (get keys :source) subresource))))))))

(defn add-links
  ([resource instance id]
   (let [keys dao/relationship-maps]
     (assoc instance :links (keys-to-links keys resource instance id))))
  ([resource resourceid subresource subresourceid instance]
   (let [keys dao/relationship-maps]
     (assoc instance :links (keys-to-links keys resource resourceid subresource subresourceid instance)))))

(defn construct-resource [resource id]
  (let [instance (dao/get-by-id resource id)]
    (if (some? instance)
      (add-links resource instance id))))

(defresource database-resource [resource id]
  :available-media-types ["application/json"]
  :exists? (fn [_]
             (let [instance (construct-resource resource id)]
               (if (some? instance)
                 {:entry instance})))
  :handle-ok :entry)

(defresource database-resource-list [resource]
  :available-media-types ["application/json"]
  :handle-ok (map (fn [item] (add-links resource item (dao/get-pk-value resource item))) (dao/get-all resource)))

(defresource database-subresource [resource id subresource]
  :available-media-types ["application/json"]
  :handle-ok (map (fn [item] (add-links resource id subresource (dao/get-pk-value subresource item) item)) (dao/get-all-sub resource id subresource)))

(defroutes app
  (ANY "/:resource/:id" [resource id] (database-resource (to-db resource) id))
  (ANY "/:resource" [resource] (database-resource-list (to-db resource)))
  (ANY "/:resource/:id/:subresource" [resource id subresource] (database-subresource (to-db resource) id (to-db subresource))))

(def handler
  (-> app
      wrap-params))

