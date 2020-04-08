(ns ingest.datomic
  "A CLI for ingesting from a Datomic DB.

  This is useful to get a 'quick view' of an unfamiliar database, one that
  hasn't yet been annotated with supplemental schema. However, in the long run
  you should use the output from `schema.voyager.datomic/infer-references` and
  `schema.voyager.datomic/infer-deprecations` as a starting point for your own
  supplemental schema.

  WARNING: This has not been tested on large databases, where it may have
  performance impacts. Use at your own risk.

  ```
  clj -A:ingest:datomic -m ingest.datomic <db-name> <datomic-client-config-edn>
  ```"
  (:require [schema-voyager.ingest.datomic :as ingest.datomic]
            [schema-voyager.data :as data]
            [schema-voyager.ingest.core :as ingest]
            [schema-voyager.export :as export]
            [clojure.edn :as edn]
            [clojure.string :as string]
            [clojure.tools.cli :as cli]))

(def valid-infer-args
  ["all"
     "deprecations"
     "references"
       "plain-references"
       "tuple-references"
         "homogeneous-tuple-references"
         "heterogeneous-tuple-references"])

(defn- fmt-desc [lines]
  (string/join \newline lines))

(def ^:private cli-options
  [["-h" "--help"]
   ["-i" "--infer INFERENCE" (fmt-desc ["Which inferences to make. May be specified multiple times."
                                        ""
                                        (str
                                         "\tWARNING: Inferences are made by querying your database for how attributes are used. "
                                         "These queries can be large and slow. "
                                         "Avoid running them on a critical query group. "
                                         "Find more guidance about how to annotate your schema without running inference in the README and in `schema-voyager.ingest.datomic`.")
                                        ""
                                        "\tValid values for INFERENCE:"
                                        ""
                                        "\tall - make all inferences"
                                        "\t\tdeprecations - infer deprecations based on attributes that are defined but not currently used"
                                        "\t\treferences - infer references for attributes that are :db.type/ref or :db.type/tuple, based on the entities to which they refer"
                                        "\t\t\tplain-references - infer references for attributes that are :db.type/ref"
                                        "\t\t\ttuple-references - infer references for attributes that are :db.type/tuple"
                                        "\t\t\t\thomogeneous-tuple-references - infer references for attributes that have :db/tupleType"
                                        "\t\t\t\theterogeneous-tuple-references - infer references for attributes that have :db/tupleTypes"])
    :default #{}
    :assoc-fn (fn [m k v] (update m k conj v))
    :validate [(set valid-infer-args) (str "Must be one of " (string/join ", " valid-infer-args))]]])

(defn- usage [options-summary]
  (fmt-desc
   ["Ingest schema into Schema Voyager from a running Datomic database."
    ""
    "Usage: clj -A:ingest:datomic -m ingest.datomic [options] db-name datomic-client-config-edn"
    ""
    "\tdb-name: A string naming the DB to which you want to connect."
    ""
    "\tdatomic-client-config-edn: An EDN string with the connection parameters for the Datomic client."
    ""
    "Options:"
    options-summary
    ""]))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (fmt-desc errors)))

(defn validate-args
  "Validate command line arguments. Either return a map indicating the program
  should exit (with a error message, and optional ok status), or a map
  indicating the action the program should take and the options provided."
  [args]
  (let [{:keys [options arguments errors summary]} (cli/parse-opts args cli-options)]
    (cond
      (:help options) ; help => exit OK with usage summary
      {:exit-message (usage summary) :ok? true}

      errors ; errors => exit with description of errors
      {:exit-message (error-msg errors)}

      ;; custom validation on arguments
      (= 2 (count arguments))
      (assoc options
             :db-name (first arguments)
             :client-config (edn/read-string (second arguments)))

      :else ; failed custom validation => exit with usage summary
      {:exit-message (usage summary)})))

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn -main [& args]
  (let [{:keys [db-name client-config infer exit-message ok?]} (validate-args args)]
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (let [db (ingest.datomic/datomic-db client-config db-name)]
        (->> (data/join (ingest.datomic/ingest db)
                        (when (some infer #{"all" "deprecations"})
                          (ingest.datomic/infer-deprecations db))
                        (when (some infer #{"all" "references" "plain-references"})
                          (ingest.datomic/infer-plain-references db))
                        (when (some infer #{"all" "references" "tuple-references" "homogeneous-tuple-references"})
                          (ingest.datomic/infer-homogeneous-tuple-references db))
                        (when (some infer #{"all" "references" "tuple-references" "heterogeneous-tuple-references"})
                          (ingest.datomic/infer-heterogeneous-tuple-references db)))
             data/process
             ingest/into-db
             export/save-db)))))
