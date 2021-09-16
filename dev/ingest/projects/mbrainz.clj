(ns ingest.projects.mbrainz
  (:require [schema-voyager.data :as data]
            [schema-voyager.export :as export]
            [schema-voyager.ingest.core :as ingest]
            [schema-voyager.ingest.file :as ingest.file]))

(def mbrainz-db
  "A datascript DB that contains the mbrainz schema, enums, and supplemental
  data about references.

  Loaded out of resources/mbrainz-*.edn files."
  (->> ["resources/mbrainz-schema.edn"
        "resources/mbrainz-enums.edn"
        "resources/mbrainz-supplemental.edn"]
       (mapcat ingest.file/ingest)
       data/process
       ingest/into-db))

(defn -main []
  (export/save-db mbrainz-db))

(comment
  (require '[clojure.pprint :as pprint])
  (require '[datascript.core :as d])

  (println
   ;; The (deprecated) :track/artistCredit attribute
   (d/touch (d/entity mbrainz-db [:db/ident :track/artistCredit])))
  (println
   ;; The :track/artist attribute
   (d/touch (d/entity mbrainz-db [:db/ident :track/artists])))
  (pprint/pprint
   ;; A collection and the attributes that refer to it
   (d/pull mbrainz-db
           '[* {:db.schema/_references [*]}]
           (d/q '[:find ?coll .
                  :where [?coll :db.schema.collection/name]]
                mbrainz-db)))

  (pprint/pprint
   ;; The :release.type enum, and the constants it includes
   (d/pull mbrainz-db
           '[* {:db.schema/_part-of [*]}]
           (d/q '[:find ?coll .
                  :where [?coll :db.schema.collection/name :release.type]]
                mbrainz-db)))

  (pprint/pprint
   ;; The collections that the :track/artists attribute references
   (map d/touch (:db.schema/references (d/entity mbrainz-db [:db/ident :track/artists]))))
  )
