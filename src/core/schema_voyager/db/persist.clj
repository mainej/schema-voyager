(ns schema-voyager.db.persist
  "Save a DataScript DB where it can be used by the web UI.")

(def ^:private default-db-path "resources/schema_voyager_db.edn")

(defn save-db
  "Persists a DataScript `db` at a given `file-path`, by default
  `resources/schema_voyager_db.edn`."
  ([db] (save-db default-db-path db))
  ([file-path db]
   (spit (or file-path default-db-path) (pr-str db))))
