(ns schema-voyager.ingest.api
  "A facade for calling into the other ingest namespaces.

  The main purpose of this namespace is to avoid loading
  [[schema-voyager.ingest.datomic]] unless it is needed. This lets users who
  need to ingest from Datomic do so, without requiring that all users load the
  Datomic deps."
  (:require
   [schema-voyager.ingest.core :as ingest]
   [schema-voyager.ingest.file :as ingest.file]))

(defn process
  "A facade for [[schema-voyager.ingest.core/process]]"
  [data]
  (ingest/process data))

(defn datomic-attributes
  "A facade for [[schema-voyager.ingest.datomic/cli-attributes]]"
  [params]
  ;; Do not require Datomic unless it is going to be used.
  ((requiring-resolve 'schema-voyager.ingest.datomic/cli-attributes) params))

(defn datomic-inferences
  "A facade for [[schema-voyager.ingest.datomic/cli-inference]]"
  [params]
  ;; Do not require Datomic unless it is going to be used.
  ((requiring-resolve 'schema-voyager.ingest.datomic/cli-inferences) params))

(defn datomic
  "A facade for [[schema-voyager.ingest.datomic/cli-ingest]]"
  [params]
  ;; Do not require Datomic unless it is going to be used.
  ((requiring-resolve 'schema-voyager.ingest.datomic/cli-ingest) params))

(defn file
  "A facade for [[schema-voyager.ingest.file/ingest]]"
  [file-name]
  (ingest.file/ingest file-name))
