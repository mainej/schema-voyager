(ns schema-voyager.ingest.datomic
  "Ingest schema data directly from a Datomic database.

  A running Datomic database is the authority on which attributes are installed,
  so many projects will want to ingest schema data from it.

  To use this namespace, `datomic.client.api` must be on your classpath."
  (:require [datomic.client.api :as d]
            [schema-voyager.ingest.core :as ingest]
            [clojure.set :as set]))

(defn- lift-ident [item k]
  (cond-> item
    (contains? item k) (update k :db/ident)))

(defn datomic-db
  "Get the current value of the Datomic db `db-name`, using a client described
  by `client-config`."
  [client-config db-name]
  (let [client (d/client client-config)
        conn   (d/connect client {:db-name db-name})]
    (d/db conn)))

(def datomic-coll-exclusions
  "These are collections installed by Datomic itself, as of release 480-8770.
  Typically, you don't want to document them, so they are excluded by default,
  along with all of their attributes. You can re-enable certain attributes or
  constants, and their associated collection, by passing
  `datomic-entity-inclusions` to [[ingest]]."
  #{#schema/agg :db.alter
    #schema/agg :db.attr
    #schema/agg :db.entity
    #schema/agg :db.excise
    #schema/agg :db.install
    #schema/agg :db
    #schema/agg :fressian
    #schema/enum :db.bootstrap
    #schema/enum :db.cardinality
    #schema/enum :db.part
    #schema/enum :db.type
    #schema/enum :db.unique
    #schema/enum :db})

(defn excluded-attr?
  "Individual attributes can be ignored by including their :db/ident in
  `entity-exclusions`. Entire collections worth of attributes can be ignored by
  including the collection in `coll-exclusions`. Attributes installed by Datomic
  itself are excluded unless they are part of `datomic-entity-inclusions`."
  [attr {:keys [entity-exclusions coll-exclusions datomic-entity-inclusions]
         :or   {entity-exclusions         #{}
                coll-exclusions           #{}
                datomic-entity-inclusions #{}}}]
  (let [coll (ingest/attribute-derive-part-of attr)]
    (or (contains? entity-exclusions (:db/ident attr))
        (some coll-exclusions coll)
        (and (some datomic-coll-exclusions coll)
             (not (contains? datomic-entity-inclusions (:db/ident attr)))))))

(defn ingest
  "Converts all attributes from a Datomic database `db` into the form that
  [[schema-voyager.ingest.core/process]] expects.

  Ignores attributes that are excluded per the `exclusions`. See
  [[excluded-attr?]]."
  ([db] (ingest db {}))
  ([db exclusions]
   (->> (d/q '[:find (pull ?e [*])
               :where [?e :db/ident]]
             db)
        (map first)
        (map #(-> %
                  (dissoc :db/id)
                  (lift-ident :db/valueType)
                  (lift-ident :db/cardinality)
                  (lift-ident :db/unique)))
        (remove #(excluded-attr? % exclusions)))))

(def ^:private reference-rules
  '[[(referred-attr-from-entity [?referred-e] ?referred-attr)
     (or
      ;; attribute
      (and
       [(missing? $ ?referred-e :db/ident)]
       [?referred-e ?referred-attr])
      ;; constant
      (and
       [?referred-e :db/ident]
       [(identity ?referred-e) ?referred-attr]))]])

(defn- references-from-attrs [exclusions attr-pairs]
  (->> attr-pairs
       (map (fn [[refers-attr referred-attr]]
              [(lift-ident refers-attr :db/valueType)
               (ingest/attribute-derive-collection (lift-ident referred-attr :db/valueType))]))
       distinct
       (remove (fn [[refers-attr _]]
                 (excluded-attr? refers-attr exclusions)))
       (group-by first)
       (map (fn [[refers-attr grouped]]
              [refers-attr (map second grouped)]))
       (map (fn [[refers-attr referred-colls]]
              (assoc refers-attr :db.schema/references (vec referred-colls))))))

(defn infer-plain-references
  "Infer references between `:db.type/ref` attributes by inspecting the things
  to which they refer.

  Ignores attributes that are not in-use and attributes that are excluded per
  the `exclusions`. See [[excluded-attr?]].

  Before using, see the warnings in the [inference
  docs](/doc/datomic-inference.md)."
  ([db] (infer-plain-references db {}))
  ([db exclusions]
   (->> (d/q '[:find (pull ?refers-attr [:db/ident :db/valueType]) (pull ?referred-attr [:db/ident :db/valueType])
               :in $ %
               :where
               [?refers-attr :db/valueType :db.type/ref]
               [_ ?refers-attr ?referred-e]
               (referred-attr-from-entity ?referred-e ?referred-attr)]
             db reference-rules)
        (references-from-attrs exclusions))))

(defn infer-homogeneous-tuple-references
  "Infer references from homogeneous tuple attributes by inspecting the things
  to which they refer.

  Ignores attributes that are not in-use and attributes that are excluded per
  the `exclusions`. See [[excluded-attr?]].

  NOTE: Makes a (large) assumption. Each item in the tuple could refer to
  different types of entities. This function assumes that the first item in the
  tuple *can* and *will* refer to all the interesting types. That means that if
  collection-a never appears in the first item of the tuple, this will fail to
  infer that collection-a is a reference. Of course, if your data is structured
  that way, perhaps you would be better suited by a heterogeneous tuple.

  Before using, see the warnings in the [inference
  docs](/doc/datomic-inference.md)."
  ([db] (infer-homogeneous-tuple-references db {}))
  ([db exclusions]
   (->> (d/q '[:find (pull ?refers-attr [:db/ident :db/valueType]) (pull ?referred-attr [:db/ident :db/valueType])
               :in $ %
               :where [?refers-attr :db/tupleType :db.type/ref]
               [_ ?refers-attr ?tuple]
               [(untuple ?tuple) [?referred-e]]
               (referred-attr-from-entity ?referred-e ?referred-attr)]
             db reference-rules)
        (references-from-attrs exclusions))))

(defn infer-heterogeneous-tuple-references
  "Infer references from heterogeneous tuple attributes by inspecting the things
  to which they refer.

  Ignores attributes that are not in-use and attributes that are excluded per
  the `exclusions`. See [[excluded-attr?]].

  Before using, see the warnings in the [inference
  docs](/doc/datomic-inference.md)."
  ([db] (infer-heterogeneous-tuple-references db {}))
  ([db exclusions]
   (->> (d/q '[:find (pull ?tuple-attr [:db/id :db/ident :db/valueType :db/tupleTypes])
               :where [?tuple-attr :db/tupleTypes]]
             db)
        (map first)
        (map #(lift-ident % :db/valueType))
        (remove #(excluded-attr? % exclusions))
        (keep (fn [refers-attr]
                (when-let [tuple-refs (->> refers-attr
                                           :db/tupleTypes
                                           (keep-indexed (fn [position kw]
                                                           (when (= kw :db.type/ref)
                                                             position)))
                                           (keep (fn [position]
                                                   (let [untuple-bindings (->> refers-attr
                                                                               :db/tupleTypes
                                                                               count
                                                                               range
                                                                               (map (fn [type-position]
                                                                                      (if (= type-position position)
                                                                                        '?referred-e
                                                                                        '_)))
                                                                               vec)
                                                         referred-colls   (->> (d/q {:find  '[(pull ?referred-attr [:db/ident :db/valueType])]
                                                                                     :in    '[$ % ?refers-attr]
                                                                                     :where ['[_ ?refers-attr ?tuple]
                                                                                             ['(untuple ?tuple) untuple-bindings]
                                                                                             '(referred-attr-from-entity ?referred-e ?referred-attr)] }
                                                                                    db reference-rules (:db/id refers-attr))
                                                                               (map first)
                                                                               (map #(lift-ident % :db/valueType))
                                                                               (map ingest/attribute-derive-collection)
                                                                               distinct)]
                                                     (when (seq referred-colls)
                                                       {:db.schema.tuple/position position
                                                        :db.schema/references     (vec referred-colls)}))))
                                           seq)]
                  {:db/ident                   (:db/ident refers-attr)
                   :db.schema/tuple-references (vec tuple-refs)}))))))

(defn infer-references
  "Infer references between ref and tuple attributes by inspecting the things
  to which they refer.

  Ignores attributes that are not in-use and attributes that are excluded per
  the `exclusions`. See [[excluded-attr?]].

  Before using, see the warnings in the [inference
  docs](/doc/datomic-inference.md)."
  ([db] (infer-references db {}))
  ([db exclusions]
   (concat (infer-plain-references db exclusions)
           (infer-homogeneous-tuple-references db exclusions)
           (infer-heterogeneous-tuple-references db exclusions))))

(defn infer-deprecations
  "Infer that attributes and constants that do not appear on any entities are
  deprecated.

  Ignores attributes that are excluded per the `exclusions`. See
  [[excluded-attr?]].

  Before using, see the warnings in the [inference
  docs](/doc/datomic-inference.md)."
  ([db] (infer-deprecations db {}))
  ([db exclusions]
   (let [defined (->> (d/q '[:find (pull ?attr [:db/ident :db/valueType])
                             :where
                             [?attr :db/ident]]
                           db)
                      (map first)
                      set)
         used    (->> (d/q '[:find (pull ?attr [:db/ident :db/valueType])
                             :where
                             [?attr :db/ident]
                             (or
                              ;; attribute
                              (and [?attr :db/valueType]
                                   [_ ?attr])
                              ;; constant
                              (and [(missing? $ ?attr :db/valueType)]
                                   [_ _ ?attr]))]
                           db)
                      (map first)
                      set)]
     (->> (set/difference defined used)
          (remove #(excluded-attr? % exclusions))
          (map (fn [attr]
                 {:db/ident              (:db/ident attr)
                  :db.schema/deprecated? true}))
          (sort-by :db/ident)))))

(defn infer
  "Infer deprecations and/or references from db usage.

  This is a unified interface for calling the other more specialized `infer-*`
  methods.

  Before using, see the warnings in the [inference
  docs](/doc/datomic-inference.md)."
  [db infer]
  (let [infer (set infer)]
    (vec
     (concat (when (some infer #{:all :deprecations})
               (infer-deprecations db))
             (when (some infer #{:all :references :plain-references})
               (infer-plain-references db))
             (when (some infer #{:all :references :tuple-references :homogeneous-tuple-references})
               (infer-homogeneous-tuple-references db))
             (when (some infer #{:all :references :tuple-references :heterogeneous-tuple-references})
               (infer-heterogeneous-tuple-references db))))))

(defn- db-from-source
  [{:keys [client-config db-name]}]
  (datomic-db client-config db-name))

#_{:clj-kondo/ignore #{:clojure-lsp/unused-public-var}}
(defn cli-ingest
  "A shorthand, used by the CLI, for connecting to a Datomic database, ingesting
  and making inferences all in one step. Essentially the same as running
  [[cli-attributes]] and [[cli-inferences]] at the same time.

  Before calling with the `infer` param, see the warnings in the [inference
  docs](/doc/datomic-inference.md)."
  {:arglists '([{:keys [client-config db-name infer exclusions]}])}
  [{:keys [exclusions] inferences :infer :as source}]
  (let [db (db-from-source source)]
    (concat (ingest db exclusions)
            (infer db inferences))) )

#_{:clj-kondo/ignore #{:clojure-lsp/unused-public-var}}
(defn cli-inferences
  "A shorthand, used by the CLI, for connecting to a Datomic database (with
  [[datomic-db]]) and inferring deprecations and/or references from db usage
  (with [[infer]]).

  Specify what you would like to `infer`, as explained in the [inference
  docs](/doc/datomic-inference.md), heeding the warnings there."
  {:arglists '([{:keys [client-config db-name infer]}])}
  [{inferences :infer :as source}]
  (infer (db-from-source source) inferences) )

#_{:clj-kondo/ignore #{:clojure-lsp/unused-public-var}}
(defn cli-attributes
  "A shorthand, used by the CLI, for connecting to a Datomic database (with
  [[datomic-db]]) and extracting installed attributes (with [[ingest]]).

  See [[excluded-attr?]] for information about how to use `exclusions`."
  {:arglists '([{:keys [client-config db-name exclusions]}])}
  [{:keys [exclusions] :as source}]
  (ingest (db-from-source source) exclusions) )
