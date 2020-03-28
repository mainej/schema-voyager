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

(defn derive-element-type [e]
  (cond
    (:db.schema.collection/name e) :collection
    (:db/valueType e)              :attribute
    (or (:db.entity/attrs e)
        (:db.entity/preds e))      :entity-spec
    :else                          :constant))

(defn attribute-derive-collection-type [attribute]
  (if (:db/valueType attribute)
    :aggregate
    :enum))

(defn attribute-derive-collection-name [attribute]
  (keyword (namespace (:db/ident attribute))))

(defn attribute-derive-collection [attribute]
  {:db.schema.collection/type (attribute-derive-collection-type attribute)
   :db.schema.collection/name (attribute-derive-collection-name attribute)})

(defn attribute-derive-part-of [attribute]
  (get attribute :db.schema/part-of [(attribute-derive-collection attribute)]))

(defn attribute-with-part-of [attribute]
  (assoc attribute :db.schema/part-of (attribute-derive-part-of attribute)))

(defn element-with-element-type [e]
  (assoc e :db.schema.pseudo/type (derive-element-type e)))

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

(defn elements-of-type [types]
  (comp types :db.schema.pseudo/type))

(defn process [elements]
  (let [elements     (->> elements
                          (map element-with-element-type))
        attributes   (->> elements
                          (filter (elements-of-type #{:attribute :constant}))
                          (map attribute-with-part-of))
        entity-specs (->> elements
                          (filter (elements-of-type #{:entity-spec})))
        collections  (->> (concat (->> attributes
                                       (mapcat :db.schema/part-of)
                                       (map element-with-element-type))
                                  (->> elements
                                       (filter (elements-of-type #{:collection}))))
                          (merge-by coll-identity)
                          (map-indexed (fn [idx coll]
                                         (assoc coll :db/id (* -1 (inc idx))))))]
    (concat collections
            entity-specs
            (replace-collections-by-temp-ids collections attributes))))

(defn join [& schemas]
  (->> schemas
       (apply concat)
       (merge-by #(select-keys % [:db/ident :db.schema.collection/type :db.schema.collection/name]))))
