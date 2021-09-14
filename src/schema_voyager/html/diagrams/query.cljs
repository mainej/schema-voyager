(ns schema-voyager.html.diagrams.query
  "Extract edges from the graph of collections which are connected by reference
  type attributes.

  Returns tuples of [source-collection source-attr target-collection]."
  (:require [clojure.walk :as walk]
            [datascript.core :as ds]))

(def ^:private edge-q
  {:find  '[?source ?source-attr ?target]
   :where ['[?source-attr :db.schema/part-of ?source]
           '(or-join [?source-attr ?target]
                     [?source-attr :db.schema/references ?target]
                     (and
                      [?source-attr :db.schema/tuple-references ?target-tuple-ref]
                      [?target-tuple-ref :db.schema/references ?target]))]})

(def ^:private active-edge-q
  (update edge-q :where conj '(not [?source-attr :db.schema/deprecated? true])))

(defn- expand-edge-eids [db edges]
  (let [eids            (distinct (mapcat identity edges))
        entities        (ds/pull-many db '[*] eids)
        entities-by-eid (zipmap (map :db/id entities)
                                entities)]
    (walk/postwalk-replace entities-by-eid edges)))

(defn colls-edges
  "All the edges in the whole db."
  [db]
  (let [edges (ds/q active-edge-q db)]
    (expand-edge-eids db edges)))

(defn coll-edges
  "All the edges either into or out of this collection."
  [db coll]
  (let [coll-eid (ds/q '[:find ?coll .
                         :in $ ?collection-type ?collection-name
                         :where
                         [?coll :db.schema.collection/type ?collection-type]
                         [?coll :db.schema.collection/name ?collection-name]
                         [?coll :db.schema.pseudo/type :collection]]
                       db (:db.schema.collection/type coll) (:db.schema.collection/name coll))
        sources  (ds/q (assoc active-edge-q :in '[$ ?source])
                       db coll-eid)
        targets  (ds/q (assoc active-edge-q :in '[$ ?target])
                       db coll-eid)
        edges    (distinct (concat sources targets))]
    (expand-edge-eids db edges)))

(defn attr-edges
  "All the edges that this attribute joins. Can be many for tuple-references."
  [db attr]
  (let [attr-eid (:db/id (ds/pull db [:db/id] (:db/ident attr)))
        edges    (ds/q (assoc edge-q :in '[$ ?source-attr])
                       db attr-eid)]
    (expand-edge-eids db edges)))
