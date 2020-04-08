(ns ingest.datomic
  "A CLI for ingesting from a Datomic DB.

  This is useful to get a 'quick view' of an unfamiliar database, one that
  hasn't yet been annotated with supplemental schema. However, in the long run
  you should use the output from `schema.voyager.datomic/infer-references` and
  `schema.voyager.datomic/infer-deprecations` as a starting point for your own
  supplemental schema.

  WARNING: This has not been tested on large databases, where it may have
  performance impacts. Use at your own risk.

  ```
  clj -A:ingest:datomic -m ingest.datomic <db-name> <datomic-client-config-edn>
  ```"
  (:require [schema-voyager.ingest.datomic :as ingest.datomic]
            [schema-voyager.data :as data]
            [schema-voyager.ingest.core :as ingest]
            [schema-voyager.export :as export]
            [clojure.edn :as edn]))

(defn -main [db-name client-config]
  (let [db (ingest.datomic/datomic-db (edn/read-string client-config)
                                      db-name)]
    (->> (data/join (ingest.datomic/ingest db)
                    (ingest.datomic/infer-references db)
                    (ingest.datomic/infer-deprecations db))
         data/process
         ingest/into-db
         export/save-db)))
