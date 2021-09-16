(ns ingest.files
  "A CLI for ingesting one or more schema files.

  ```
  clojure -A:ingest -m ingest.files <file>+
  ```"
  (:require [schema-voyager.data :as data]
            [schema-voyager.export :as export]
            [schema-voyager.ingest.core :as ingest]
            [schema-voyager.ingest.file :as ingest.file]))

(defn -main [& files]
  (->> files
       (mapcat ingest.file/ingest)
       data/process
       ingest/into-db
       export/save-db))
