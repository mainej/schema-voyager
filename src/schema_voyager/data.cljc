(ns schema-voyager.data
  (:refer-clojure :exclude [read-string])
  (:require [clojure.edn :as edn]
            [clojure.walk :as walk]))

(defn collection [type name]
  #:db.schema.collection{:type type, :name name})

(defn aggregate [name]
  (collection :aggregate name))

(defn enum [name]
  (collection :enum name))

(defn read-schema-coll [[type name]]
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
  #schema/coll[:enum :foo] ;; => #:db.schema.collection{:type :enum, :name :foo}
  ```"
  [s]
  (edn/read-string {:readers {'schema-coll read-schema-coll
                              'schema/coll read-schema-coll
                              'schema/agg  aggregate
                              'schema/enum enum}}
                   s))

(def metaschema
  {:db.schema.collection/name  {;; :db/valueType   :db.type/keyword
                                :db/cardinality :db.cardinality/one
                                :db/doc         "The name of a collection. Can be any keyword, but usually matches the namespace of other idents in the schema."}
   :db.schema.collection/type  {;; :db/valueType   :db.type/keyword
                                :db/cardinality :db.cardinality/one
                                :db/doc         "The type of a collection, either :aggregate or :enum."}
   :db.schema/deprecated?      {;; :db/valueType   :db.type/boolean
                                :db/cardinality :db.cardinality/one
                                :db/doc         "Whether this attribute or constant has fallen out of use. Often used with :db.schema/see-also, to point to a new way of storing some data."}
   :db.schema/see-also         {:db/valueType   :db.type/ref
                                :db/cardinality :db.cardinality/many
                                :db/doc         "Other attributes to which this attribute is related. Often used with :db.schema/deprecated? to point to a new way of storing some data."}
   :db.schema/part-of          {:db/valueType   :db.type/ref
                                :db/cardinality :db.cardinality/many
                                :db/doc         "Which collection(s) this attribute or constant is a part of. Usually derived from the type and namespace of the ident. Can be overridden for attributes that are used on many aggregates, or which have many versions."}
   :db.schema/references       {:db/valueType   :db.type/ref
                                :db/cardinality :db.cardinality/many
                                :db/doc         "Which collection(s) this attribute refers to."}
   :db.schema/tuple-references {:db/valueType   :db.type/ref
                                :db/cardinality :db.cardinality/many
                                :db/doc         "Which collection(s) various parts of this heterogeneous tuple refers to."}
   :db.schema.tuple/position   {;; :db/valueType   :db.type/long
                                :db/cardinality :db.cardinality/one
                                :db/doc         "The position of a ref within a heterogeneous tuple. Zero-indexed."}})

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
  "Prepare the provided list of schema `elements` for import into the Datascript DB.
  Each element should be an attribute, constant, entity-spec or collection.

  This function has two main purposes. First it gives attributes and constants
  their default `:db.schema/part-of`. Second, it convert literal collection
  references into full Datascript relationships, so that it is possible to
  navigate between collections via attributes."
  [elements]
  (let [elements     (->> elements
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

(defn join
  "Join schema from several `sources`. Data from later sources overrides earlier
  sources."
  [& sources]
  (->> sources
       (apply concat)
       (merge-by #(select-keys % [:db/ident :db.schema.collection/type :db.schema.collection/name]))))

(defn join-all
  "Like [[join]], but for a `->>` context."
  [sources]
  (apply join sources))
