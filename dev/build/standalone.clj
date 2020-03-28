(ns build.standalone
  (:require [schema-voyager.html.standalone :as html]))

(defn -main []
  #_(html/debug)
  (html/standalone)
  (shutdown-agents))

(comment
  (html/standalone)
  )
