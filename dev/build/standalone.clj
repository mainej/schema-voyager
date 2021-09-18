(ns build.standalone
  (:require [schema-voyager.html.standalone :as html]))

(defn standalone [_]
  #_(html/debug)
  (html/standalone)
  (shutdown-agents))

(comment
  (html/standalone)
  )
