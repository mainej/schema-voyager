(ns examples
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]
            [schema-voyager.data :as data]
            [datascript.core :as d]))

(let [schema (edn/read-string (slurp (io/resource "mbrainz-schema.edn")))
      supp   (edn/read-string (slurp (io/resource "mbrainz-supplemental.edn")))
      conn   (data/empty-conn)
      db     (data/process conn (data/join schema supp))]
  (println
   (count (data/attribute-eids db)))

  (println
   (d/touch (d/entity db [:db/ident :track/artistCredit])))
  (println
   (d/touch (d/entity db [:db/ident :track/artists]))
   )
  )
