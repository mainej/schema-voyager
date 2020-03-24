(ns examples
  (:require [ingest.files]))

(def mbrainz-db
  (ingest.files/from-files ["resources/mbrainz-schema.edn"
                            "resources/mbrainz-enums.edn"
                            "resources/mbrainz-supplemental.edn"]))

(defn -main []
  (ingest.files/save-db mbrainz-db))

(comment
  (require '[clojure.pprint :as pprint])
  (require '[datascript.core :as d])
  (let [db mbrainz-db]
    #_(println
       (d/touch (d/entity db [:db/ident :track/artistCredit])))
    #_(println
       (d/touch (d/entity db [:db/ident :track/artists]))
       )
    #_(pprint/pprint
       (d/pull db
               '[* {:db.schema/_references [*]}]
               (d/q '[:find ?coll .
                      :where [?coll :db.schema.collection/name]]
                    db)))

    (pprint/pprint
     (d/pull
      db
      '[*
        {:db.schema/_part-of [*]}]
      (d/q '[:find ?coll .
             :where [?coll :db.schema.collection/name :release.type]]
           db)))

    #_(pprint/pprint
       (map d/touch (:db.schema/references (d/entity db [:db/ident :track/artists])))
       )
    ))
