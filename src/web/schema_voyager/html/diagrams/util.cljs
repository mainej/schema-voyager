(ns schema-voyager.html.diagrams.util
  (:require
   [reagent.core :as r]
   [graphviz]
   [promesa.core :as p]))

(defonce state (r/atom {:fit-screen? false}))
(def ^:private !fit-screen? (r/cursor state [:fit-screen?]))
(defn- fit-screen? [] @!fit-screen?)

(def colors
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

#_{:clj-kondo/ignore [:unused-private-var]}
(defn- label-edges
  "Technically, assuming dot-edge has been given :label, the edges already have
  their label. But we want the labels to follow the paths of the edges. Graphviz
  doesn't know how to do this, but SVGs do."
  [svg]
  (doseq [edge (.querySelectorAll svg ".edge")]
    (when-let [text-node (.querySelector edge "text")]
      (let [path-id        (str (.-id edge) "-path")
            text-content   (.-innerHTML text-node)
            text-path-node (js/document.createElementNS "http://www.w3.org/2000/svg" "textPath")]
        ;; move the text into a textPath node, which follows the edge's path
        (-> edge (.querySelector "path") (.setAttribute "id" path-id))
        (doto text-path-node
          (.setAttribute "startOffset" "50%")
          (.setAttribute "href" (str "#" path-id)))
        (set! (.-innerHTML text-path-node) text-content)

        ;; adjust the text node to render the textPath
        (doto text-node
          (.setAttribute "fill" (colors :gray-600))
          (.setAttribute "dy" "-6px")
          (.removeAttribute "x")
          (.removeAttribute "y"))
        (.replaceChildren ^js/Element text-node text-path-node)

        ;; when the text is moved into a path, the surrounding anchor tag
        ;; loses its implicit title
        (-> text-node
            .-parentNode
            (.setAttributeNS "http://www.w3.org/1999/xlink" "title" text-content))))))

;;;; Imperative nonsense for resizing the SVG.

(defn- ^js/SVGAnimatedString get-svg! []
  (js/document.querySelector "#diagram-svg"))

(defn- fit-screen!
  "graphviz gives the svg a height and width that match the viewbox height and
  width, meaning the svg is as large as the viewbox, even if that means it has
  to overflow its container. We can remove those attributes to force the svg to
  fit in its container."
  [^js/Element svg]
  (reset! !fit-screen? true)
  (doto svg
    (.removeAttribute "height")
    (.removeAttribute "width")))

(defn- fit-intrinsic!
  "Restore the svg's width and height, so that it overflows its container. See
  [[fit-screen!]]."
  [^js/SVGAnimatedString svg]
  (reset! !fit-screen? false)
  (let [view-box (.-baseVal (.-viewBox svg))]
    (doto svg
      (.setAttribute "height" (.-height view-box))
      (.setAttribute "width" (.-width view-box)))))

(defn toggle-fit-screen []
  (let [svg (get-svg!)]
    (if (fit-screen?) (fit-intrinsic! svg) (fit-screen! svg))))

(defn with-dot-to-svg [dot-s f]
  (p/let [svg-s (graphviz/graphviz.dot dot-s)]
    (let [svg (-> (js/document.createRange) (.createContextualFragment svg-s) (.querySelector "svg"))]
      (.setAttribute svg "id" "diagram-svg")
      ;; keep in sync with user selection of sizing
      (if (fit-screen?) (fit-screen! svg) (fit-intrinsic! svg))
      ;; To make this work, must also enable :label on dot-edge
      #_(label-edges svg)
      (f svg))))

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
