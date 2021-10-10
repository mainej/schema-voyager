(ns schema-voyager.db.query
  "A facade for querying a DataScript DB.

  Contains the queries used by the web UI to render each of the pages and
  diagrams. Also usable from a REPL (and tests) to understand what data is being
  pulled out of the DataScript db."
  (:require [clojure.walk :as walk]
            [datascript.core :as ds]))

;; Helper Queries

(def ^:private attr-link-pull
  [:db/ident
   :db.schema/deprecated?
   {:db.schema/part-of ['*]}])

(defn- promote-attrs
  "There are a few places we need attributes (to generate links to them) but we
  actually only have keywords, not refs to attributes.

  This converts the idents to real attributes, maintaing order."
  [entity db prop]
  (if (seq (prop entity))
    (update entity prop (fn [idents]
                          ;; not pull-many to maintain order
                          (map #(ds/pull db attr-link-pull %)
                               idents)))
    entity))

(defn- enrich-value-type
  "Substitute `:db.type/ref` with references, when we have them."
  [value-type references]
  (if (and (= :db.type/ref value-type) (seq references))
    references
    value-type))

(defn- enrich-attr-type
  "Classify tuples, and when we have supplemental refs, subsitute them in for
  the `:db.type/ref` keyword in `:db/valueType`, `:db/tupleType` and
  `:db/tupleTypes`."
  [{:keys [db/valueType db/tupleAttrs db/tupleType db/tupleTypes db.schema/references db.schema/tuple-references] :as attr}]
  (cond
    (nil? valueType)
    #_=> attr
    (not= :db.type/tuple valueType)
    #_=> (-> attr
             (update :db/valueType enrich-value-type references)
             (dissoc :db.schema/references))
    :else
    #_=> (cond
           tupleAttrs (-> attr
                          (assoc :db/valueType :db.type/tuple.composite))
           tupleType  (-> attr
                          (assoc :db/valueType :db.type/tuple.homogeneous)
                          (update :db/tupleType enrich-value-type references)
                          (dissoc :db.schema/references))
           tupleTypes (let [refs-by-position (zipmap (map :db.schema.tuple/position tuple-references)
                                                     (map :db.schema/references tuple-references))]
                        (-> attr
                            (assoc :db/valueType :db.type/tuple.heterogeneous)
                            (update :db/tupleTypes (fn [value-types]
                                                     (map-indexed (fn [position value-type]
                                                                    (enrich-value-type value-type (get refs-by-position position)))
                                                                  value-types)))
                            (dissoc :db.schema/tuple-references))))))

;;;; Attribute page Queries

(def ^:private attr-pull
  ['*
   {:db.schema/part-of                             ['*]
    :db.schema/references                          ['*]
    :db.schema/tuple-references                    ['*
                                                    {:db.schema/references ['*]}]
    :db.schema/see-also                            attr-link-pull
    [:db.schema/_see-also :as :db.schema/noted-by] attr-link-pull}])

(defn attribute-by-ident
  "Pull data for an entity page. Relevant for both constants and attributes."
  [db ident]
  (-> db
      (ds/pull attr-pull [:db/ident ident])
      (promote-attrs db :db/tupleAttrs)
      (enrich-attr-type)))

;;;; Collection page Queries

(defn- collection-eid-by-type-and-name [db collection-type collection-name]
  (ds/q '[:find ?collection .
          :in $ ?collection-type ?collection-name
          :where
          [?collection :db.schema.collection/type ?collection-type]
          [?collection :db.schema.collection/name ?collection-name]
          [?collection :db.schema.pseudo/type :collection]]
        db collection-type collection-name))

(def ^:private collection-pull
  ['*
   {[:db.schema/_part-of :as :db.schema.collection/attributes]
    ['*
     {:db.schema/references       ['*]
      :db.schema/tuple-references ['*
                                   {:db.schema/references ['*]}]}]

    [:db.schema/_references :as :db.schema.collection/referenced-by-attrs]
    ;; could actually be an attr or a tuple that references the collection
    (into #_attr attr-link-pull
          ;; Reverse lookups always come back as vectors. I think it's true to
          ;; say that a db.schema/tuple-references belong to one and only one
          ;; attribute. If that's so, a better name for this attribute would be
          ;; :db.schema.tuple/attr, presuming it would be updated by a `first`
          ;; after being fetched. But since I'm not 100% sure it's singular,
          ;; better to treat it as a vector of attributes. See also
          ;; `flatten-tuple-attrs`.
          #_tuple [{[:db.schema/_tuple-references :as :db.schema.tuple/attrs]
                    attr-link-pull}])}])

(defn- flatten-tuple-attrs [attrs-or-tuples]
  (mapcat (fn [attr-or-tuple]
            (or (:db.schema.tuple/attrs attr-or-tuple)
                [attr-or-tuple]))
          attrs-or-tuples))

(defn- attribute-comparable
  "Helper for sorting attributes. Returns items in this order:
  * Unique attributes
  * Deprecated unique attributes (rare)
  * Regular attributes
  * Deprecated attributes

  Further sorts alphabetically within each group."
  [{:keys [db.schema/deprecated? db/unique db/ident]}]
  [(not= :db.unique/identity unique) deprecated? ident])

(defn collection-by-type-and-name
  "Pull data for a collection page, either an aggregate or an enum."
  [db collection-type collection-name]
  (-> (ds/pull db collection-pull (collection-eid-by-type-and-name db
                                                                   collection-type
                                                                   collection-name))
      (update :db.schema.collection/referenced-by-attrs flatten-tuple-attrs)
      (update :db.schema.collection/referenced-by-attrs #(sort-by :db/ident %))
      (update :db.schema.collection/attributes (fn [attributes]
                                                 (->> attributes
                                                      (map (fn [attribute]
                                                             (-> attribute
                                                                 (promote-attrs db :db/tupleAttrs)
                                                                 (enrich-attr-type))))
                                                      (sort-by attribute-comparable))))))

;;;; Spec page Queries

(defn entity-spec-by-ident
  "Pull data for an entity spec page."
  [db ident]
  (-> (ds/pull db ['*] [:db/ident ident])
      (promote-attrs db :db.entity/attrs)
      (update :db.entity/preds
              (fn [pred-or-preds]
                (when pred-or-preds
                  (if (sequential? pred-or-preds)
                    pred-or-preds
                    [pred-or-preds]))))))

;;;; Homepage Queries

(defn collections-by-type
  "Pull collections for the homepage. The `collection-type` specifies whether you
  retrieve `:enum`s or `:aggregate`s."
  [db collection-type]
  (->> (ds/q '[:find [?coll ...]
               :in $ ?collection-type
               :where
               [?coll :db.schema.collection/type ?collection-type]
               [?coll :db.schema.pseudo/type :collection]]
             db collection-type)
       (ds/pull-many db '[*])
       (sort-by :db.schema.collection/name)))

(defn entity-specs
  "Pull entity specs for thte homepage."
  [db]
  (->> (ds/q '[:find [?spec ...]
               :where [?spec :db.schema.pseudo/type :entity-spec]]
             db)
       (ds/pull-many db '[:db/id :db/ident])
       (sort-by :db/ident)))

;;;; Diagram Queries

;; Extract edges from the graph of collections which are connected by reference
;; type attributes.

;; Returns tuples of [source-collection source-attr target-collection].

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
  (let [coll-eid (collection-eid-by-type-and-name db
                                                  (:db.schema.collection/type coll)
                                                  (:db.schema.collection/name coll))
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
