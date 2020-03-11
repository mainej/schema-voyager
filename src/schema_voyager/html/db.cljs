(ns schema-voyager.html.db
  (:require [schema-voyager.data :as data]
            [shadow.resource :as resource]))

(def mbrainz-db
  (let [schema (data/read-string (resource/inline "mbrainz-schema.edn"))
        enums  (data/read-string (resource/inline "mbrainz-enums.edn"))
        supp   (data/read-string (resource/inline "mbrainz-supplemental.edn"))]
    (data/process (data/empty-db) (-> schema
                                      (data/join enums)
                                      (data/join supp)))))
