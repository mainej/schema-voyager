(ns ingest.files
  "A CLI for ingesting one or more schema files.

  ```
  clojure -M:ingest -m ingest.files <file>+
  ```"
  (:require [schema-voyager.data :as data]
            [schema-voyager.export :as export]
            [schema-voyager.ingest.core :as ingest]
            [schema-voyager.ingest.file :as ingest.file]))

(defn -main [& files]
  (->> files
       (map ingest.file/ingest)
       data/join-all
       data/process
       ingest/into-db
       export/save-db))
