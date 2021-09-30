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

(def ^:private collection-pull
  ['*
   {[:db.schema/_part-of :as :db.schema.collection/attributes]
    ['*
     {:db.schema/references       ['*]
      :db.schema/tuple-references ['*
                                   {:db.schema/references ['*]}]}]

    [:db.schema/_references :as :db.schema.collection/referenced-by-attrs]
    ;; could actually be an attr or a tuple that references the collection
    (into #_attr util/attr-link-pull
          ;; Reverse lookups always come back as vectors. I think it's true to
          ;; say that a db.schema/tuple-references belong to one and only one
          ;; attribute. If that's so, a better name for this attribute would be
          ;; :db.schema.tuple/attr, presuming it would be updated by a `first`
          ;; after being fetched. But since I'm not 100% sure it's singular,
          ;; better to treat it as a vector of attributes. See also
          ;; `flatten-tuple-attrs`.
          #_tuple [{[:db.schema/_tuple-references :as :db.schema.tuple/attrs]
                    util/attr-link-pull}])}])

(defn flatten-tuple-attrs [attrs-or-tuples]
  (mapcat (fn [attr-or-tuple]
            (or (:db.schema.tuple/attrs attr-or-tuple)
                [attr-or-tuple]))
          attrs-or-tuples))

(defn- attribute-comparable
  "Helper for sorting attributes. Returns items in this order:
  * Unique attributes
  * Deprecated unique attributes (rare)
  * Regular attributes
  * Deprecated attributes

  Further sorts alphabetically within each group."
  [{:keys [db.schema/deprecated? db/unique db/ident]}]
  [(not= :db.unique/identity unique) deprecated? ident])

(defn- by-type-and-name [db collection-type collection-name]
  (-> (ds/pull db collection-pull (eid-by-type-and-name db collection-type collection-name))
      (update :db.schema.collection/referenced-by-attrs flatten-tuple-attrs)
      (update :db.schema.collection/referenced-by-attrs #(sort-by :db/ident %))
      (update :db.schema.collection/attributes #(sort-by attribute-comparable %))))

(defn collection-from-route
  [collection-type parameters]
  (by-type-and-name db/db collection-type (keyword (:id (:path parameters)))))

(def ^:private chevron-right
  [:svg.fill-none.stroke-current.stroke-2.w-4.h-4 {:viewBox "0 0 24 24"}
   [:path {:stroke-linecap "round" :stroke-linejoin "round" :d "M9 5l7 7-7 7"}]])

(defn doc-str [{:keys [db/doc]}]
  (when doc
    [:p.text-gray-600.italic.col-span-2.hidden.sm:block doc]))

(defn- attribute-header [{:keys [db/ident db/unique db.schema/deprecated?] :as attribute} coll-type]
  [:h1.flex.items-center.space-x-2
   [:a.group-hover:underline.focus:outline-none.focus:underline
    {:href (util/attr-href attribute)}
    [util/ident-name {:ident-props {:class [:font-semibold]}}
     ident coll-type]]
   (when (= :db.unique/identity unique)
     util/lock-closed)
   (when deprecated?
     [:span.font-medium
      util/deprecated-pill])])

(defmulti attribute-panel :db.schema.pseudo/type)

(defmethod attribute-panel :attribute [attribute]
  [:div.grid.sm:grid-cols-2.gap-x-4.sm:gap-y-2
   [attribute-header attribute :aggregate]
   [:div.sm:text-right [value-type/shorthand attribute]]
   [doc-str attribute]])

(defmethod attribute-panel :constant [constant]
  [:div.grid.sm:grid-cols-2.gap-x-4.sm:gap-y-2
   [attribute-header constant :enum]
   [doc-str constant]])

(defn page [{:keys [db.schema.collection/attributes db.schema.collection/referenced-by-attrs db/doc] :as coll}]
  [:div.space-y-6
   [:div.px-4.sm:px-0.space-y-6
    [:h1.whitespace-nowrap
     [util/coll-name* {:class [:font-bold]} coll]
     " "
     [util/aggregate-abbr coll]]
    (when doc
      [:p doc])
    (when (seq referenced-by-attrs)
      [:div.text-gray-600 "Referenced by " [util/attr-links referenced-by-attrs]])]
   [:div.sm:shadow-lg.overflow-hidden.sm:rounded-lg.bg-white.max-w-4xl.divide-y
    (for [attribute attributes]
      ^{:key (:db/id attribute)}
      [:section
       {:on-click #(util/visit (util/attr-route attribute))
        :class    [:px-4              :sm:px-6
                   :py-2              :sm:py-4
                   :flex
                   :items-center
                   :justify-between
                   :space-x-4         :sm:space-x-6
                   :group
                   :cursor-pointer
                   :transition-colors
                   (if (:db.schema/deprecated? attribute)
                     :bg-gray-300
                     :hover:bg-gray-100)]}
       [:div.flex-1 [attribute-panel attribute]]
       chevron-right])]
   [:div
    ^{:key (:db/id coll)}
    [diagrams/erd (diagrams.query/coll-edges db/db coll)]]])
