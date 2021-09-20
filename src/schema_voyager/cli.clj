(ns schema-voyager.cli
  (:require [schema-voyager.data :as data]
            [schema-voyager.export :as export]
            [schema-voyager.ingest.core :as ingest]
            [clojure.pprint :as pprint]))

(def ^:private default-db-path "resources/schema_voyager_db.edn")

(defn save-db
  ([db] (save-db default-db-path db))
  ([file-path db]
   (spit (or file-path default-db-path) (pr-str db))))

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
  "Create an in-memory Datascript DB from the `source`s. Does not persist the
  database. Useful primarily for exploring the database from a script."
  [{:keys [sources]}]
  (->> (mapcat extract-source sources)
       data/process
       ingest/into-db))

(defn ingest
  "Ingest schema into Schema Voyager from one or more `source`s.

  Read about specifying sources in `doc/sources.md`.

  Read about how to invoke [[ingest]] in `doc/installation.md`.

  NOTICE: If you experience errors using a Datomic source, see
  `doc/troubleshooting.md`. "
  [params]
  (save-db (:db-file params) (ingest-into-db params))
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
