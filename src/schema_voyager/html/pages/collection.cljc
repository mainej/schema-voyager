(ns schema-voyager.html.pages.collection
  (:require [schema-voyager.html.db :as db]
            [datascript.core :as d]
            [schema-voyager.html.components.entity :as entity]
            [schema-voyager.html.diagrams.collection :as diagrams.collection]
            [schema-voyager.html.util :as util]))

(defn eid-by-type-and-name [db collection-type collection-name]
  (d/q '[:find ?collection .
         :in $ ?collection-type ?collection-name
         :where
         [?collection :db.schema.collection/type ?collection-type]
         [?collection :db.schema.collection/name ?collection-name]]
       db collection-type collection-name))

(defn by-type-and-name [db collection-type collection-name]
  (d/pull db
          ['*
           {:db.schema/_references util/attr-link-pull
            :db.schema/_part-of    ['*
                                    {:db.schema/references ['*]}]}]
          (eid-by-type-and-name db collection-type collection-name)))

(defn collection-from-route
  [collection-type parameters]
  (by-type-and-name db/db collection-type (keyword (:id (:path parameters)))))

(defn entity-comparable
  "Helper for sorting attributes. Returns items in this order:
  * Unique attributes
  * Deprecated unique attributes (rare)
  * Regular attributes
  * Deprecated attributes

  Further sorts alphabetically within each group."
  [{:keys [db.schema/deprecated? db/unique db/ident]}]
  [(not= :db.unique/identity unique) deprecated? ident])

(def chevron-right
  [:svg.fill-none.stroke-current.stroke-2.w-4.h-4 {:viewBox "0 0 24 24"}
   [:path {:stroke-linecap "round" :stroke-linejoin "round" :d "M9 5l7 7-7 7"}]])

(defn value-type-shorthand [{:keys [db/cardinality] :as entity}]
  (let [many? (= :db.cardinality/many cardinality)]
    [:span
     (when many? "[")
     [entity/value-type entity]
     (when many? "]")]))

(defn entity-header [{:keys [db/ident] :as entity} coll-type]
  [:h1.mb-4
   [:a.underline
    {:href (util/attr-href entity)}
    [util/ident-name ident coll-type]]
   [entity/unique-span entity]
   [entity/deprecated-span entity]])

(defmulti entity-panel entity/entity-type)

(defmethod entity-panel :attribute [entity]
  [:div.sm:flex
   [:div.sm:w-4of6
    [:div.font-medium
     [entity-header entity :aggregate]]
    [:div.hidden.sm:block.mt-4
     [entity/doc-str entity]]]
   [:div.flex-grow.sm:text-right.mt-4.sm:mt-0
    [value-type-shorthand entity]]])

(defmethod entity-panel :constant [entity]
  [:div
   [:div.font-medium
    [entity-header entity :enum]]
   [:div.hidden.sm:block.mt-4
    [entity/doc-str entity]]])

(defn page [{:keys [db.schema/_part-of db.schema/_references db/doc] :as coll}]
  [:div
   [:div.px-4.sm:px-0
    [:h1.mb-4.font-bold
     [util/coll-name* coll]
     " "
     [util/aggregate-abbr coll]]
    (when doc
      [:p doc])
    (when (seq _references)
      [:div.text-gray-600 "Referenced by "
       [util/attr-links _references]])]
   [:div.mt-6.sm:shadow-lg.sm:rounded-lg.bg-white.max-w-4xl
    (for [entity (sort-by entity-comparable _part-of)]
      ^{:key (:db/id entity)}
      [:section.border-b
       {:class (when (:db.schema/deprecated? entity)
                 :bg-gray-300)}
       [:div.p-4.sm:p-6.flex.items-center.justify-center.cursor-pointer.hover:bg-gray-100.transition.duration-300.ease-in-out
        {:on-click #(util/visit (util/attr-route entity))}
        [:div.flex-grow [entity-panel entity]]
        [:div.ml-4.sm:ml-6 chevron-right]]])]
   [:div.mt-4
    [diagrams.collection/force-graph
     (concat
      (mapcat #(for [source (:db.schema/part-of %)]
                 [source coll])
              _references)
      (mapcat #(for [target (:db.schema/references %)]
                 [coll target])
              _part-of))]]])
