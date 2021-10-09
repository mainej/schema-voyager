(ns schema-voyager.html.diagrams.config
  (:require [reagent.core :as r]
            ["file-saver" :as file-saver]
            [headlessui-reagent.core :as ui]
            [schema-voyager.html.util :as util]
            [schema-voyager.html.components.toggle :as toggle]
            [schema-voyager.html.diagrams.util :as diagrams.util]))

(defn- set-toggle [s item]
  (if (contains? s item)
    (disj s item)
    (conj s item)))

(def ^:private default-state
  {:attrs-visible? true
   :excluded-eids  #{}})

(defonce state (r/atom default-state))
(defn reset-state [] (reset! state default-state))

(def ^:private !attrs-visible? (r/cursor state [:attrs-visible?]))
(defn attrs-visible? [] @!attrs-visible?)
(defn- toggle-attrs-visible [] (r/rswap! !attrs-visible? not))

(def ^:private !excluded-eids (r/cursor state [:excluded-eids]))
(defn- excluded-eids [] @!excluded-eids)

(defn- excluded-eid? [eid]
  (contains? @(r/track excluded-eids) eid))

(defn- excluded-entity? [{:keys [db/id]}]
  @(r/track excluded-eid? id))

(defn- included-entity? [entity]
  (not @(r/track excluded-entity? entity)))

(defn- some-entities-included? [entities]
  (some #(deref (r/track included-entity? %)) entities))

(defn some-entities-excluded? [entities]
  (some #(deref (r/track excluded-entity? %)) entities))

(defn- toggle-entity [exclusions entity] (set-toggle exclusions (:db/id entity)))
(defn- exclude-entities [exclusions entities] (apply conj exclusions (map :db/id entities)))
(defn- include-entities [exclusions entities] (apply disj exclusions (map :db/id entities)))
(defn- swap-exclusions [f & args] (apply r/rswap! !excluded-eids f args))

(def ^:private gear-icon
  [:svg.h-6.w-6.fill-none.stroke-current.stroke-2
   {:viewBox         "0 0 24 24"
    :stroke-linejoin "round"
    :stroke-linecap  "round"}
   [:title "Configure Diagram"]
   [:path {:d "M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"}]
   [:path {:d "M15 12a3 3 0 11-6 0 3 3 0 016 0z"}]])

(def ^:private download-icon
  [:svg.inline-block.w-4.h-4.fill-none.stroke-current.text-teal-500.group-focus:ring-2.group-focus:ring-teal-400.rounded-sm.group-hover:text-teal-400.stroke-2.transition-colors
   {:viewBox         "0 0 24 24"
    :stroke-linejoin "round"
    :stroke-linecap  "round"}
   [:title "Download"]
   [:path {:d "M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"}]])

(defn- clear-filters []
  [:div.flex.justify-between
   [:h2.font-semibold.p-3 "Filters"]
   [:button.p-3.hover:underline.focus:outline-none.focus:underline
    {:type     "button"
     :on-click reset-state}
    "clear all"]])

(defn- attr-visibility []
  (let [checked? @(r/track attrs-visible?)]
    [:fieldset.flex.items-center.space-x-2.p-3
     [toggle/toggle {:checked   checked?
                     :on-change toggle-attrs-visible}
      "Show attributes on aggregates?"]]))

(defn- attr-inclusion [coll attr]
  [:fieldset.flex.items-center.space-x-2
   [toggle/toggle {:checked   @(r/track included-entity? attr)
                   :on-change #(swap-exclusions toggle-entity attr)}
    [:span
     [:span.sr-only "Toggle inclusion of attribute "]
     [util/ident-name (:db/ident attr) (:db.schema.collection/type coll)]]]])

(defn- collection-inclusion [coll]
  [:fieldset.flex.items-center.space-x-2
   [toggle/toggle {:checked   @(r/track included-entity? coll)
                   :on-change #(swap-exclusions toggle-entity coll)}
    [:span
     [:span.sr-only "Toggle inclusion of "]
     [util/coll-name coll]]]])

(defn- enum-inclusion [enums]
  (let [some-enums-included? @(r/track some-entities-included? enums)
        toggle-entities      (if some-enums-included? exclude-entities include-entities)]
    [:fieldset.flex.items-center.space-x-2.p-3
     [toggle/toggle {:checked   some-enums-included?
                     :on-change #(swap-exclusions toggle-entities enums)}
      "Include enums?"]]))

(defn- collections-inclusion [colls]
  (let [attrs-visible? @(r/track attrs-visible?)]
    [:div.divide-y
     (for [coll colls
           :let [attrs (:db.schema.collection/attributes coll)]]
       ^{:key (:db/id coll)}
       [:div.p-3.space-y-2
        [collection-inclusion coll]
        (when (and (seq attrs) attrs-visible?)
          [:div.ml-4.space-y-2
           (for [attr attrs]
             ^{:key (:db/id attr)}
             [attr-inclusion coll attr])])])]))

(defn- dropdown [body]
  [ui/popover {:class [:relative]}
   [ui/popover-button
    {:class [:p-2
             :rounded-md
             :transition-colors
             :bg-white
             :text-gray-700
             :hover:text-gray-500
             :focus:outline-none
             :focus:ring-2
             :focus:ring-gray-700
             :active:bg-gray-50
             :active:text-gray-800]}
    gear-icon]
   [ui/popover-overlay {:class [:bg-gray-900 :fixed :inset-0 :opacity-50]}]
   [ui/popover-panel body]])

(defn- svg-to-blob [svg]
  (js/Blob. #js [svg] #js {:type "image/svg+xml"}))

(defn- download [dot-s]
  [:button.p-3.focus:outline-none.group
   {:type     "button"
    :on-click (fn [_e]
                (diagrams.util/with-dot-to-svg dot-s
                  #(file-saver/saveAs (svg-to-blob %) "erd.svg")))}
   [:div.flex.items-center
    [:span.mr-1 "Export SVG"]
    download-icon]])

(defn config [nodes dot-s]
  (let [{enums      :enum
         aggregates :aggregate}
        (group-by :db.schema.collection/type nodes)]
    [dropdown
     [:div.absolute.mt-2.rounded-md.shadow-lg.overflow-hidden.origin-top-left.left-0.bg-white.text-xs.leading-5.text-gray-700.whitespace-nowrap
      [:div.divide-y.divide-gray-500
       [download dot-s]
       [clear-filters]
       [attr-visibility]
       [collections-inclusion aggregates]
       (when (seq enums)
         [:<>
          [enum-inclusion enums]
          [collections-inclusion enums]])]]]))
