(ns schema-voyager.html.diagrams.collection
  (:require [graphviz]
            [dorothy.core :as dot]
            [promesa.core :as p]
            [reagent.core :as r]
            [reagent.dom.server :as dom]
            [clojure.walk :as walk]
            ["file-saver" :as file-saver]
            [datascript.core :as ds]
            [schema-voyager.html.util :as util]))

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

(defn- colls-with-attrs [references]
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

(defn- graphviz-svg [s]
  [:div.overflow-auto
   {:ref (fn [div]
           (when div
             ;; TODO: decide on layout engine: fdp, neato, dot
             (p/let [svg (graphviz/graphviz.dot s)]
               (set! (.-innerHTML div) svg))))}])

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
  (let [excluded-eid?    (comp (excluded-eids) :db/id)
        attrs-visible?   (attrs-visible?)
        shown-references (remove (fn [entities]
                                   (some excluded-eid? entities))
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
                    colls-with-attrs
                    (map #(dot-node % attrs-visible?)))
               (->> shown-references
                    (map #(dot-edge % attrs-visible?))))))))

(defn- set-toggle [s item]
  (if (contains? s item)
    (disj s item)
    (conj s item)))

(defn- attrs-visible-state []
  (let [!attrs-visible? (r/atom true)]
    {:attrs-visible?       #(deref !attrs-visible?)
     :attrs-visible-toggle #(swap! !attrs-visible? not)}))

(defn- dropdown-state []
  (let [!open? (r/atom false)]
    {:dropdown-open?  #(deref !open?)
     :dropdown-close  #(reset! !open? false)
     :dropdown-open   #(reset! !open? true)
     :dropdown-toggle #(swap! !open? not)}))

(defn- excluded-eid-state []
  (let [!excluded-eids (r/atom #{})]
    {:excluded-eids #(deref !excluded-eids)
     :exclude-eids  (fn [entities]
                      (swap! !excluded-eids #(apply conj % (map :db/id entities))))
     :include-eids  (fn [entities]
                      (swap! !excluded-eids #(apply disj % (map :db/id entities))))
     :toggle-eid    (fn [entity]
                      (swap! !excluded-eids set-toggle (:db/id entity)))}))

(def ^:private configure-gear
  [:svg.h-6.w-6.fill-none.stroke-current.stroke-2
   {:viewBox         "0 0 24 24"
    :stroke-linejoin "round"
    :stroke-linecap  "round"}
   [:title "Configure Diagram"]
   [:path {:d "M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"}]
   [:path {:d "M15 12a3 3 0 11-6 0 3 3 0 016 0z"}]])

(def ^:private download-icon
  [:svg.inline-block.w-4.h-4.fill-none.stroke-current.text-teal-500.group-hover:text-teal-400.stroke-2.transition-colors.ease-in-out.duration-200
   {:viewBox         "0 0 24 24"
    :stroke-linejoin "round"
    :stroke-linecap  "round"}
   [:title "Download"]
   [:path {:d "M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"}]])

(defn- toggle-span [checked]
  [:span.relative.inline-block.leading-none.flex-shrink-0.h-4.w-6.border-2.border-transparent.rounded-full.transition-colors.ease-in-out.duration-200.focus:outline-none.focus:shadow-outline
   {:aria-checked (pr-str checked)
    :tabIndex     "0"
    :role         "checkbox"
    :class        (if checked :bg-teal-400 :bg-gray-200)}
   [:span.inline-block.h-3.w-3.rounded-full.bg-white.shadow.transform.transition.ease-in-out.duration-200
    {:class       (if checked :translate-x-2 :translate-x-0)
     :aria-hidden "true"}]])

(defn- stop [e]
  (.stopPropagation e))

(defn- prevent [e]
  (.preventDefault e))

(defn- toggle-handlers [on-change]
  {:on-click    (fn [e]
                  (stop e)
                  (on-change))
   :on-key-down (fn [e]
                  (let [key (.-key e)]
                    (when (not= "Tab" key)
                      (prevent e))
                    (when (= " " key)
                      (on-change))))})

(defn- erd-collection-config [[coll attrs] {:keys [excluded-eids toggle-eid attrs-visible?]}]
  (let [excluded-eid? (comp (excluded-eids) :db/id)]
    [:div.p-3.stack-my-2
     [:div.flex.items-center.stack-mx-2.cursor-pointer
      (toggle-handlers #(toggle-eid coll))
      [toggle-span (not (excluded-eid? coll))]
      [util/coll-name coll]]
     (when (and (attrs-visible?) (seq attrs))
       [:div.ml-4.stack-my-2
        (for [attr attrs]
          ^{:key (:db/id attr)}
          [:div.flex.items-center.stack-mx-2.cursor-pointer
           (toggle-handlers #(toggle-eid attr))
           [toggle-span (not (excluded-eid? attr))]
           [util/ident-name (:db/ident attr) (:db.schema.collection/type coll)]])])]))

(defn- config-dropdown [{:keys [dropdown-open? dropdown-close dropdown-toggle]} body]
  [:div.relative.inline-block.ml-4.sm:ml-0
   [:button.rounded-md.border.p-2.bg-white.text-gray-700.hover:text-gray-500.focus:outline-none.focus:border-blue-300.focus:shadow-outline-blue.active:bg-gray-50.active:text-gray-800.transition.ease-in-out.duration-150
    {:type     "button"
     :on-click dropdown-toggle}
    configure-gear]
   (when (dropdown-open?)
     [:<>
      [:div.fixed.inset-0.bg-gray-900.opacity-50
       {:on-click dropdown-close}]
      body])])

(defn- config-attr-visibility [{:keys [attrs-visible? attrs-visible-toggle]}]
  [:div.p-3.border-b.border-t.border-gray-500
   [:div.flex.items-center.stack-mx-2.cursor-pointer
    (toggle-handlers attrs-visible-toggle)
    [toggle-span (attrs-visible?)]
    [:span "Show attributes on aggregates?"]]])

(defn- config-enum-visibility [enums {:keys [excluded-eids exclude-eids include-eids]}]
  (let [some-enums-shown? (not-every? (comp (excluded-eids) :db/id) enums)]
    [:div.p-3.border-b.border-t.border-gray-500
     [:div.flex.items-center.stack-mx-2.cursor-pointer
      (toggle-handlers #(if some-enums-shown?
                          (exclude-eids enums)
                          (include-eids enums)))
      [toggle-span some-enums-shown?]
      [:span "Show enums?"]]]))

(defn- svg-to-blob [svg]
  (js/Blob. #js [svg] #js {:type "image/svg+xml"}))

(defn- download [dot-s]
  [:div.p-3
   [:button
    {:type     "button"
     :on-click (fn [_e]
                 (p/let [svg (graphviz/graphviz.dot dot-s)]
                   (file-saver/saveAs (svg-to-blob svg) "erd.svg")))}
    [:div.flex.items-center.group
     [:span.mr-1 "Export SVG"]
     download-icon]]])

(defn- erd-config [references config-state dot-s]
  (let [{enums-and-attrs      :enum
         aggregates-and-attrs :aggregate}
        (->> references
             colls-with-attrs
             (group-by (comp :db.schema.collection/type first)))]
    [config-dropdown config-state
     [:div.absolute.mt-2.rounded-md.shadow-lg.overflow-hidden.origin-top-left.left-0.bg-white.text-xs.leading-5.text-gray-700.whitespace-no-wrap
      [download dot-s]
      [config-attr-visibility config-state]
      [:div.stack-border-y
       (for [[aggregate _attrs :as aggregate-and-attrs] aggregates-and-attrs]
         ^{:key (:db/id aggregate)}
         [erd-collection-config aggregate-and-attrs config-state])]
      (when-let [enums (seq (map first enums-and-attrs))]
        [:<>
         [config-enum-visibility enums config-state]
         [:div.stack-border-y
          (for [[enum _attrs :as enum-and-attrs] enums-and-attrs]
            ^{:key (:db/id enum)}
            [erd-collection-config enum-and-attrs config-state])]])]]))

(defn erd [_]
  (let [config-state (merge (excluded-eid-state)
                            (dropdown-state)
                            (attrs-visible-state))]
    (fn [references]
      (when (seq references)
        (let [dot-s (dot-graph references config-state)]
          [:div
           [erd-config references config-state dot-s]
           [graphviz-svg dot-s]])))))

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
        entities        (ds/pull-many db '[*] eids)
        entities-by-eid (zipmap (map :db/id entities)
                                entities)]
    (walk/postwalk-replace entities-by-eid refs)))

(defn q-colls [db]
  (let [refs (ds/q active-ref-q db)]
    (q-expand-eids db refs)))

(defn q-coll [db coll]
  (let [coll-eid (ds/q '[:find ?coll .
                         :in $ ?collection-type ?collection-name
                         :where
                         [?coll :db.schema.collection/type ?collection-type]
                         [?coll :db.schema.collection/name ?collection-name]
                         [?coll :db.schema.pseudo/type :collection]]
                       db (:db.schema.collection/type coll) (:db.schema.collection/name coll))
        sources  (ds/q (concat active-ref-q '[:in $ ?source])
                       db coll-eid)
        dests    (ds/q (concat active-ref-q '[:in $ ?dest])
                       db coll-eid)
        refs     (distinct (concat sources dests))]
    (q-expand-eids db refs)))

(defn q-attr [db attr]
  (let [attr-eid (:db/id (ds/pull db [:db/id] (:db/ident attr)))
        refs     (ds/q (concat ref-q '[:in $ ?source-attr])
                       db attr-eid)]
    (q-expand-eids db refs)))
