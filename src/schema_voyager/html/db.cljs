(ns schema-voyager.html.db
  (:require [schema-voyager.data :as data]
            [reitit.frontend.controllers :as rfc]
            [reagent.core :as r]
            [shadow.resource :as resource]))

(def db
  (let [schema (data/read-string (resource/inline "mbrainz-schema.edn"))
        enums  (data/read-string (resource/inline "mbrainz-enums.edn"))
        supp   (data/read-string (resource/inline "mbrainz-supplemental.edn"))]
    (data/process (data/empty-db) (-> schema
                                      (data/join enums)
                                      (data/join supp)))))

(defonce !active-route (r/atom nil))

(defn active-route []
  (deref !active-route))

(defn save-route [new-match]
  (when new-match
    (swap! !active-route
           (fn [active-route]
             (let [controllers (rfc/apply-controllers (:controllers active-route) new-match)]
               (assoc new-match :controllers controllers))))))
