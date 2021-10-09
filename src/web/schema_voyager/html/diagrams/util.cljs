(ns schema-voyager.html.diagrams.util
  (:require [graphviz]
            [promesa.core :as p]))

(defn with-dot-to-svg [dot-s f]
  (p/let [svg (graphviz/graphviz.dot dot-s)]
    (f svg)))

(defn edges-as-nodes
  "Convert `edges`, which are graph edges, into graph nodes.

  In particular, `edges`, which are tuples of [source-collection source-attr
  target-collection], become a list of all the collections, either sources or
  targets, along with the attributes of the sources.

  The returned collections are sorted by type and name, which downstream code
  depends on."
  [edges]
  (let [attrs-by-sources (->> edges
                              (reduce (fn [result [source attr _target]]
                                        (update result source (fnil conj []) attr))
                                      {})
                              (map (fn [[source attrs]]
                                     [source (sort-by :db/ident (distinct attrs))]))
                              (into {}))]
    (->> edges
         (mapcat (fn [[source _attr target]]
                   [source target]))
         distinct
         (sort-by (juxt :db.schema.collection/type :db.schema.collection/name))
         (map (fn [coll]
                (assoc coll :db.schema.collection/attributes (attrs-by-sources coll)))))))
