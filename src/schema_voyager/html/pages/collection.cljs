(ns schema-voyager.html.pages.collection
  (:require [schema-voyager.html.db :as db]
            [datascript.core :as ds]
            [schema-voyager.html.components.value-type :as value-type]
            [schema-voyager.html.diagrams.core :as diagrams]
            [schema-voyager.html.diagrams.query :as diagrams.query]
            [schema-voyager.html.util :as util]))

(defn- eid-by-type-and-name [db collection-type collection-name]
  (ds/q '[:find ?collection .
          :in $ ?collection-type ?collection-name
          :where
          [?collection :db.schema.collection/type ?collection-type]
          [?collection :db.schema.collection/name ?collection-name]
          [?collection :db.schema.pseudo/type :collection]]
        db collection-type collection-name))

(defn- by-type-and-name [db collection-type collection-name]
  (let [collection    (ds/pull db
                               ['*
                                {:db.schema/_references (into util/attr-link-pull
                                                              [{:db.schema/_tuple-references util/attr-link-pull}])
                                 :db.schema/_part-of    ['*
                                                         {:db.schema/references       ['*]
                                                          :db.schema/tuple-references ['*
                                                                                       {:db.schema/references ['*]}]}]}]
                               (eid-by-type-and-name db collection-type collection-name))
        referenced-by (sort-by :db/ident
                               (concat (->> collection
                                            :db.schema/_references
                                            (remove :db.schema/_tuple-references))
                                       (->> collection
                                            :db.schema/_references
                                            (mapcat :db.schema/_tuple-references))))]
    (assoc collection :db.schema.pseudo/referenced-by referenced-by)))

(defn collection-from-route
  [collection-type parameters]
  (by-type-and-name db/db collection-type (keyword (:id (:path parameters)))))

(defn- attribute-comparable
  "Helper for sorting attributes. Returns items in this order:
  * Unique attributes
  * Deprecated unique attributes (rare)
  * Regular attributes
  * Deprecated attributes

  Further sorts alphabetically within each group."
  [{:keys [db.schema/deprecated? db/unique db/ident]}]
  [(not= :db.unique/identity unique) deprecated? ident])

(def ^:private chevron-right
  [:svg.fill-none.stroke-current.stroke-2.w-4.h-4 {:viewBox "0 0 24 24"}
   [:path {:stroke-linecap "round" :stroke-linejoin "round" :d "M9 5l7 7-7 7"}]])

(defn doc-str [{:keys [db/doc]}]
  (when doc
    [:p.text-gray-600.italic.hidden.sm:block doc]))

(defn- attribute-header [{:keys [db/ident db/unique db.schema/deprecated?] :as attribute} coll-type]
  [:h1.font-medium.flex.items-center.space-x-2
   [:a.group-hover:underline
    {:href (util/attr-href attribute)}
    [util/ident-name ident coll-type]]
   (when (= :db.unique/identity unique)
     util/lock-closed)
   (when deprecated?
     util/deprecated-pill)])

(defmulti attribute-panel :db.schema.pseudo/type)

(defmethod attribute-panel :attribute [attribute]
  [:div.space-y-4
   [:div.sm:flex.justify-between.space-y-4.sm:space-y-0.sm:space-x-4
    [attribute-header attribute :aggregate]
    [:div [value-type/shorthand attribute]]]
   [doc-str attribute]])

(defmethod attribute-panel :constant [constant]
  [:div.space-y-4
   [attribute-header constant :enum]
   [doc-str constant]])

(defn page [{:keys [db.schema/_part-of db.schema.pseudo/referenced-by db/doc] :as coll}]
  [:div.space-y-6
   [:div.px-4.sm:px-0.space-y-6
    [:h1.font-bold.whitespace-nowrap
     [util/coll-name* coll]
     " "
     [util/aggregate-abbr coll]]
    (when doc
      [:p doc])
    (when (seq referenced-by)
      [:div.text-gray-600 "Referenced by "
       [util/attr-links referenced-by]])]
   [:div.sm:shadow-lg.overflow-hidden.sm:rounded-lg.bg-white.max-w-4xl.divide-y
    (for [attribute (sort-by attribute-comparable _part-of)]
      ^{:key (:db/id attribute)}
      [:section
       {:class (when (:db.schema/deprecated? attribute)
                 :bg-gray-300)}
       [:div
        {:on-click #(util/visit (util/attr-route attribute))
         :class [:p-4              :sm:p-6
                 :flex
                 :items-center
                 :justify-between
                 :space-x-4        :sm:space-x-6
                 :group
                 :cursor-pointer
                 :transition-colors
                 :hover:bg-gray-100]}
        [:div.flex-1 [attribute-panel attribute]]
        chevron-right]])]
   [:div
    ^{:key (:db/id coll)}
    [diagrams/erd (diagrams.query/coll db/db coll)]]])
