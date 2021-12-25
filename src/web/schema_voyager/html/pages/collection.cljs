(ns schema-voyager.html.pages.collection
  (:require ["@heroicons/react/outline/ChevronRightIcon" :as ChevronRightIcon]
            [schema-voyager.html.db :as db]
            [schema-voyager.html.components.value-type :as value-type]
            [schema-voyager.html.diagrams.core :as diagrams]
            [schema-voyager.html.util :as util]))

(defn collection-from-route
  [collection-type parameters]
  (db/collection-by-type-and-name collection-type (keyword (:id (:path parameters)))))

(def ^:private chevron-right
  [:> ChevronRightIcon {:class [:stroke-2 :w-4 :h-4]}])

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
     util/primary-key)
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
    [diagrams/erd (db/coll-edges coll)]]])
