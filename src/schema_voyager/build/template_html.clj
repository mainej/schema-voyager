(ns schema-voyager.build.template-html
  "Builds the template file used by `schema-voyager.build.standalone-html/fill-template`.

  The dependency order is important here... we want to keep hiccup out of the
  dependencies. Therefore, the CLI depends on
  `schema-voyager.build.standalone-html`, which doesn't require hiccup. This
  file also depends on `schema-voyager.build.standalone-html`, but it does
  require hiccup, letting us use hiccup only when necessary."
  (:require
   [clojure.java.shell :refer (sh)]
   [hiccup.page :as hiccup.page]
   [schema-voyager.build.standalone-html :as standalone-html]
   [schema-voyager.build.db :as build.db]))

(defn optimized-js []
  ;; Not (shadow.cljs.devtools.api/release :app)
  ;; Because if -X:datomic is also used, you get errors
  ;;
  ;; Execution error (NoSuchMethodError) at com.google.javascript.jscomp.deps.ModuleLoader/createRootPaths (ModuleLoader.java:257).
  ;; com.google.common.collect.ImmutableSortedSet.toImmutableSortedSet(Ljava/util/Comparator;)Ljava/util/stream/Collector;
  ;;
  ;; See https://clojurians-log.clojureverse.org/shadow-cljs/2021-05-27
  ;; There is some sort of dependency conflict, but I couldn't figure out how to resolve it.
  ;;
  ;; NOTE: this may be fixed, as of the separation of this file from
  ;; `schema-voyager.build.standalone-html`. In theory, you shouldn't ever have
  ;; to use -X:datomic at the same time as using this file, so the error
  ;; shouldn't arise.
  (sh "npx" "shadow-cljs" "release" ":app"))

(defn optimized-css []
  (sh "bin/dev/css" "--minify"))

(defn standalone-html
  []
  (spit standalone-html/template-file
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
  (build.db/save-db standalone-html/db-template-placeholder)
  (optimized-js)
  (optimized-css)
  (standalone-html))
