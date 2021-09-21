(ns schema-voyager.cli
  (:require
   [clojure.pprint :as pprint]
   [schema-voyager.build.db :as build.db]
   [schema-voyager.build.standalone-html :as standalone-html]
   [schema-voyager.data :as data]
   [schema-voyager.ingest.core :as ingest]))

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
       data/process
       build.db/into-db))

(defn ingest
  "Ingest schema into Schema Voyager from one or more `source`s.

  Read about specifying sources in `doc/sources.md`.

  Read about how to invoke [[ingest]] in `doc/installation-and-usage.md`.

  NOTICE: If you experience errors using a Datomic source, see
  `doc/troubleshooting.md`. "
  [params]
  (build.db/save-db (:db-file params) (ingest-into-db params))
  (shutdown-agents))

(defn print-inferences
  "Show the inferences Schema Voyager is making. Expects `params` to be a
  Datomic source as defined by [[ingest]]. This can be a good starting point for
  transitioning to managing supplemental schema manually instead of re-running
  the expensive inferences."
  [params]
  (pprint/pprint (ingest/datomic-inferences (datomic-config params)))
  (shutdown-agents))

(defn print-attributes
  "Show the attributes Schema Voyager is loading. Expects `params` to be a
  Datomic source as defined by [[ingest]]. Useful mostly for debugging."
  [params]
  (pprint/pprint (ingest/datomic-attributes (datomic-config params)))
  (shutdown-agents))

(defn standalone
  "Creates a standalone HTML page at `output-path`, after importing the `sources`
  as per [[ingest]].

  By default, the generated HTML page is called `schema-voyager.html`."
  [{:keys [output-path] :or {output-path "schema-voyager.html"} :as spec}]
  (standalone-html/fill-template output-path (ingest-into-db spec)))
