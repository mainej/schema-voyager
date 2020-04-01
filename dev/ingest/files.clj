(ns ingest.files
  (:require [schema-voyager.ingest.files :as ingest.files]
            [schema-voyager.data :as data]
            [schema-voyager.ingest.core :as ingest]
            [schema-voyager.export :as export]))

(defn -main [& files]
  (->> files
       (map ingest.files/ingest)
       data/join-all
       data/process
       ingest/into-db
       export/save-db))
