(ns schema-voyager.build.standalone-html
  (:require
   [clojure.java.io :as io]
   [clojure.string :as string]))

(def db-template-placeholder 'SCHEMA_VOYAGER_DB_PLACEHOLDER)

(def template-dir (io/resource "standalone_template"))
(def template-file (io/file template-dir "standalone.html"))

#_{:clj-kondo/ignore #{:clojure-lsp/unused-public-var}}
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
