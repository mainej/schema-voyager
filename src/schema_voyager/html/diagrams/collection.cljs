(ns schema-voyager.html.diagrams.collection
  (:require [graphviz]
            [dorothy.core :as dot]
            [promesa.core :as p]
            [reagent.core :as r]
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

(defn dropdown-state []
  (let [!open? (r/atom false)]
    {:dropdown-open?  #(deref !open?)
     :dropdown-close  #(reset! !open? false)
     :dropdown-open   #(reset! !open? true)
     :dropdown-toggle #(swap! !open? not)}))

(def configure-gear
  [:svg.h-6.w-6.fill-none.stroke-current.stroke-2 {:viewbox "0 0 24 24"}
   [:title "Configure Diagram"]
   [:g {:stroke-linejoin "round"
        :stroke-linecap  "round"}
    [:path {:d "M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"}]
    [:path {:d "M15 12a3 3 0 11-6 0 3 3 0 016 0z"}]]])

(defn erd-config [_ _]
  (let [{:keys [dropdown-open? dropdown-close dropdown-toggle]} (dropdown-state)]
    (fn [references !excluded-eids]
      (let [excluded? @!excluded-eids
            colls     (colls-with-attrs references)

            checkbox-attrs (fn [entity]
                             {:type      "checkbox"
                              :checked   (not (excluded? (:db/id entity)))
                              :on-change (fn [e]
                                           (.stopPropagation e)
                                           (swap! !excluded-eids (if (-> e .-target .-checked)
                                                                   disj conj)
                                                  (:db/id entity)))})]
        [:div.relative.inline-block
         [:button.rounded-md.border.border-gray-300.p-2.bg-white.text-gray-700.hover:text-gray-500.focus:outline-none.focus:border-blue-300.focus:shadow-outline-blue.active:bg-gray-50.active:text-gray-800.transition.ease-in-out.duration-150
          {:type     "button"
           :on-click dropdown-toggle}
          configure-gear]
         (when (dropdown-open?)
           [:<>
            [:div.fixed.inset-0.bg-gray-900.opacity-50
             {:on-click dropdown-close}]
            [:div.absolute.mt-2.rounded-md.shadow-lg.overflow-hidden.origin-top-left.left-0.bg-white.text-xs.leading-5.text-gray-700.whitespace-no-wrap
             (for [[coll attrs] colls]
               ^{:key (:db/id coll)}
               [:div.p-3.border-t.border-gray-300
                [:label.block
                 {:class (when (seq attrs) :pb-1)}
                 [:input.mr-1 (checkbox-attrs coll)]
                 [util/coll-name coll]]
                (for [attr attrs]
                  ^{:key (:db/id attr)}
                  [:label.block.ml-4.py-1
                   [:input.mr-1 (checkbox-attrs attr)]
                   [util/ident-name (:db/ident attr) (:db.schema.collection/type coll)]])])]])]))))

(defn erd []
  (let [!excluded-eids (r/atom #{})]
    (fn [references]
      (let [excluded?        @!excluded-eids
            shown-references (remove (fn [entities]
                                       (some excluded? (map :db/id entities)))
                                     references)]
        [:div
         [erd-config references !excluded-eids]
         [graphviz-svg
          (dot/dot (dot/digraph
                    (concat
                     [(dot/graph-attrs {:bgcolor (colors :transparent)})
                      (dot/node-attrs {:shape "plaintext"})
                      (dot/edge-attrs {:color     (colors :gray-600)
                                       :penwidth  0.5
                                       :arrowsize 0.75})]
                     (->> shown-references
                          colls-with-attrs
                          (map dot-node))
                     (->> shown-references
                          (map dot-edge)))))]]))))

(def ^:private ref-q
  '[:find ?source ?dest ?source-attr
    :where
    [?source-attr :db.schema/part-of ?source]
    (or-join [?source-attr ?dest]
             [?source-attr :db.schema/references ?dest]
             (and
              [?source-attr :db.schema/tuple-references ?dest-tuple-ref]
              [?dest-tuple-ref :db.schema/references ?dest]))])

(def ^:private active-ref-q
  (into ref-q '[(not [?source-attr :db.schema/deprecated? true])]))

(defn- q-expand-eids [db refs]
  (let [eids            (distinct (mapcat identity refs))
        entities        (d/pull-many db '[*] eids)
        entities-by-eid (zipmap (map :db/id entities)
                                entities)]
    (walk/postwalk-replace entities-by-eid refs)))

(defn q-colls [db]
  (let [refs (d/q active-ref-q db)]
    (q-expand-eids db refs)))

(defn q-coll [db coll]
  (let [coll-eid (d/q '[:find ?coll .
                        :in $ ?collection-type ?collection-name
                        :where
                        [?coll :db.schema.collection/type ?collection-type]
                        [?coll :db.schema.collection/name ?collection-name]
                        [?coll :db.schema.pseudo/type :collection]]
                      db (:db.schema.collection/type coll) (:db.schema.collection/name coll))
        sources  (d/q (concat active-ref-q '[:in $ ?source])
                      db coll-eid)
        dests    (d/q (concat active-ref-q '[:in $ ?dest])
                      db coll-eid)
        refs     (distinct (concat sources dests))]
    (q-expand-eids db refs)))

(defn q-attr [db attr]
  (let [attr-eid (:db/id (d/pull db [:db/id] (:db/ident attr)))
        refs     (d/q (concat ref-q '[:in $ ?source-attr])
                      db attr-eid)]
    (q-expand-eids db refs)))
