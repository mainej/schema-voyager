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
            [schema-voyager.data :as data]))

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

(defn ingest
  "Converts all attributes from a Datomic database `db` into the form that
  [[schema-voyager.data/process]] expects.

  Individual attributes can be ignored by including their :db/ident in
  `entity-exclusions`. Entire collections worth of attributes can be ignored by
  including the collection in `coll-exclusions`."
  ([db] (ingest db {}))
  ([db {:keys [entity-exclusions coll-exclusions]
        :or   {entity-exclusions #{}
               coll-exclusions   base-coll-exclusions}}]
   (->> (d/q '[:find (pull ?e [*])
               :where [?e :db/ident]]
             db)
        (map first)
        (map #(-> %
                  (dissoc :db/id)
                  (lift-ident :db/valueType)
                  (lift-ident :db/cardinality)
                  (lift-ident :db/unique)))
        (remove (fn [e]
                  (contains? entity-exclusions (:db/ident e))))
        (remove (fn [e]
                  (some coll-exclusions (data/entity-derive-part-of e)))))))
