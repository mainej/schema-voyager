(ns build.standalone
  (:require [schema-voyager.html.standalone :as html]))

(defn standalone [_]
  #_(html/debug)
  (html/standalone)
  (shutdown-agents))

(defn -main []
  (standalone {}))

(comment
  (html/standalone)
  )
