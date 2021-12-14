(ns schema-voyager.template-html
  "Builds the template file used by `schema-voyager.template.standalone/fill-template`.

  The dependency order is important here... we want to keep hiccup out of the
  dependencies. Therefore, the CLI depends on
  `schema-voyager.template.standalone` which depends on
  `schema-voyager.template.config`, neither of which requires hiccup. This file
  also depends on `schema-voyager.template.config`, but it does require hiccup,
  letting us use hiccup only when necessary."
  (:require
   [clojure.java.io :as io]
   [clojure.tools.build.api :as b]
   [hiccup.page :as hiccup.page]
   [schema-voyager.db.persist :as db.persist]
   [schema-voyager.template.config :as template.config]))


(defn- die
  ([code message & args]
   (die code (apply format message args)))
  ([code message]
   (binding [*out* *err*]
     (println message))
   (System/exit code)))

(defn create-placeholder-db []
  (println "\nCreating placeholder DB...")
  (db.persist/save-db template.config/db-template-placeholder))

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
  (spit (io/file "resources" template.config/template-file-name)
        ;; IMPORTANT: keep this in sync with index.html
        (hiccup.page/html5
         [:head
          [:meta {:charset "UTF-8"}]
          [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
          [:title "Schema Voyager"]
          [:style (slurp "target/styles.css")]
          [:script {:src "https://cdn.jsdelivr.net/npm/@hpcc-js/wasm@1.12.7/dist/index.min.js" :integrity "sha256-N1b/aWuIrKlh7iz7GpUSJo17CQz+tAQt8HBB3u/4HOg=" :crossorigin "anonymous"}]
          [:script "var hpccWasm = window[\"@hpcc-js/wasm\"];"]]
         [:body
          [:div#app]
          [:script {:type "text/javascript"} (slurp "target/main.js")]])))

#_{:clj-kondo/ignore #{:clojure-lsp/unused-public-var}}
(defn make-template
  "Creates a template file with a placeholder string instead of a DataScript DB.
  It's used by `schema-voyager.template.standalone/fill-template`, which takes
  an ingested DB, then substitutes that into the template file."
  [params]
  (create-placeholder-db)
  (optimized-js)
  (optimized-css)
  (standalone-html)
  params)
