(ns schema-voyager.html.pages.collection
  (:require [re-posh.core :as rp]
            [schema-voyager.html.components.attributes :as attributes]
            [schema-voyager.html.components.entity :as entity]
            [schema-voyager.html.util :refer [<sub] :as util]))

(rp/reg-query-sub
 ::collection-eid-by-type-and-name
 '[:find ?collection .
   :in $ ?collection-type ?collection-name
   :where
   [?collection :db.schema.collection/type ?collection-type]
   [?collection :db.schema.collection/name ?collection-name]])

(rp/reg-sub
 ::collection-by-type-and-name
 (fn [[_ collection-type collection-name]]
   (rp/subscribe [::collection-eid-by-type-and-name collection-type collection-name]))
 (fn [eid _]
   {:type    :pull
    :pattern ['*
              {:db.schema/_references util/attr-link-pull
               :db.schema/_part-of    ['*
                                       {:db.schema/references ['*]
                                        :db.schema/see-also util/attr-link-pull}]}]
    :id      eid}))

(defn collection-from-route
  [collection-type parameters]
  (<sub [::collection-by-type-and-name collection-type (keyword (:id (:path parameters)))]))

(defn page [{:keys [db.schema.collection/name db.schema.collection/type db.schema/_part-of db.schema/_references]}]
  [:div
   [:h1
    (pr-str name)
    " "
    "("
    type
    ")"]
   (when (seq _references)
     [:div "Referenced by"
      [attributes/links _references]])
   [:div
    (for [entity _part-of]
      ^{:key (:db/id entity)}
      [entity/panel entity])]])
