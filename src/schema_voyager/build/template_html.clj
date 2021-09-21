(ns schema-voyager.build.template-html
  (:require
   [clojure.java.io :as io]
   [clojure.java.shell :refer (sh)]
   [clojure.string :as string]
   [hiccup.page :as hiccup.page]
   [schema-voyager.build.db :as build.db]))

(def db-template-placeholder 'SCHEMA_VOYAGER_DB_PLACEHOLDER)

(def template-dir (io/resource "standalone_template"))
(def template-file (io/file template-dir "standalone.html"))

(defn optimized-js []
  ;; Not (shadow.cljs.devtools.api/release :app)
  ;; Because if -X:datomic is also used, you get errors
  ;;
  ;; Execution error (NoSuchMethodError) at com.google.javascript.jscomp.deps.ModuleLoader/createRootPaths (ModuleLoader.java:257).
  ;; com.google.common.collect.ImmutableSortedSet.toImmutableSortedSet(Ljava/util/Comparator;)Ljava/util/stream/Collector;
  ;;
  ;; See https://clojurians-log.clojureverse.org/shadow-cljs/2021-05-27
  ;; There is some sort of dependency conflict, but I couldn't figure out how to resolve it.
  (sh "npx" "shadow-cljs" "release" ":app"))

(defn optimized-css []
  (sh "yarn" "run" "--prod" "compile-css"))

(defn standalone-html
  []
  (spit template-file
        ;; IMPORTANT: keep this in sync with index.html
        (hiccup.page/html5
         [:head
          [:meta {:charset "UTF-8"}]
          [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
          [:title "Schema Voyager"]
          [:style (slurp "target/styles.css")]
          [:script {:src "https://unpkg.com/@hpcc-js/wasm/dist/index.min.js"}]
          [:script "var hpccWasm = window[\"@hpcc-js/wasm\"];"]]
         [:body
          [:div#app]
          [:script {:type "text/javascript"} (slurp "target/main.js")]])))

(defn make-template
  "Creates a template file with a placeholder string instead of a DataScript DB.
  It's used by [[fill-template]], which takes an ingested DB, then substitutes
  that into the template file."
  ;; This is kind of a hack. It works in contrast to the old approach where the
  ;; ingested DB was saved and then compiled directly into the JS.
  ;;
  ;; This approach of pre-compiling the template is used for two reasons. First,
  ;; it makes standalone HTML creation faster, because you only have to ingest
  ;; the data, instead of also re-compiling the JS. Second, we can distribute
  ;; the template file as a resource, which makes this library usable from other
  ;; projects. Before they couldn't compile the app (shadow-cljs.edn wasn't
  ;; available, among other things). Now they just substitute their data into
  ;; the template.
  [_]
  (build.db/save-db db-template-placeholder)
  (optimized-js)
  (optimized-css)
  (standalone-html))

(defn fill-template
  "Creates a standalone HTML page at `output-path`, by subsitituting the `db`
  into the template file."
  [output-path db]
  (let [target-file (io/file "." output-path)
        contents    (slurp template-file)
        replaced    (string/replace contents
                                    (pr-str (str db-template-placeholder))
                                    (pr-str (pr-str db)))]
    (when-not (.exists (.getParentFile target-file))
      (.mkdirs (.getParentFile target-file)))
    (spit target-file replaced :append false)))
