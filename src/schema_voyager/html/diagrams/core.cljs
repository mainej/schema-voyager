(ns schema-voyager.html.diagrams.core
  (:require [dorothy.core :as dot]
            [reagent.dom.server :as dom]
            [schema-voyager.html.util :as util]
            [schema-voyager.html.diagrams.config :as diagrams.config]
            [schema-voyager.html.diagrams.util :as diagrams.util]))

(def ^:private colors
  {:purple-700  "#6b46c1"
   :green-600   "#38a169"
   :blue-500    "#4299e1"
   :white       "#ffffff"
   :gray-100    "#f7fafc"
   :gray-200    "#edf2f7"
   :gray-300    "#e2e8f0"
   :gray-500    "#a0aec0"
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
             (diagrams.util/with-dot-to-svg s
               #(set! (.-innerHTML div) %))))}])

(def ^:private html
  dom/render-to-static-markup)

(defn- dot-node [[coll attrs] attrs-visible?]
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
                               :color   (colors :gray-300) ;; border color
                               :bgcolor (colors :white)
                               :sides   "brl"
                               :href    (util/coll-href coll)
                               :title   coll-name}
                          [:font {:color coll-color} coll-name]]])
                  (when attrs-visible?
                    (for [{:keys [db/ident] :as attr} attrs]
                      ^{:key ident}
                      [:tr [:td {:align   "LEFT"
                                 :color   (colors :gray-300) ;; border color
                                 :bgcolor (colors :gray-100)
                                 :sides   "brl"
                                 :port    (attr-id attr)
                                 :href    (util/attr-href attr)
                                 :title   (pr-str ident)}
                            [:font {:color coll-color} ":" (namespace ident)]
                            [:font {:color (colors :gray-500)} "/"]
                            [:font {:color (colors :blue-500)} (name ident)]]]))])}]))

(defn- dot-edge [[source target attr] attrs-visible?]
  (let [source-id   (coll-id source)
        source-port (attr-id attr)
        target-id   (coll-id target)
        self-ref?   (= source-id target-id)]
    [(str source-id (when attrs-visible? (str ":" source-port)) (when self-ref? ":e"))
     (str target-id (when self-ref? ":ne"))
     {:arrowhead (if (= (:db/cardinality attr) :db.cardinality/one)
                   "inv"
                   "crow")
      :tooltip   (pr-str (:db/ident attr))
      :href      (util/attr-href attr)}]))

(defn- dot-graph [references {:keys [excluded-eids attrs-visible?]}]
  (let [excluded-entity? (comp (excluded-eids) :db/id)
        attrs-visible?   (attrs-visible?)
        ;; do not show reference if either source, target, or attribute is marked as excluded
        shown-references (remove (fn [entities]
                                   (some excluded-entity? entities))
                                 references)]
    (dot/dot (dot/digraph
              (concat
               [(dot/graph-attrs {:bgcolor (colors :gray-200)})
                (dot/node-attrs {:shape    "plaintext"
                                 :fontname "Helvetica"
                                 :fontsize 12})
                (dot/edge-attrs {:color     (colors :gray-600)
                                 :penwidth  0.5
                                 :arrowsize 0.75})]
               (->> shown-references
                    diagrams.util/colls-with-attrs
                    (map #(dot-node % attrs-visible?)))
               (->> shown-references
                    (map #(dot-edge % attrs-visible?))))))))

(defn erd [_]
  (let [config-state (diagrams.config/state)]
    (fn [references]
      (when (seq references)
        (let [dot-s (dot-graph references config-state)]
          [:div
           [diagrams.config/config config-state references dot-s]
           [graphviz-svg dot-s]])))))