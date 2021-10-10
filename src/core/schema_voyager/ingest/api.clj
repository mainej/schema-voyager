(ns schema-voyager.ingest.api
  (:require
   [schema-voyager.ingest.core :as ingest]
   [schema-voyager.ingest.file :as ingest.file]))

(defn process [data]
  (ingest/process data))

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
