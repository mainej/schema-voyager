(ns schema-voyager.db.init
  "Transact ingested schema data into a DataScript DB."
  (:require [datascript.core :as ds]))

(def metaschema
  "Definition of the supplemental attributes that can be transacted into the
  DataScript DB."
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
                                :db/doc         "Which collection(s) this attribute or constant is a part of. Usually derived from the namespace of the ident and whether it has a :db/valueType. Can be overridden for an attribute that is used on many aggregates, or whose namespace differs from the entities on which it appears."}
   :db.schema/references       {:db/valueType   :db.type/ref
                                :db/cardinality :db.cardinality/many
                                :db/doc         "Which collection(s) this attribute refers to."}
   :db.schema/tuple-references {:db/valueType   :db.type/ref
                                :db/cardinality :db.cardinality/many
                                :db/doc         "Which collection(s) various parts of this heterogeneous tuple refers to."}
   :db.schema.tuple/position   {;; :db/valueType   :db.type/long
                                :db/cardinality :db.cardinality/one
                                :db/doc         "The position of a ref within a heterogeneous tuple. Zero-indexed."}})
(defn into-db
  "Transacts data processed by [[schema-voyager.ingest.core/process]] into a new
  DataScript DB."
  [data]
  (ds/db-with (ds/empty-db metaschema) data))
