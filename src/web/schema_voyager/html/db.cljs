(ns schema-voyager.html.db
  (:require
   [clojure.edn :as edn]
   [reagent.core :as r]
   [reitit.frontend.controllers :as rfc]
   [schema-voyager.db.query :as db]
   [shadow.resource :as resource]))

(def db
  (edn/read-string (resource/inline "schema_voyager_db.edn")))

(defonce !active-route (r/atom nil))

(defn active-route []
  (deref !active-route))

(defn save-route [new-match]
  (when new-match
    (swap! !active-route
           (fn [active-route]
             (let [controllers (rfc/apply-controllers (:controllers active-route) new-match)]
               (assoc new-match :controllers controllers))))))

(defn attribute-by-ident [ident]
  (db/attribute-by-ident db ident))

(defn collection-by-type-and-name [collection-type collection-name]
  (db/collection-by-type-and-name db collection-type collection-name))

(defn entity-spec-by-ident [ident]
  (db/entity-spec-by-ident db ident))

(defn collections-by-type [collection-type]
  (db/collections-by-type db collection-type))

(defn entity-specs []
  (db/entity-specs db))

(defn colls-edges []
  (db/colls-edges db))

(defn coll-edges [coll]
  (db/coll-edges db coll))

(defn attr-edges [attr]
  (db/attr-edges db attr))
