(ns schema-voyager.db.persist)

(def ^:private default-db-path "resources/schema_voyager_db.edn")

(defn save-db
  "Persists a DataScript `db` at a given `file-path`."
  ([db] (save-db default-db-path db))
  ([file-path db]
   (spit (or file-path default-db-path) (pr-str db))))
