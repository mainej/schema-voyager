(ns schema-voyager.cli
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
  database. Useful primarily for exploring the database from a script."
  [{:keys [sources]}]
  (->> (mapcat extract-source sources)
       ingest/process
       db.init/into-db))

#_{:clj-kondo/ignore #{:clojure-lsp/unused-public-var}}
(defn ^{:arglists '([{:keys [db-file sources]}])} ingest
  "Ingest schema into Schema Voyager from one or more `source`s, saving the
  resulting DataScript db at `db-file` (resources/schema_voyager_db.edn by
  default).

  Read about specifying sources in `/doc/sources.md`.

  Read about how to invoke [[ingest]] in `/doc/installation-and-usage.md`.

  NOTICE: If you experience errors using a Datomic source, see
  `/doc/troubleshooting.md`. "
  [{:keys [db-file] :as params}]
  (db.persist/save-db db-file (ingest-into-db params))
  (shutdown-agents))

#_{:clj-kondo/ignore #{:clojure-lsp/unused-public-var}}
(defn ^{:arglists '([{:keys [client-config db-name infer] :as params}])}
  print-inferences
  "Print the supplemental attributes Schema Voyager infers from a Datomic source.

  Running inferences is expensive (see `/doc/datomic-inference.md`) so use this
  command to run the inferences once, copying the output to a file. In the
  future, use the file as a source, instead of re-running the inferences.

  Expects `params` to be a Datomic source as defined by `/doc/sources.md`, that
  is, `params` should contain `client-config` and `db-name`. Specify what you would
  like to `infer`, as documented in `/doc/datomic-inference.md`"
  [params]
  (pprint/pprint (ingest/datomic-inferences (datomic-config params)))
  (shutdown-agents))

#_{:clj-kondo/ignore #{:clojure-lsp/unused-public-var}}
(defn ^{:arglists '([{:keys [client-config db-name] :as params}])} print-attributes
  "Print the attributes Schema Voyager is loading from a Datomic source.

  Expects `params` to be a Datomic source as defined by `/doc/sources.md`, that
  is, `params` should contain `client-config` and `db-name`.

  Useful mostly for debugging."
  [params]
  (pprint/pprint (ingest/datomic-attributes (datomic-config params)))
  (shutdown-agents))

#_{:clj-kondo/ignore #{:clojure-lsp/unused-public-var}}
(defn ^{:arglists '([{:keys [sources output-path]}])} standalone
  "Creates a standalone HTML page at `output-path`, after importing the `sources`
  as per [[ingest-into-db]].

  By default, the `output-path` is `schema-voyager.html`."
  [{:keys [output-path] :or {output-path "schema-voyager.html"} :as spec}]
  (template.standalone/fill-template output-path (ingest-into-db spec)))
