(ns schema-voyager.ingest.datomic
  "Tools for extracting schema information directly from a Datomic database.

  This can be useful if your schema files are hard to collate, or when you want
  to know what _is_ installed in your database, as opposed to what _should_ be
  installed.

  To use this namespace, [[datomic.client.api]] must be on your classpath. This
  project provides an alias `:datomic` which will pull in a version of
  [[com.datomic/client-cloud]], but you may need your own version.

  ```bash
  clojure -A:ingest:datomic -m ingest.projects.my-project
  ```

  The most basic usage of this namespace would look like:

  ```clojure
  (let [db (ingest.datomic/datomic-db {:server-type :ion
                                       :region      \"us-east-1\"
                                       :system      \"my-system\"
                                       :endpoint    \"http://entry.my-system.us-east-1.datomic.net:8182/\"
                                       :proxy-port  8182}
                                      \"my-system-db\")]
    (-> (ingest.datomic/ingest db)
        data/process
        ingest/into-db
        export/save-db))
  ```

  However, assuming you don't store your supplemental data in Datomic, you may
  want to join the DB data with data from another source:

  ```clojure
  (-> (data/join (ingest.datomic/ingest db)
                 supplemental-data)
      data/process
      ingest/into-db
      export/save-db)
  ```"
  (:require [datomic.client.api :as d]
            [schema-voyager.data :as data]
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

(def base-coll-exclusions
  "These are collections installed by Datomic itself, as of release 480-8770.
  Typically, you don't want to document them, so they are excluded by default.
  You can control which collections are excluded, either more or fewer, by
  passing `coll-exclusions` to [[ingest]]."
  #{(data/aggregate :db.alter)
    (data/aggregate :db.attr)
    (data/aggregate :db.entity)
    (data/aggregate :db.excise)
    (data/aggregate :db.install)
    (data/aggregate :db)
    (data/aggregate :fressian)
    (data/enum :db.bootstrap)
    (data/enum :db.cardinality)
    (data/enum :db.part)
    (data/enum :db.type)
    (data/enum :db.unique)
    (data/enum :db)})

(defn excluded-attr?
  "Individual attributes can be ignored by including their :db/ident in
  `entity-exclusions`. Entire collections worth of attributes can be ignored by
  including the collection in `coll-exclusions`."
  [attr {:keys [entity-exclusions coll-exclusions]
         :or   {entity-exclusions #{}
                coll-exclusions   base-coll-exclusions}}]
  (or (contains? entity-exclusions (:db/ident attr))
      (some coll-exclusions (data/attribute-derive-part-of attr))))

(defn ingest
  "Converts all attributes from a Datomic database `db` into the form that
  [[schema-voyager.data/process]] expects.

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
               (data/attribute-derive-collection (lift-ident referred-attr :db/valueType))]))
       distinct
       (remove (fn [[refers-attr _]]
                 (excluded-attr? refers-attr exclusions)))
       (group-by first)
       (map (fn [[refers-attr grouped]]
              [refers-attr (map second grouped)]))
       (map (fn [[refers-attr referred-colls]]
              (assoc refers-attr :db.schema/references referred-colls)))))

(defn infer-plain-references
  "Infer references between `:db.type/ref` attributes by inspecting the things
  to which they refer.

  Ignores attributes that are not in-use and attributes that are excluded per
  the `exclusions`. See [[excluded-attr?]].

  WARNING: This has not been tested on large databases, where it may have
  performance impacts. Use at your own risk.

  This may help kick start your supplemental schema, but consider running it
  once, caching the results in a file, then maintaing it by hand."

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

  This may help kick start your supplemental schema, but consider running it
  once, caching the results in a file, then maintaing it by hand."
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

  WARNING: This has not been tested on large databases, where it may have
  performance impacts. This is even more true than [[infer-references]] because
  it executes one query per tuple attribute per `:db.type/ref`. Use at your own
  risk.

  This may help kick start your supplemental schema, but consider running it
  once, caching the results in a file, then maintaing it by hand."
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
                                                                               (map data/attribute-derive-collection)
                                                                               distinct)]
                                                     (when (seq referred-colls)
                                                       {:db.schema.tuple/position position
                                                        :db.schema/references     referred-colls}))))
                                           seq)]
                  {:db/ident                   (:db/ident refers-attr)
                   :db.schema/tuple-references tuple-refs}))))))

(defn infer-references
  "Infer references between ref and tuple attributes by inspecting the things
  to which they refer.

  Ignores attributes that are not in-use and attributes that are excluded per
  the `exclusions`. See [[excluded-attr?]].

  Be sure to see WARNINGS on [[infer-plain-references]],
  [[infer-homogeneous-tuple-references]] and
  [[infer-heterogeneous-tuple-references]].

  This may help kick start your supplemental schema, but consider running it
  once, caching the results in a file, then maintaing it by hand."
  ([db] (infer-references db {}))
  ([db exclusions]
   (data/join (infer-plain-references db exclusions)
              (infer-homogeneous-tuple-references db exclusions)
              (infer-heterogeneous-tuple-references db exclusions))))

(defn infer-deprecations
  "Infer deprecated attributes and constants, based on whether they are used.

  Ignores attributes that are excluded per the `exclusions`. See
  [[excluded-attr?]].

  WARNING: This has not been tested on large databases, where it may have
  performance impacts. Use at your own risk.

  This may help kick start your supplemental schema, but consider running it
  once, caching the results in a file, then maintaing it by hand."
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
