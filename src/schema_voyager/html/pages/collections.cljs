(ns schema-voyager.html.pages.collections
  (:require [re-posh.core :as rp]
            [schema-voyager.html.util :as util :refer [<sub >dis]]
            [re-frame.core :as re-frame]))

(rp/reg-sub
 ::aggregate-eids
 (fn [_ _]
   {:type  :query
    :query '[:find [?coll ...]
             :where [?coll :db.schema.collection/type :aggregate]]}))

(rp/reg-sub
 ::enum-eids
 (fn [_ _]
   {:type  :query
    :query '[:find [?coll ...]
             :where [?coll :db.schema.collection/type :enum]]}))

(rp/reg-sub
 ::aggregates-pull
 :<- [::aggregate-eids]
 (fn [aggregate-eids]
   {:type    :pull-many
    :pattern '[*]
    :ids     aggregate-eids}))

(rp/reg-sub
 ::enums-pull
 :<- [::enum-eids]
 (fn [enum-eids]
   {:type    :pull-many
    :pattern '[*]
    :ids     enum-eids}))

(defn sort-by-collection-name [collection _]
  (sort-by :db.schema.collection/name collection))

(re-frame/reg-sub ::aggregates :<- [::aggregates-pull] sort-by-collection-name)
(re-frame/reg-sub ::enums :<- [::enums-pull] sort-by-collection-name)

(defn collection-list [collection]
  [:ul
   (for [{:keys [db/id] :as coll} collection]
     ^{:key id}
     [:li
      [util/link {:href (util/coll-href coll)}
       [util/coll-name* coll]]])])

(defn list-section [title description collections]
  [:div.md:flex.mb-6
   [:div.md:w-1of4
    [:h1.small-caps title]
    [:p.my-4.font-light.text-gray-700.mr-4 description]]
   [:div.md:w-3of4
    [collection-list collections]]])

(defn page []
  [:div.px-4.sm:px-0
   [list-section
    "Aggregates"
    "Aggregates are collections of attributes that often co-exist on an entity. They are analogous to a SQL table, though some attributes may appear on many aggregates."
    (<sub [::aggregates])]
   [list-section
    "Enums"
    "Enums are collections of named constants. They usually specify the various values that an attribute may take."
    (<sub [::enums])]])
