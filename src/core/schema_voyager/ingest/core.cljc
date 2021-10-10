(ns schema-voyager.ingest.core
  "Process data from any kind of a source into a format ready to be put in a
  DataScript DB.

  Also provides tools for reading collection data from tagged literals and for
  deriving collections from attributes."
  (:refer-clojure :exclude [read-string])
  (:require [clojure.edn :as edn]
            [clojure.walk :as walk]))

(defn collection [type name]
  #:db.schema.collection{:type type, :name name})

(defn aggregate [name]
  (collection :aggregate name))

(defn enum [name]
  (collection :enum name))

(defn ^:deprecated read-schema-coll [[type name]]
  (collection type name))

(defn read-string
  "Reads `s` as EDN, converting
  ```clojure
  #schema/enum :foo ;; => #:db.schema.collection{:type :enum, :name :foo}
  #schema/agg :foo ;; => #:db.schema.collection{:type :aggregate, :name :foo}
  ```

  Still supported, but deprecated:
  ```
  #schema-coll[:enum :foo] ;; => #:db.schema.collection{:type :enum, :name :foo}
  #schema/coll[:agg :foo] ;; => #:db.schema.collection{:type :aggregate, :name :foo}
  ```"
  [s]
  #_{:clj-kondo/ignore #{:deprecated-var}}
  (edn/read-string {:readers {'schema-coll read-schema-coll
                              'schema/coll read-schema-coll
                              'schema/agg  aggregate
                              'schema/enum enum}}
                   s))

(defn- derive-element-type [e]
  (cond
    (:db.schema.collection/name e) :collection
    (:db/valueType e)              :attribute
    (or (:db.entity/attrs e)
        (:db.entity/preds e))      :entity-spec
    :else                          :constant))

(defn- attribute-derive-collection-type [attribute]
  (if (:db/valueType attribute)
    :aggregate
    :enum))

(defn- attribute-derive-collection-name [attribute]
  (keyword (namespace (:db/ident attribute))))

(defn attribute-derive-collection [attribute]
  (collection (attribute-derive-collection-type attribute)
              (attribute-derive-collection-name attribute)))

(defn attribute-derive-part-of [attribute]
  (get attribute :db.schema/part-of [(attribute-derive-collection attribute)]))

(defn- attribute-with-part-of [attribute]
  (assoc attribute :db.schema/part-of (attribute-derive-part-of attribute)))

(defn- element-with-element-type [e]
  (assoc e :db.schema.pseudo/type (derive-element-type e)))

(defn- merge-by [f items]
  (->> items
       (group-by f)
       (map (fn [[_group items]]
              (apply merge items)))))

(defn- coll-identity [coll]
  (select-keys coll [:db.schema.collection/type :db.schema.collection/name]))

(defn- replace-collections-by-temp-ids [collections entities]
  (let [coll-to-temp-id (zipmap (map coll-identity collections)
                                (map :db/id collections))]
    (walk/postwalk (fn [x] (if (map? x)
                             (get coll-to-temp-id (coll-identity x) x)
                             x))
                   entities)))

(defn- elements-of-type [types]
  (comp types :db.schema.pseudo/type))

(defn process
  "Prepare the provided list of schema `elements` for import into the DataScript DB.
  Each element should be an attribute, constant, entity-spec or collection.

  This function has two main purposes. First it gives attributes and constants
  their default `:db.schema/part-of`. Second, it converts literal collection
  references into full DataScript relationships, so that it's possible to
  navigate between collections via attributes."
  [elements]
  (let [elements     (->> elements
                          (merge-by #(select-keys % [:db/ident :db.schema.collection/type :db.schema.collection/name]))
                          (map element-with-element-type))
        attributes   (->> elements
                          ;; attributes and constants get the same treatment
                          (filter (elements-of-type #{:attribute :constant}))
                          (map attribute-with-part-of))
        entity-specs (->> elements
                          (filter (elements-of-type #{:entity-spec})))
        collections  (->> (concat (->> elements
                                       (filter (elements-of-type #{:collection})))
                                  (->> attributes
                                       (mapcat :db.schema/part-of)
                                       (map element-with-element-type)))
                          (merge-by coll-identity)
                          (map-indexed (fn [idx coll]
                                         (assoc coll :db/id (* -1 (inc idx))))))]
    (concat collections
            entity-specs
            (replace-collections-by-temp-ids collections attributes))))
