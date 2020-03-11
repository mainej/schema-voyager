(ns ingest
  (:require [clojure.java.io :as io]
            [datascript.core :as d]
            [schema-voyager.data :as data]))

(defn- into-db [data]
  (d/db-with (d/empty-db data/metaschema) data))

(defn- join-files [files]
  (reduce (fn [result file]
            (data/join result
                       (data/read-string (slurp (io/file file)))))
          []
          files))

(defn save-db [db]
  (spit "resources/db.edn" (pr-str db)))

(defn from-files [files]
  (-> (join-files files)
      (data/process)
      (into-db)))

(defn -main [& files]
  (save-db (from-files files)))
