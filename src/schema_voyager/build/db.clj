(ns schema-voyager.build.db
  (:require [schema-voyager.db :as db]))

(def ^:private default-db-path "resources/schema_voyager_db.edn")

(defn into-db
  "Drops data processed by [[schema-voyager.data/process]] into a new DataScript
  DB."
  [data]
  (db/into-db data))

(defn save-db
  "Persists a DataScript `db` at a given `file-path`."
  ([db] (save-db default-db-path db))
  ([file-path db]
   (spit (or file-path default-db-path) (pr-str db))))
