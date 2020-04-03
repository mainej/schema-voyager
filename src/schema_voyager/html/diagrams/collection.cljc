(ns schema-voyager.html.diagrams.collection
  (:require #?@(:cljs [[graphviz]
                       [dorothy.core :as d]
                       [promesa.core :as p]
                       [reagent.dom.server :as dom]])
            [schema-voyager.html.util :as util]))

(def colors
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

(defn graphviz-svg [s]
  #?(:clj [:div]
     :cljs
     [:div.overflow-auto
      {:ref (fn [div]
              (when div
                ;; TODO: decide on layout engine: fdp, neato, dot
                (p/let [svg (graphviz/graphviz.dot s)]
                  (set! (.-innerHTML div) svg))))}]))

(def html
  #?(:clj (constantly nil)
     :cljs dom/render-to-static-markup))

(defn dot-node [[coll attrs]]
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
                  (for [attr attrs]
                    (let [id (attr-id attr)]
                      ^{:key id}
                      [:tr [:td {:align   "LEFT"
                                 :color   (colors :gray-300)
                                 :bgcolor "white"
                                 :sides   "br"
                                 :port id
                                 :href    (util/attr-href attr)
                                 :title   (pr-str (:db/ident attr))}
                            [:font {:color coll-color} ":" (namespace (:db/ident attr))]
                            "/"
                            [:font {:color (colors :blue-500)} (name (:db/ident attr))]]]))])}]))

(defn dot-edge [[source target attr]]
  [(str (coll-id source) ":" (attr-id attr))
   (coll-id target)
   {:arrowhead (if (= (:db/cardinality attr) :db.cardinality/one)
                 "inv"
                 "crow")
    :tooltip   (pr-str (:db/ident attr))
    :href      (util/attr-href attr)}])

(defn erd [references]
  #?(:clj [:div]
     :cljs
     (let [attrs-by-sources (->> references
                                 (group-by first)
                                 (map (fn [[source refs]]
                                        [source (map last refs)]))
                                 (into {}))
           colls            (->> references
                                 (mapcat (juxt first second))
                                 distinct)]
       [graphviz-svg
        (d/dot (d/digraph
                (concat
                 [(d/graph-attrs {:bgcolor (colors :transparent)})
                  (d/node-attrs {:shape "plaintext"})
                  (d/edge-attrs {:color     (colors :gray-600)
                                 :penwidth  0.5
                                 :arrowsize 0.75})]
                 (->> colls
                      (map (fn [coll]
                             [coll (attrs-by-sources coll)]))
                      (map dot-node))
                 (->> references
                      (map dot-edge)))))])))
