(ns schema-voyager.html.diagrams.core
  (:require [dorothy.core :as dot]
            [reagent.core :as r]
            [reagent.dom.server :as dom]
            [schema-voyager.html.util :as util]
            [schema-voyager.html.diagrams.config :as diagrams.config]
            [schema-voyager.html.diagrams.util :as diagrams.util]))

(def ^:private colors
  {:purple-700  "#6D28D9"
   :green-600   "#059669"
   :blue-600    "#2563EB"
   :white       "#ffffff"
   :gray-100    "#F3F4F6"
   :gray-200    "#E5E7EB"
   :gray-300    "#D1D5DB"
   :gray-500    "#6B7280"
   :gray-600    "#4B5563"
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
               (fn [svg-string]
                 (set! (.-innerHTML div) svg-string)
                 (doto (.querySelector div "svg")
                   (.setAttribute "id" "diagram-svg"))
                 ;; keep in sync with user selection of sizing
                 (if (diagrams.config/fit-screen?)
                   (diagrams.config/fit-screen!)
                   (diagrams.config/fit-intrinsic!))))))}])

(def ^:private html
  dom/render-to-static-markup)

(defn coll-color [coll]
  (colors (if (= :enum (:db.schema.collection/type coll))
            :green-600
            :purple-700)))

(defn attr-coll-color [attr]
  ;; Are constants ever rendered as attributes in the diagrams? I don't think
  ;; so, so this could probably always be purple.
  (colors (if (= :constant (:db.schema.pseudo/type attr))
            :green-600
            :purple-700)))

(defn- dot-node [coll attrs-visible?]
  (let [id (coll-id coll)]
    [id {:label (html
                 [:table {:port         id
                          :border       0
                          :cellborder   1
                          :cell-spacing 0
                          :cell-padding 4}
                  (let [coll-name (pr-str (:db.schema.collection/name coll))]
                    [:tr [:td {:align   "TEXT"
                               :color   (colors :gray-300) ;; border color
                               :bgcolor (colors :white)
                               :sides   "brl"
                               :href    (util/coll-href coll)
                               :title   coll-name}
                          [:font {:color (coll-color coll)} coll-name]]])
                  (when attrs-visible?
                    (for [{:keys [db/ident] :as attr} (:db.schema.collection/attributes coll)]
                      ^{:key ident}
                      [:tr [:td {:align   "LEFT"
                                 :color   (colors :gray-300) ;; border color
                                 :bgcolor (colors :gray-100)
                                 :sides   "brl"
                                 :port    (attr-id attr)
                                 :href    (util/attr-href attr)
                                 :title   (pr-str ident)}
                            [:font {:color (attr-coll-color attr)} ":" (namespace ident)]
                            [:font {:color (colors :gray-500)} "/"]
                            [:font {:color (colors :blue-600)} (name ident)]]]))])}]))

(defn- dot-edge [[source attr target] attrs-visible?]
  (let [source-id   (coll-id source)
        source-port (attr-id attr)
        target-id   (coll-id target)
        self-ref?   (= source-id target-id)]
    [(str source-id (when attrs-visible? (str ":" source-port)) (when self-ref? ":e"))
     (str target-id (when self-ref? ":n"))
     {:arrowhead (if (= (:db/cardinality attr) :db.cardinality/one)
                   "inv"
                   "crow")
      :tooltip   (pr-str (:db/ident attr))
      :href      (util/attr-href attr)}]))

(defn- dot-graph [edges]
  (let [attrs-visible? (diagrams.config/attrs-visible?)
        ;; do not show edges if either source, target, or attribute is marked as excluded
        shown-edges    (remove diagrams.config/some-entities-excluded? edges)]
    (dot/dot (dot/digraph
              (concat
               [(dot/graph-attrs {:bgcolor (colors :gray-200)})
                (dot/node-attrs {:shape    "plaintext"
                                 :fontname "Helvetica"
                                 :fontsize 12})
                (dot/edge-attrs {:color     (colors :gray-600)
                                 :class     "stroke-half hover:stroke-1"
                                 :arrowsize 0.75})]
               (->> shown-edges
                    diagrams.util/edges-as-nodes
                    (map #(dot-node % attrs-visible?)))
               (->> shown-edges
                    (map #(dot-edge % attrs-visible?))))))))

(defn erd [edges]
  (r/with-let [_ (diagrams.config/reset-state)]
    (when (seq edges)
      (let [dot-s (dot-graph edges)]
        [:div
         [:div.ml-4.sm:ml-0
          [diagrams.config/config (diagrams.util/edges-as-nodes edges) dot-s]]
         [graphviz-svg dot-s]]))))
