(ns schema-voyager.ingest.core
  (:require [datascript.core :as ds]
            [schema-voyager.data :as data]))

(defn into-db
  "Drops data processed by [[schema-voyager.data/process]] into a new datascript
  DB."
  [data]
  (ds/db-with (ds/empty-db data/metaschema) data))
