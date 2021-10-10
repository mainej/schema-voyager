(ns schema-voyager.template.standalone
  "Create a standalone HTML file, by pouring a DataScript DB into a template.

  OK, what's going on here? Why does this template exist?

  There are two modes to using Schema Voyager.

  When you're hacking on Schema Voyager, you use shadow-cljs to compile the CLJS
  and launch a server. Of course, changes to the .cljs files are shipped to the
  browser automatically. This version of the CLJS reads the schema data from a
  file which contains an EDN version of a DataScript DB. Changes to the file are
  shipped to the browser too, through the magic of `shadow.resource/inline`.
  Thus, when you re-run [[schema-voyager.cli/ingest]], the DB file is updated
  and then the web page updates. This is great for live interaction with Schema
  Voyager code—you get quick feedback on changes both to the CLJS and to the
  ingestion process. But it doesn't work so well with the other mode of using
  Schema Voyager.

  As an application author, you don't care about dynamically updating CLJS. You
  also don't want a DB file, but instead want a single HTML file. That's where
  this template comes in. We put a placeholder string into the file that usually
  holds the DB, compile that into the JS (in a release build,
  `shadow.resource/inline` inlines whatever is in the DB file *at that moment*),
  and package everything up as an HTML file. Then the application author only
  has to ingest their schema data and substitute it in place of the placeholder.
  That's fast and means that we don't have to try to compile Schema Voyager's
  CLJS from the application's codebase."
  (:require
   [clojure.java.io :as io]
   [clojure.string :as string]
   [schema-voyager.template.config :as template.config]))

#_{:clj-kondo/ignore #{:clojure-lsp/unused-public-var}}
(defn fill-template
  "Creates a standalone HTML page at `output-path`, by subsitituting the `db`
  into the template file."
  [output-path db]
  (let [target-file (io/file "." output-path)
        contents    (slurp (io/resource template.config/template-file-name))
        replaced    (string/replace contents
                                    (pr-str (str template.config/db-template-placeholder))
                                    (pr-str (pr-str db)))]
    (io/make-parents target-file)
    (spit target-file replaced :append false)))
