(ns schema-voyager.html.diagrams.config
  (:require [reagent.core :as r]
            ["file-saver" :as file-saver]
            ["@heroicons/react/outline/CogIcon" :as CogIcon]
            ["@heroicons/react/outline/DocumentDownloadIcon" :as DocumentDownloadIcon]
            ["@heroicons/react/outline/ArrowsExpandIcon" :as ArrowsExpandIcon]
            [headlessui-reagent.core :as ui]
            [schema-voyager.html.util :as util]
            [schema-voyager.html.components.toggle :as toggle]
            [schema-voyager.html.diagrams.util :as diagrams.util]))

(defn- set-toggle [s item]
  (if (contains? s item)
    (disj s item)
    (conj s item)))

(def ^:private default-filters
  {:attrs-visible? true
   :excluded-eids  #{}})

(defonce state (r/atom default-filters))
(defn reset-filters [] (reset! state default-filters))

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
  [:> CogIcon {:class [:h-6 :w-6 :stroke-2]}])

(defn- clear-filters []
  [:div.flex.justify-between
   [:h2.font-semibold.p-3 "Filters"]
   [:button.p-3.hover:underline.focus:outline-none.focus:underline
    {:type     "button"
     :on-click reset-filters}
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

(defn- svg-to-blob [^js/Element svg]
  (js/Blob. #js [(.-outerHTML svg)] #js {:type "image/svg+xml"}))

(defn- icon-button [text icon props]
  [:button.p-3.focus:outline-none.group
   (assoc props :type "button")
   [:div.flex.items-center.gap-2
    text
    [:> icon {:class [:inline-block :w-4 :h-4
                      :text-teal-500
                      :rounded-sm :stroke-2 :transition-colors
                      :group-focus-visible:ring-2 :group-focus-visible:ring-teal-400
                      :group-hover:text-teal-400]}]]])

(defn- download [dot-s]
  [icon-button "Export SVG" DocumentDownloadIcon
   {:on-click (fn [_e]
                (diagrams.util/with-dot-to-svg dot-s
                  #(file-saver/saveAs (svg-to-blob %) "erd.svg")))}])

(defn- fit-screen-button
  "graphviz renders the SVG at a legible size, even if that means it has to be
  wider than the screen. But sometimes it's nice to shrink wide SVGs to fit on
  screen, even if they become less legible. This will also enlarge small SVGs,
  which is less desirable, but still OK."
  []
  [icon-button "Fit Screen" ArrowsExpandIcon
   {:on-click diagrams.util/toggle-fit-screen}])

(defn config [nodes dot-s]
  (let [{enums      :enum
         aggregates :aggregate}
        (group-by :db.schema.collection/type nodes)]
    [dropdown
     [:div.absolute.mt-2.rounded-md.shadow-lg.overflow-hidden.origin-top-left.left-0.bg-white.text-xs.leading-5.text-gray-700.whitespace-nowrap
      [:div.divide-y.divide-gray-500
       [:div.flex.justify-between
        [download dot-s]
        [fit-screen-button]]
       [clear-filters]
       [attr-visibility]
       [collections-inclusion aggregates]
       (when (seq enums)
         [:<>
          [enum-inclusion enums]
          [collections-inclusion enums]])]]]))
