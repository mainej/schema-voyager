(ns ingest.projects.mbrainz
  (:require
   [schema-voyager.cli :as cli]))

(defn import-mbrainz
  "Creates a web page at target/index.html that contains the mbrainz schema,
  enums, and supplemental data about references.

  Loaded out of resources/mbrainz-schema/*.edn files.

  Invoke with clojure -A:netlify -X ingest.projects.mbrainz/import-mbrainz"
  [_]
  (cli/standalone {:sources     [{:file/name "resources/mbrainz-schema/schema.edn"}
                                 {:file/name "resources/mbrainz-schema/enums.edn"}
                                 {:file/name "resources/mbrainz-schema/supplemental.edn"}]
                   :output-path "target/index.html"}))

;; Needed by Netlify
(defn -main []
  (import-mbrainz {}))
