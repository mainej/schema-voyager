(ns schema-voyager.ingest.file
  "Load schema information out of EDN files.

  These files may contain the data that was transacted into Datomic to establish
  your schema. They may also contain 'supplemental' data about your schema; see
  the [supplemental properties docs](/doc/annotation.md) for a definition of
  what kind of supplemental data you can include. The files can contain any
  special tags documented in [[schema-voyager.ingest.core/read-string]]."
  (:require [schema-voyager.ingest.core :as ingest]))

(defn ingest
  "Read a `file`, using the tags defined in [[schema-voyager.ingest.core/read-string]]."
  [file]
  (ingest/read-string (slurp file)))
