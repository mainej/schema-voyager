(ns schema-voyager.ingest.db
  "Tools for extracting schema information directly from a Datomic database.

  This can be useful if your schema files are hard to collate, or when you want
  to know what _is_ installed in your database, as opposed to what _should_ be
  installed.

  To use this namespace, [[datomic.client.api]] must be on your classpath. This
  project provides an alias `:datomic` which will pull in a version of
  [[com.datomic/client-cloud]], but you may need your own version.

  ```bash
  clj -A:ingest:datomic -m ingest.projects.my-project
  ```

  The most basic usage of this namespace would look like:

  ```clojure
  (let [db (ingest.db/datomic-db {:server-type :ion
                                  :region      \"us-east-1\"
                                  :system      \"my-system\"
                                  :endpoint    \"http://entry.my-system.us-east-1.datomic.net:8182/\"
                                  :proxy-port  8182}
                                 \"my-system-db\")]
    (-> (ingest.db/ingest db)
        data/process
        ingest/into-db
        export/save-db))
  ```

  However, assuming you don't store your supplemental data in Datomic, you may
  want to join the DB data with data from another source:

  ```clojure
  (-> (data/join (ingest.db/ingest db)
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
  You can control which collections are excluded, either more or less, by
  passing `coll-exclusions` to [[ingest]]."
  #{#:db.schema.collection{:type :aggregate, :name :db.alter}
    #:db.schema.collection{:type :aggregate, :name :db.attr}
    #:db.schema.collection{:type :aggregate, :name :db.entity}
    #:db.schema.collection{:type :aggregate, :name :db.excise}
    #:db.schema.collection{:type :aggregate, :name :db.install}
    #:db.schema.collection{:type :aggregate, :name :db}
    #:db.schema.collection{:type :aggregate, :name :fressian}
    #:db.schema.collection{:type :enum, :name :db.bootstrap}
    #:db.schema.collection{:type :enum, :name :db.cardinality}
    #:db.schema.collection{:type :enum, :name :db.part}
    #:db.schema.collection{:type :enum, :name :db.type}
    #:db.schema.collection{:type :enum, :name :db.unique}
    #:db.schema.collection{:type :enum, :name :db}})

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

(defn infer-references
  "Infer references between attributes by inspecting the things to which they
  refer.

  Ignores attributes that are not in-use. Does not (currently) infer
  either homogeneous or heterogeneous tuple references.

  Ignores attributes that are excluded per the `exclusions`. See
  [[excluded-attr?]].

  WARNING: This has not been tested on large databases, where it may have
  performance impacts. Use at your own risk.

  This may help kick start your supplemental schema, but consider running it
  once, caching the results in a file, then maintaing it by hand."

  ([db] (infer-references db {}))
  ([db exclusions]
   (->> (d/q '[:find (pull ?refers-attr [:db/ident :db/valueType]) (pull ?referred-attr [:db/ident :db/valueType])
               :where
               [?refers-attr :db/valueType :db.type/ref]
               [_ ?refers-attr ?referred-e]
               (or
                ;; attribute
                (and
                 [(missing? $ ?referred-e :db/ident)]
                 [?referred-e ?referred-attr])
                ;; constant
                (and
                 [?referred-e :db/ident]
                 [(identity ?referred-e) ?referred-attr]))]
             db)
        (keep (fn [[refers-attr referred-attr]]
                (let [refers-attr (-> refers-attr
                                      (lift-ident :db/valueType))]
                  (when-not (excluded-attr? refers-attr exclusions)
                    (let [referred-coll (-> referred-attr
                                            (lift-ident :db/valueType)
                                            data/attribute-derive-collection)]
                      [refers-attr referred-coll])))))
        distinct
        (group-by first)
        (map (fn [[refers-attr grouped]]
               [refers-attr (map second grouped)]))
        (map (fn [[refers-attr referred-colls]]
               (assoc refers-attr :db.schema/references referred-colls))))))

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
