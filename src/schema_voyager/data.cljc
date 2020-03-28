(ns schema-voyager.data
  (:refer-clojure :exclude [read-string])
  (:require [clojure.edn :as edn]
            [clojure.walk :as walk]))

(defn read-schema-coll [[type name]]
  #:db.schema.collection{:type type, :name name})

(defn read-string
  "Reads `s` as EDN, converting `#schema-coll[:enum :foo]` into:
  `#:db.schema.collection{:type :enum, :name :foo}`"
  [s]
  (edn/read-string {:readers {'schema-coll read-schema-coll}} s))

(def metaschema
  {
   ;; Other attributes to which this attribute is related. Often used with
   ;; :db.schema/deprecated? to point to a new way of storing some data
   :db.schema/see-also         {:db/valueType   :db.type/ref
                                :db/cardinality :db.cardinality/many}
   ;; Which collection(s) this attribute is a part of. Usually derived from the
   ;; name and type of the attribute. Can be overridden for attributes that are
   ;; used on many aggregates.
   :db.schema/part-of          {:db/valueType   :db.type/ref
                                :db/cardinality :db.cardinality/many}
   ;; Which collection(s) this attribute refers to.
   :db.schema/references       {:db/valueType   :db.type/ref
                                :db/cardinality :db.cardinality/many}
   ;; Which collection(s) various parts of this tuple refers to.
   :db.schema/tuple-references {:db/valueType   :db.type/ref
                                :db/cardinality :db.cardinality/many}
   })

(defn entity-derive-collection-type [e]
  (if (:db/valueType e)
    :aggregate
    :enum))

(defn entity-derive-collection-name [e]
  (keyword (namespace (:db/ident e))))

(defn entity-derive-collection [e]
  {:db.schema.collection/type (entity-derive-collection-type e)
   :db.schema.collection/name (entity-derive-collection-name e)})

(defn entity-derive-part-of [e]
  (get e :db.schema/part-of [(entity-derive-collection e)]))

(defn entity-with-part-of [e]
  (assoc e :db.schema/part-of (entity-derive-part-of e)))

(defn- merge-by [f items]
  (->> items
       (group-by f)
       (map (fn [[_group items]]
              (apply merge items)))))

(defn coll-identity [coll]
  (select-keys coll [:db.schema.collection/type :db.schema.collection/name]))

(defn entity-spec? [attribute]
  (or (:db.entity/attrs attribute)
      (:db.entity/preds attribute)))

(defn replace-collections-by-temp-ids [collections entities]
  (let [coll-to-temp-id (zipmap (map coll-identity collections)
                                (map :db/id collections))]
    (walk/postwalk (fn [x] (if (map? x)
                             (get coll-to-temp-id (coll-identity x) x)
                             x))
                   entities)))

(defn process [attributes]
  (let [entities     (->> attributes
                          (filter :db/ident)
                          (remove entity-spec?)
                          (map entity-with-part-of))
        entity-specs (->> attributes
                          (filter entity-spec?))
        collections  (->> (concat (mapcat :db.schema/part-of entities)
                                  (filter :db.schema.collection/name attributes))
                          (merge-by coll-identity)
                          (map-indexed (fn [idx coll]
                                         (assoc coll :db/id (* -1 (inc idx))))))]
    (concat collections
            entity-specs
            (replace-collections-by-temp-ids collections entities))))

(defn join [& schemas]
  (->> schemas
       (apply concat)
       (merge-by #(select-keys % [:db/ident :db.schema.collection/type :db.schema.collection/name]))))
