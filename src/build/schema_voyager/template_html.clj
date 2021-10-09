(ns schema-voyager.template-html
  "Builds the template file used by `schema-voyager.build.standalone-html/fill-template`.

  The dependency order is important here... we want to keep hiccup out of the
  dependencies. Therefore, the CLI depends on
  `schema-voyager.build.standalone-html`, which doesn't require hiccup. This
  file also depends on `schema-voyager.build.standalone-html`, but it does
  require hiccup, letting us use hiccup only when necessary."
  (:require
   [clojure.java.io :as io]
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
  (spit (io/file "resources" standalone-html/template-file-name)
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
  ;; OK, what's going on here? Why does this template exist?
  ;;
  ;; There are two modes to using Schema Voyager.
  ;;
  ;; When you're hacking on Schema Voyager, you use shadow-cljs to compile the
  ;; CLJS and launch a server. Of course, changes to the .cljs files are shipped
  ;; to the browser automatically. But, changes to the DataScript DB are too,
  ;; through the magic of `shadow.resource/inline`. Thus, when you re-run
  ;; `ingest`, the DB file is updated and then the web page updates. This is
  ;; great for live interaction with Schema Voyager code--you get quick feedback
  ;; on changes both to the CLJS rendering and to the ingestion process. But it
  ;; doesn't work so well with the other mode of using Schema Voyager.
  ;;
  ;; As an application author, you don't care about dynamically updating CLJS.
  ;; You also don't want a DB file, but instead want a single HTML file. That's
  ;; where this template comes in. We put a placeholder string into the file
  ;; that usually holds the DB, compile that into the JS (in a release build,
  ;; `shadow.resource/inline` inlines whatever is in the DB file *at that
  ;; moment*), and package everything up as an HTML file. Then the application
  ;; author only has to ingest their schema data and substitute it in place of
  ;; the placeholder. That's fast and means that we don't have to try to compile
  ;; Schema Voyager's CLJS from the application's codebase.
  [params]
  (create-placeholder-db)
  (optimized-js)
  (optimized-css)
  (standalone-html)
  params)
