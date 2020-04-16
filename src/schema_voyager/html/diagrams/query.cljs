(ns schema-voyager.html.diagrams.query
  (:require [clojure.walk :as walk]
            [datascript.core :as ds]))

(def ^:private ref-q
  {:find  '[?source ?dest ?source-attr]
   :where ['[?source-attr :db.schema/part-of ?source]
           '(or-join [?source-attr ?dest]
                     [?source-attr :db.schema/references ?dest]
                     (and
                      [?source-attr :db.schema/tuple-references ?dest-tuple-ref]
                      [?dest-tuple-ref :db.schema/references ?dest]))]})

(def ^:private active-ref-q
  (update ref-q :where conj '(not [?source-attr :db.schema/deprecated? true])))

(defn- expand-eids [db refs]
  (let [eids            (distinct (mapcat identity refs))
        entities        (ds/pull-many db '[*] eids)
        entities-by-eid (zipmap (map :db/id entities)
                                entities)]
    (walk/postwalk-replace entities-by-eid refs)))

(defn colls [db]
  (let [refs (ds/q active-ref-q db)]
    (expand-eids db refs)))

(defn coll [db coll]
  (let [coll-eid (ds/q '[:find ?coll .
                         :in $ ?collection-type ?collection-name
                         :where
                         [?coll :db.schema.collection/type ?collection-type]
                         [?coll :db.schema.collection/name ?collection-name]
                         [?coll :db.schema.pseudo/type :collection]]
                       db (:db.schema.collection/type coll) (:db.schema.collection/name coll))
        sources  (ds/q (assoc active-ref-q :in '[$ ?source])
                       db coll-eid)
        dests    (ds/q (assoc active-ref-q :in '[$ ?dest])
                       db coll-eid)
        refs     (distinct (concat sources dests))]
    (expand-eids db refs)))

(defn attr [db attr]
  (let [attr-eid (:db/id (ds/pull db [:db/id] (:db/ident attr)))
        refs     (ds/q (assoc ref-q :in '[$ ?source-attr])
                       db attr-eid)]
    (expand-eids db refs)))
