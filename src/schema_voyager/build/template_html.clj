(ns schema-voyager.build.template-html
  "Builds the template file used by `schema-voyager.build.standalone-html/fill-template`.

  The dependency order is important here... we want to keep hiccup out of the
  dependencies. Therefore, the CLI depends on
  `schema-voyager.build.standalone-html`, which doesn't require hiccup. This
  file also depends on `schema-voyager.build.standalone-html`, but it does
  require hiccup, letting us use hiccup only when necessary."
  (:require
   [clojure.tools.build.api :as b]
   [hiccup.page :as hiccup.page]
   [schema-voyager.build.db :as build.db]
   [schema-voyager.build.standalone-html :as standalone-html]))

(defn- die
  ([code message & args]
   (die code (apply format message args)))
  ([code message]
   (binding [*out* *err*]
     (println message))
   (System/exit code)))

(defn create-placeholder-db []
  (println "\nCreating placeholder DB...")
  (build.db/save-db standalone-html/db-template-placeholder))

(defn optimized-js []
  ;; Shelling out doesn't seem to be any slower than
  ;; (shadow.cljs.devtools.api/release :app), so better to keep shadow-cljs out
  ;; of the deps while making the template.
  (println "\nCompiling CLJS...")
  (when-not (zero? (:exit (b/process {:command-args ["npx" "shadow-cljs" "release" ":app"]})))
    (die 1 "Couldn't compile CLJS")))

(defn optimized-css []
  (println "\nCompiling CSS...")
  (when-not (zero? (:exit (b/process {:command-args ["bin/dev/css" "--minify"]})))
    (die 2 "Couldn't compile CSS")))

(defn standalone-html
  []
  (println "\nCombining HTML, CSS and JS into template file...")
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

#_{:clj-kondo/ignore #{:clojure-lsp/unused-public-var}}
(defn make-template
  "Creates a template file with a placeholder string instead of a DataScript DB.
  It's used by `schema-voyager.build.standalone-html/fill-template`, which takes
  an ingested DB, then substitutes that into the template file."
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
  (create-placeholder-db)
  (optimized-js)
  (optimized-css)
  (standalone-html)
  (println "\nDone"))
