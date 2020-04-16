(ns schema-voyager.html.diagrams.util
  (:require [graphviz]
            [promesa.core :as p]))

(defn with-dot-to-svg [dot-s f]
  (p/let [svg (graphviz/graphviz.dot dot-s)]
    (f svg)))

(defn colls-with-attrs [references]
  (let [attrs-by-sources (->> references
                              (group-by first)
                              (map (fn [[source refs]]
                                     [source (sort-by :db/ident (distinct (map last refs)))]))
                              (into {}))]
    (->> references
         (mapcat (juxt first second))
         distinct
         (sort-by (juxt :db.schema.collection/type :db.schema.collection/name))
         (map (fn [coll]
                [coll (attrs-by-sources coll)])))))
