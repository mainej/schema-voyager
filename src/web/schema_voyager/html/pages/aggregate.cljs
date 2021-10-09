(ns schema-voyager.html.pages.aggregate
  (:require [schema-voyager.html.pages.collection :as pages.collection]))

(defn page [parameters]
  [pages.collection/page
   (pages.collection/collection-from-route :aggregate parameters)])
