(ns schema-voyager.data
  (:refer-clojure :exclude [read-string])
  (:require [datascript.core :as d]
            [clojure.edn :as edn]))

(defn read-schema-coll [[type name]]
  {:db.schema.collection/type type
   :db.schema.collection/name name})

(defn read-string [s]
  (edn/read-string {:readers {'schema-coll read-schema-coll}} s))

(defn read-file [f]
  (read-string (slurp f)))

(def metaschema
  {
   ;; Other attributes to which this attribute is related. Often used with
   ;; :db.schema/deprecated? to point to a new way of storing some data
   :db.schema/see-also   {:db/valueType   :db.type/ref
                          :db/cardinality :db.cardinality/many}
   ;; Which collection(s) this attribute is a part of. Usually derived from the
   ;; name and type of the attribute. Can be overridden for attributes that are
   ;; used on many aggregates.
   :db.schema/part-of    {:db/valueType   :db.type/ref
                          :db/cardinality :db.cardinality/many}
   ;; Which collection(s) this attribute refers to.
   :db.schema/references {:db/valueType   :db.type/ref
                          :db/cardinality :db.cardinality/many}})

(defn empty-db []
  (d/empty-db metaschema))

(defn entity-derive-collection-type [e]
  (if (:db/valueType e)
    :aggregate
    :enum))

(defn entity-derive-collection-name [e]
  (keyword (namespace (:db/ident e))))

(defn entity-derive-collection [e]
  {:db.schema.collection/type (entity-derive-collection-type e)
   :db.schema.collection/name (entity-derive-collection-name e)})

(defn entity-with-part-of [e]
  (if (:db.schema/part-of e)
    e
    (assoc e :db.schema/part-of [(entity-derive-collection e)])))

(defn process [db entities]
  (let [entities      (map entity-with-part-of entities)
        index-by-coll (zipmap (set (mapcat :db.schema/part-of entities))
                              (map (fn [idx]
                                     (* -1 (inc idx)))
                                   (range)))
        entities      (map (fn [e]
                             (cond-> (update e :db.schema/part-of #(mapv index-by-coll %))
                               (seq (:db.schema/references e)) (update :db.schema/references #(mapv index-by-coll %))))
                           entities)
        collections   (map (fn [[coll idx]]
                             (assoc coll :db/id idx))
                           index-by-coll)]
    (d/db-with db (concat collections entities))))

(defn join [& schemas]
  (->> schemas
       (apply concat)
       (group-by :db/ident)
       (map (fn [[_ident items]]
              (apply merge items)))))
