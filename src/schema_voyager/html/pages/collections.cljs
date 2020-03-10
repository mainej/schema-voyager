(ns schema-voyager.html.pages.collections
  (:require [re-posh.core :as rp]
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
   (for [{:keys [db/id db.schema.collection/name]} collection]
     ^{:key id}
     [:li name])])

(defn page []
  [:div
   [:h1 "aggregates"]
   [collection-list @(rp/subscribe [::aggregates])]
   [:h1 "enums"]
   [collection-list @(rp/subscribe [::enums])]])
