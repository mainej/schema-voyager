(ns schema-voyager.export)

(defn save-db [db]
  (spit "resources/schema_voyager_db.edn" (pr-str db)))
