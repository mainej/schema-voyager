(ns schema-voyager.export)

(defn save-db [db]
  (spit "resources/db.edn" (pr-str db)))
