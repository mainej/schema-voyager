(ns examples
  (:require [schema-voyager.ingest.files :as ingest.files]
            [schema-voyager.data :as data]
            [schema-voyager.ingest.core :as ingest]
            [schema-voyager.export :as export]))

(def mbrainz-db
  "A datascript DB that contains the mbrainz schema, enums, and supplemental
  data about references.

  Loaded out of resources/mbrainz-*.edn files."
  (-> ["resources/mbrainz-schema.edn"
       "resources/mbrainz-enums.edn"
       "resources/mbrainz-supplemental.edn"]
      ingest.files/ingest
      data/process
      ingest/into-db))

(defn -main []
  (export/save-db mbrainz-db))

(comment
  (require '[clojure.pprint :as pprint])
  (require '[datascript.core :as d])

  (println (d/touch (d/entity mbrainz-db [:db/ident :track/artistCredit])))
  (println (d/touch (d/entity mbrainz-db [:db/ident :track/artists])))
  (pprint/pprint
   (d/pull mbrainz-db
           '[* {:db.schema/_references [*]}]
           (d/q '[:find ?coll .
                  :where [?coll :db.schema.collection/name]]
                mbrainz-db)))

  (pprint/pprint
   (d/pull mbrainz-db
           '[* {:db.schema/_part-of [*]}]
           (d/q '[:find ?coll .
                  :where [?coll :db.schema.collection/name :release.type]]
                mbrainz-db)))

  (pprint/pprint
   (map d/touch (:db.schema/references (d/entity mbrainz-db [:db/ident :track/artists]))))
  )
