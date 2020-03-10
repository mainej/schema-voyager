(ns schema-voyager.html.components.entity
  (:require [schema-voyager.html.util :as util]))

(def chevron-right
  [:svg.fill-none.stroke-current.stroke-2.w-4.h-4 {:viewBox "0 0 24 24"}
   [:path {:stroke-linecap "round" :stroke-linejoin "round" :d "M9 5l7 7-7 7"}]])

(def lock-closed
  [:svg.inline.fill-none.stroke-current.stroke-2.w-4.h-4 {:viewBox "0 0 24 24"}
   [:title ":db.unique/identity"]
   [:path {:stroke-linecap "round" :stroke-linejoin "round" :d "M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"}]])

(defn doc-str [{:keys [db/doc]}]
  (when-let [doc-str doc]
    [:p.mt-4.text-gray-600 doc-str]))

(defn value-type [{:keys [db/valueType db/cardinality db.schema/references]}]
  (let [many? (= :db.cardinality/many cardinality)]
    [:p
     (when many? "[")
     (if (seq references)
       [util/coll-links references]
       valueType)
     (when many? "]")]))

(defn header [{:keys [db/ident db/unique db.schema/deprecated?]}]
  [:h1.mb-4
   (pr-str ident)
   (when (= :db.unique/identity unique)
     [:span.ml-2.text-purple-700 lock-closed])
   (when deprecated?
     [:span.ml-2.inline-block.px-2.rounded-full.bg-gray-400.text-xs "DEPRECATED"])])

(defmulti panel (fn [entity]
                  (if (:db/valueType entity)
                    :attribute
                    :constant)))

(defmethod panel :attribute [entity]
  [:section.border-b
   {:class (when (:db.schema/deprecated? entity)
             :bg-gray-300)}
   [:a.p-4.sm:p-6.flex.items-center.justify-center
    {:href (util/attr-href entity)}
    [:div.sm:flex.flex-grow
     [:div.sm:w-4of6
      [header entity]
      [:div.hidden.sm:block
       [doc-str entity]]]
     [:div.flex-grow.sm:text-right.mt-4.sm:mt-0
      [value-type entity]]]
    [:div.ml-4.sm:ml-6 chevron-right]]])

(defmethod panel :constant [entity]
  [:section.border-b
   {:class (when (:db.schema/deprecated? entity)
             :bg-gray-300)}
   [:a.p-4.sm:p-6.flex.items-center.justify-center
    {:href (util/attr-href entity)}
    [:div.flex-grow
     [header entity]
     [:div.hidden.sm:block
      [doc-str entity]]]
    [:div.ml-4.sm:ml-6 chevron-right]]])
