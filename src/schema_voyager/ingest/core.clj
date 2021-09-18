(ns schema-voyager.ingest.core
  (:require [datascript.core :as ds]
            [schema-voyager.data :as data]
            [schema-voyager.ingest.file :as ingest.file]))

(defn into-db
  "Drops data processed by [[schema-voyager.data/process]] into a new datascript
  DB."
  [data]
  (ds/db-with (ds/empty-db data/metaschema) data))

(defn datomic-attributes [params]
  ;; Do not require Datomic unless it is going to be used.
  ((requiring-resolve 'schema-voyager.ingest.datomic/cli-attributes) params))

(defn datomic-inferences [params]
  ;; Do not require Datomic unless it is going to be used.
  ((requiring-resolve 'schema-voyager.ingest.datomic/cli-inferences) params))

(defn datomic [params]
  ;; Do not require Datomic unless it is going to be used.
  ((requiring-resolve 'schema-voyager.ingest.datomic/cli-ingest) params))

(defn file [file-name]
  (ingest.file/ingest file-name))
