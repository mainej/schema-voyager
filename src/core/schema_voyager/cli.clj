(ns schema-voyager.cli
  "Tools for invoking Schema Voyager from the CLI. This is the main external
  interface into Schema Voyager.

  See the [installation and usage docs](/doc/installation-and-usage.md) for the
  mechanics of calling this code."
  (:require
   [clojure.pprint :as pprint]
   [schema-voyager.db.init :as db.init]
   [schema-voyager.db.persist :as db.persist]
   [schema-voyager.ingest.api :as ingest]
   [schema-voyager.template.standalone :as template.standalone]))

(defn- datomic-config [{:keys [datomic/db-name datomic/client-config datomic/exclusions datomic/infer]}]
  {:db-name       db-name
   :client-config client-config
   :exclusions    exclusions
   :infer         infer})

(defn- extract-datomic [params]
  (ingest/datomic (datomic-config params)))

(defn- extract-file [{:keys [file/name]}]
  (ingest/file name))

(defn- extract-fn [{:keys [fn/name fn/args]}]
  ((requiring-resolve name) (or args {})))

(defn- extract-static [{:keys [static/data]}]
  data)

(defn- extract-source [source]
  (cond
    (:datomic/db-name source) (extract-datomic source)
    (:file/name source)       (extract-file source)
    (:fn/name source)         (extract-fn source)
    (:static/data source)     (extract-static source)
    :else                     (throw (ex-info "Unrecognized source" {:source source}))))

(defn ingest-into-db
  "Create an in-memory DataScript DB from the `source`s. Does not persist the
  database. Useful primarily for exploring the database from a script.

  Read about specifying sources in the [sources docs](/doc/sources.md)."
  [{:keys [sources]}]
  (->> (mapcat extract-source sources)
       ingest/process
       db.init/into-db))

#_{:clj-kondo/ignore #{:clojure-lsp/unused-public-var}}
(defn ingest
  "Ingest schema into Schema Voyager as per [[ingest-into-db]], saving the
  resulting DataScript DB at `db-file` (by default
  `resources/schema_voyager_db.edn`).

  This is mostly used during development of Schema Voyager itself.

  Read about how to invoke [[ingest]] in the [advanced usage
  docs](/doc/advanced-exploration.md).

  NOTICE: If you experience errors using a Datomic source, see the
  [troubleshooting docs](/doc/troubleshooting.md)."
  {:arglists '([{:keys [db-file sources]}])}
  [{:keys [db-file] :as params}]
  (db.persist/save-db db-file (ingest-into-db params))
  (shutdown-agents))

#_{:clj-kondo/ignore #{:clojure-lsp/unused-public-var}}
(defn print-inferences
  "Print the supplemental attributes Schema Voyager infers from a Datomic source.

  Read about how to invoke [[print-inferences]] in the [inference
  docs](/doc/datomic-inference.md).

  Running inferences is expensive (again, see the [inference
  docs](/doc/datomic-inference.md)) so use this command to run the inferences
  once, copying the output to a file. In the future, use the file as a source,
  instead of re-running the inferences.

  Expects the argument to be a [Datomic source](/doc/sources.md#Datomic-source),
  that is, it should contain `:datomic/client-config` and `:datomic/db-name`.
  Specify what you would like to `:datomic/infer`, as explained in the
  [inference docs](/doc/datomic-inference.md)."
  {:arglists '([{:keys [datomic/client-config datomic/db-name datomic/infer]}])}
  [params]
  (pprint/pprint (ingest/datomic-inferences (datomic-config params)))
  (shutdown-agents))

#_{:clj-kondo/ignore #{:clojure-lsp/unused-public-var}}
(defn print-attributes
  "Print the attributes Schema Voyager is loading from a Datomic source.

  Expects the argument to be a [Datomic source](/doc/sources.md#Datomic-source),
  that is, it should contain `:datomic/client-config` and `:datomic/db-name`.

  Useful mostly for debugging."
  {:arglists '([{:keys [datomic/client-config datomic/db-name]}])}
  [params]
  (pprint/pprint (ingest/datomic-attributes (datomic-config params)))
  (shutdown-agents))

#_{:clj-kondo/ignore #{:clojure-lsp/unused-public-var}}
(defn standalone
  "Creates a standalone HTML page at `output-path`, after importing the `sources`
  as per [[ingest-into-db]].

  This is the most common CLI function for app developers to call.

  Read about how to invoke [[standalone]] in the [usage
  docs](/doc/installation-and-usage.md).

  By default, the `output-path` is `schema-voyager.html`."
  {:arglists '([{:keys [sources output-path]}])}
  [{:keys [output-path] :or {output-path "schema-voyager.html"} :as spec}]
  (template.standalone/fill-template output-path (ingest-into-db spec)))
