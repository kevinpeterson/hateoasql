(ns hateoasql.core
  (:require [clojure.data.json :as json]
            [hateoasql.dao :as dao]
            [clojure.core.reducers :as r]
            [liberator.core :refer [resource defresource]]
            [ring.middleware.params :refer [wrap-params]]
            [compojure.core :refer [defroutes ANY]]
            [clojure.string :as s]))

(def ^:const serveraddress "http://localhost:3000/")

(extend-type java.sql.Timestamp
  json/JSONWriter
  (-write [date out]
    (json/-write (str date) out)))

(defn handle-resource-link [item instance]
  (println item)
  (println instance)
  (if-not (nil? item)
    {(get item :source)
     (str serveraddress
          (get item :source) "/"
          (get instance (keyword (s/lower-case (get item :target_col)))))}))

(defn handle-resources-link [item id]
  (if-not (nil? item)
  {(get item :target)
   (str serveraddress
        (get item :source) "/" id "/"
        (get item :target))}))

(defn keys-to-links
  ([keys resource instance id]
  (let [links {:self (str serveraddress resource "/" id)}]
    (merge
      links
      (into {} (map (fn [item] (handle-resource-link item instance)) (get (get keys :target) resource)))
      (into {} (map (fn [item] (handle-resources-link item id)) (get (get keys :source) resource))))
    ))
  ([keys resource resourceid subresource subresourceid instance]
   (if (nil? subresourceid)
     (let [links {:self (str serveraddress resource "/" resourceid "/" subresource)}]
       (merge
         links
         (into {} (map (fn [item] (handle-resource-link item instance)) (get (get keys :target) subresource))))
       )
     (let [links {:self (str serveraddress subresource "/" subresourceid)}]
       (merge
         links
         (into {} (map (fn [item] (handle-resource-link item instance)) (get (get keys :target) subresource)))
         (into {} (map (fn [item] (handle-resources-link item subresourceid)) (get (get keys :source) subresource))))
       )
     )
  ))


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
           (ANY "/:resource/:id" [resource id] (database-resource resource id))
           (ANY "/:resource" [resource] (database-resource-list resource))
           (ANY "/:resource/:id/:subresource" [resource id subresource] (database-subresource resource id subresource)))

(def handler
  (-> app
      wrap-params))


