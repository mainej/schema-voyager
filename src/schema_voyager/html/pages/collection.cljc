(ns schema-voyager.html.pages.collection
  (:require [schema-voyager.html.db :as db]
            [datascript.core :as d]
            [schema-voyager.html.components.entity :as entity]
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
  [{:keys [db.schema/deprecated? db/unique db/ident]}]
  [(not= :db.unique/identity unique) deprecated? ident])

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
      [entity/panel entity])]])
