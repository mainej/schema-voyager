(ns examples
  (:require [clojure.java.io :as io]
            [clojure.pprint :as pprint]
            [schema-voyager.data :as data]
            [datascript.core :as d]))

(def mbrainz-db
  (let [schema (data/read-string (slurp (io/resource "mbrainz-schema.edn")))
        enums  (data/read-string (slurp (io/resource "mbrainz-enums.edn")))
        supp   (data/read-string (slurp (io/resource "mbrainz-supplemental.edn")))]
    (data/process (data/empty-db) (-> schema
                                      (data/join enums)
                                      (data/join supp)))))

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
  )
