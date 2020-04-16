(ns schema-voyager.html.diagrams.config
  (:require [reagent.core :as r]
            ["file-saver" :as file-saver]
            [schema-voyager.html.util :as util]
            [schema-voyager.html.components.toggle :as toggle]
            [schema-voyager.html.diagrams.util :as diagrams.util]))

(defn- set-toggle [s item]
  (if (contains? s item)
    (disj s item)
    (conj s item)))

(defn- attrs-visible-state
  "Toggle whether attribute names are shown below the collection names."
  []
  (let [!attrs-visible? (r/atom true)]
    {:attrs-visible?       #(deref !attrs-visible?)
     :attrs-visible-toggle #(swap! !attrs-visible? not)}))

(defn- dropdown-state
  "Toggle whether the config dropdown is open."
  []
  (let [!open? (r/atom false)]
    {:dropdown-open?  #(deref !open?)
     :dropdown-close  #(reset! !open? false)
     :dropdown-toggle #(swap! !open? not)}))

(defn- excluded-eid-state
  "Set of eids of collections or attributes that are currently being ignored.
  Includes tools for excluding or including many eids at once, which is used to
  disable or enable all the enums simultaneously."
  []
  (let [!excluded-eids (r/atom #{})]
    {:excluded-eids #(deref !excluded-eids)
     :exclude-eids  (fn [eids]
                      (swap! !excluded-eids #(apply conj % eids)))
     :include-eids  (fn [eids]
                      (swap! !excluded-eids #(apply disj % eids)))
     :toggle-eid    (fn [eid]
                      (swap! !excluded-eids set-toggle eid))}))
(defn state []
  (merge (excluded-eid-state)
         (dropdown-state)
         (attrs-visible-state)))

(def ^:private gear-icon
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

(defn- collection [{:keys [excluded-eids toggle-eid attrs-visible?]} [coll attrs]]
  (let [excluded-entity? (comp (excluded-eids) :db/id)]
    [:div.p-3.stack-my-2
     [:div.flex.items-center.stack-mx-2.cursor-pointer
      (toggle/handlers #(toggle-eid (:db/id coll)))
      [toggle/span {:checked    (not (excluded-entity? coll))
                    :aria-label (str "Toggle inclusion of " (:db.schema.collection/type coll) " " (:db.schema.collection/name coll))}]
      [util/coll-name coll]]
     (when (and (attrs-visible?) (seq attrs))
       [:div.ml-4.stack-my-2
        (for [attr attrs]
          ^{:key (:db/id attr)}
          [:div.flex.items-center.stack-mx-2.cursor-pointer
           (toggle/handlers #(toggle-eid (:db/id attr)))
           [toggle/span {:checked    (not (excluded-entity? attr))
                         :aria-label (str "Toggle inclusion of attribute " (pr-str (:db/ident attr)))}]
           [util/ident-name (:db/ident attr) (:db.schema.collection/type coll)]])])]))

(defn- collections [config-state colls-and-attrs]
  [:div.stack-border-y
   (for [[coll _attrs :as coll-and-attrs] colls-and-attrs]
     ^{:key (:db/id coll)}
     [collection config-state coll-and-attrs])])

(defn- dropdown [{:keys [dropdown-open? dropdown-close dropdown-toggle]} body]
  [:div.relative.inline-block.ml-4.sm:ml-0
   [:button.rounded-md.border.p-2.bg-white.text-gray-700.hover:text-gray-500.focus:outline-none.focus:border-blue-300.focus:shadow-outline-blue.active:bg-gray-50.active:text-gray-800.transition.ease-in-out.duration-150
    {:type     "button"
     :on-click dropdown-toggle}
    gear-icon]
   (when (dropdown-open?)
     [:<>
      [:div.fixed.inset-0.bg-gray-900.opacity-50
       {:on-click dropdown-close}]
      body])])

(defn- attr-visibility [{:keys [attrs-visible? attrs-visible-toggle]}]
  [:div.p-3.border-b.border-t.border-gray-500
   [:div.flex.items-center.stack-mx-2.cursor-pointer
    (toggle/handlers attrs-visible-toggle)
    [toggle/span {:checked    (attrs-visible?)
                  :aria-label "Toggle visibility of attributes"}]
    [:span "Show attributes on aggregates?"]]])

(defn- enum-visibility [{:keys [excluded-eids exclude-eids include-eids]} enums]
  (let [some-enums-shown? (not-every? (comp (excluded-eids) :db/id) enums)
        toggle-eids       (if some-enums-shown? exclude-eids include-eids)]
    [:div.p-3.border-b.border-t.border-gray-500
     [:div.flex.items-center.stack-mx-2.cursor-pointer
      (toggle/handlers #(toggle-eids (map :db/id enums)))
      [toggle/span {:checked    some-enums-shown?
                    :aria-label "Toggle inclusion of enums"}]
      [:span "Include enums?"]]]))

(defn- svg-to-blob [svg]
  (js/Blob. #js [svg] #js {:type "image/svg+xml"}))

(defn- download [dot-s]
  [:button.p-3
   {:type     "button"
    :on-click (fn [_e]
                (diagrams.util/with-dot-to-svg dot-s
                  #(file-saver/saveAs (svg-to-blob %) "erd.svg")))}
   [:div.flex.items-center.group
    [:span.mr-1 "Export SVG"]
    download-icon]])

(defn config [config-state references dot-s]
  (let [{enums-and-attrs      :enum
         aggregates-and-attrs :aggregate}
        (->> references
             diagrams.util/colls-with-attrs
             (group-by (comp :db.schema.collection/type first)))]
    [dropdown config-state
     [:div.absolute.mt-2.rounded-md.shadow-lg.overflow-hidden.origin-top-left.left-0.bg-white.text-xs.leading-5.text-gray-700.whitespace-no-wrap
      [download dot-s]
      [attr-visibility config-state]
      [collections config-state aggregates-and-attrs]
      (when-let [enums (seq (map first enums-and-attrs))]
        [:<>
         [enum-visibility config-state enums]
         [collections config-state enums-and-attrs]])]]))
