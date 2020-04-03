(ns schema-voyager.html.diagrams.collection
  (:require [graphviz]
            [dorothy.core :as dot]
            [promesa.core :as p]
            [reagent.dom.server :as dom]
            [clojure.walk :as walk]
            [datascript.core :as d]
            [schema-voyager.html.util :as util]))

(def ^:private colors
  {:purple-700  "#6b46c1"
   :green-600   "#38a169"
   :blue-500    "#4299e1"
   :gray-300    "#e2e8f0"
   :gray-600    "#718096"
   :transparent "transparent"})

(defn- coll-id [{coll-type :db.schema.collection/type
                coll-name :db.schema.collection/name}]
  (str (name coll-type) "__" (name coll-name)))

(defn- attr-id [{:keys [db/ident]}]
  (str "attr__" (namespace ident) "__" (name ident)))

(defn- graphviz-svg [s]
  [:div.overflow-auto
   {:ref (fn [div]
           (when div
             ;; TODO: decide on layout engine: fdp, neato, dot
             (p/let [svg (graphviz/graphviz.dot s)]
               (set! (.-innerHTML div) svg))))}])

(def ^:private html
  dom/render-to-static-markup)

(defn- dot-node [[coll attrs]]
  (let [id         (coll-id coll)
        coll-color (colors (case (:db.schema.collection/type coll)
                             :enum      :green-600
                             :aggregate :purple-700))]
    [id {:label (html
                 [:table {:port        id
                          :border      0
                          :cellborder  1
                          :cellspacing 0
                          :cellpadding 4}
                  (let [coll-name (pr-str (:db.schema.collection/name coll))]
                    [:tr [:td {:align   "TEXT"
                               :color   (colors :gray-300)
                               :bgcolor "white"
                               :sides   "br"
                               :href    (util/coll-href coll)
                               :title   coll-name}
                          [:font {:color coll-color} coll-name]]])
                  (for [{:keys [db/ident] :as attr} attrs]
                    ^{:key ident}
                    [:tr [:td {:align   "LEFT"
                               :color   (colors :gray-300)
                               :bgcolor "white"
                               :sides   "br"
                               :port    (attr-id attr)
                               :href    (util/attr-href attr)
                               :title   (pr-str ident)}
                          [:font {:color coll-color} ":" (namespace ident)]
                          "/"
                          [:font {:color (colors :blue-500)} (name ident)]]])])}]))

(defn- dot-edge [[source target attr]]
  [(str (coll-id source) ":" (attr-id attr))
   (coll-id target)
   {:arrowhead (if (= (:db/cardinality attr) :db.cardinality/one)
                 "inv"
                 "crow")
    :tooltip   (pr-str (:db/ident attr))
    :href      (util/attr-href attr)}])

(defn erd [references]
  (let [attrs-by-sources (->> references
                              (group-by first)
                              (map (fn [[source refs]]
                                     [source (sort-by :db/ident (distinct (map last refs)))]))
                              (into {}))
        colls            (->> references
                              (mapcat (juxt first second))
                              distinct)]
    [graphviz-svg
     (dot/dot (dot/digraph
               (concat
                [(dot/graph-attrs {:bgcolor (colors :transparent)})
                 (dot/node-attrs {:shape "plaintext"})
                 (dot/edge-attrs {:color     (colors :gray-600)
                                  :penwidth  0.5
                                  :arrowsize 0.75})]
                (->> colls
                     (map (fn [coll]
                            [coll (attrs-by-sources coll)]))
                     (map dot-node))
                (->> references
                     (map dot-edge)))))]))

(def ^:private ref-q
  '[:find ?source ?dest ?source-attr
    :where
    [?source-attr :db.schema/part-of ?source]
    (or-join [?source-attr ?dest]
             [?source-attr :db.schema/references ?dest]
             (and
              [?source-attr :db.schema/tuple-references ?dest-tuple-ref]
              [?dest-tuple-ref :db.schema/references ?dest]))
    (not [?source-attr :db.schema/deprecated? true])])

(defn- q-expand-eids [db refs]
  (let [eids            (distinct (mapcat identity refs))
        entities        (d/pull-many db '[*] eids)
        entities-by-eid (zipmap (map :db/id entities)
                                entities)]
    (walk/postwalk-replace entities-by-eid refs)))

(defn q-colls [db]
  (let [refs (d/q ref-q db)]
    (q-expand-eids db refs)))

(defn q-coll [db coll]
  (let [coll-eid (d/q '[:find ?coll .
                        :in $ ?collection-type ?collection-name
                        :where
                        [?coll :db.schema.collection/type ?collection-type]
                        [?coll :db.schema.collection/name ?collection-name]
                        [?coll :db.schema.pseudo/type :collection]]
                      db (:db.schema.collection/type coll) (:db.schema.collection/name coll))
        sources  (d/q (concat ref-q '[:in $ ?source])
                      db coll-eid)
        dests    (d/q (concat ref-q '[:in $ ?dest])
                      db coll-eid)
        refs     (distinct (concat sources dests))]
    (q-expand-eids db refs)))

(defn q-attr [db attr]
  (let [attr-eid (:db/id (d/pull db [:db/id] (:db/ident attr)))
        refs     (d/q (concat ref-q '[:in $ ?source-attr])
                      db attr-eid)]
    (q-expand-eids db refs)))
