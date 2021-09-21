(ns schema-voyager.build.db
  (:require
    [datascript.core :as ds]
    [schema-voyager.data :as data]))

(def ^:private default-db-path "resources/schema_voyager_db.edn")

(defn into-db
  "Drops data processed by [[schema-voyager.data/process]] into a new DataScript
  DB."
  [data]
  (ds/db-with (ds/empty-db data/metaschema) data))

(defn save-db
  "Persists a DataScript `db` at a given `file-path`."
  ([db] (save-db default-db-path db))
  ([file-path db]
   (spit (or file-path default-db-path) (pr-str db))))
