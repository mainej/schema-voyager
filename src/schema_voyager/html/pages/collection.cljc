(ns schema-voyager.html.pages.collection
  (:require [re-posh.core :as rp]
            [schema-voyager.html.components.entity :as entity]
            [schema-voyager.html.util :refer [<sub] :as util]))

(rp/reg-query-sub
 ::eid-by-type-and-name
 '[:find ?collection .
   :in $ ?collection-type ?collection-name
   :where
   [?collection :db.schema.collection/type ?collection-type]
   [?collection :db.schema.collection/name ?collection-name]])

(rp/reg-sub
 ::by-type-and-name
 (fn [[_ collection-type collection-name]]
   (rp/subscribe [::eid-by-type-and-name collection-type collection-name]))
 (fn [eid _]
   {:type    :pull
    :pattern ['*
              {:db.schema/_references util/attr-link-pull
               :db.schema/_part-of    ['*
                                       {:db.schema/references ['*]}]}]
    :id      eid}))

(defn collection-from-route
  [collection-type parameters]
  (<sub [::by-type-and-name collection-type (keyword (:id (:path parameters)))]))

(defn entity-comparable
  [{:keys [db.schema/deprecated? db/unique db/ident]}]
  [(not= :db.unique/identity unique) deprecated? ident])

(defn page [{:keys [db.schema/_part-of db.schema/_references] :as coll}]
  [:div
   [:div.px-4.sm:px-0
    [:h1.mb-4.font-bold
     [util/coll-name* coll]
     " "
     [util/aggregate-abbr coll]]
    (when (seq _references)
      [:div.text-gray-600 "Referenced by "
       [util/attr-links _references]])]
   [:div.mt-6.sm:shadow-lg.sm:rounded-lg.bg-white.max-w-4xl
    (for [entity (sort-by entity-comparable _part-of)]
      ^{:key (:db/id entity)}
      [entity/panel entity])]])
