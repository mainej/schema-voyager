(ns ingest.projects.mbrainz
  (:require
   [schema-voyager.cli :as cli]))

(defn import-mbrainz
  "Create datascript DB that contains the mbrainz schema, enums, and supplemental
  data about references.

  Loaded out of resources/mbrainz-*.edn files.

  Invoke with clojure -A:ingest -X ingest.projects.mbrainz/import-mbrainz"
  [_]
  (cli/ingest {:sources [{:file/name "resources/mbrainz-schema.edn"}
                         {:file/name "resources/mbrainz-enums.edn"}
                         {:file/name "resources/mbrainz-supplemental.edn"}]}))

;; Needed by Netlify
(defn -main []
  (import-mbrainz {}))
