(ns schema-voyager.html.db
  (:require [reitit.frontend.controllers :as rfc]
            [reagent.core :as r]
            [clojure.edn :as edn]
            [schema-voyager.db :as db]
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

(defn attr-links-by-ident [idents]
  (db/attr-links-by-ident db idents))

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
