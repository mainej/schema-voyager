(ns schema-voyager.data
  (:require [datascript.core :as d]))

(defn empty-conn [] (d/create-conn))

(defn process [conn data]
  (d/transact! conn data)
  @conn)

(defn join [& schemas]
  (->> schemas
       (apply concat)
       (group-by :db/ident)
       (map (fn [[_ident items]]
              (apply merge items)))))

(defn attribute-eids [db]
  (->> (d/q '[:find ?attribute
              :where
              [?attribute :db/ident]
              [?attribute :db/valueType]]
            db)
       (map first)
       (into #{})))

(defn constant-eids [db]
  (->> (d/q '[:find ?attribute
              :in $
              :where
              [?attribute :db/ident]
              [(missing? $ ?attribute :db/valueType)]]
            db)
       (map first)
       (into #{})))

(defn constants [db]
  (d/pull-many db '[*] (constant-eids db)))

(defn attributes [db]
  (d/pull-many db '[*] (attribute-eids db)))
