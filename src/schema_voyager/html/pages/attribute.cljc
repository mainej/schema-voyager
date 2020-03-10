(ns schema-voyager.html.pages.attribute
  (:require [re-posh.core :as rp]
            [schema-voyager.html.components.entity :as entity]
            [schema-voyager.html.util :refer [<sub] :as util]))

(rp/reg-query-sub
 ::eid-by-ident
 '[:find ?attr .
   :in $ ?ident
   :where
   [?attr :db/ident ?ident]])

(rp/reg-sub
 ::by-ident
 (fn [[_ ident]]
   (rp/subscribe [::eid-by-ident ident]))
 (fn [eid _]
   {:type    :pull
    :pattern util/attribute-pull
    :id      eid}))

(defn part-of [{:keys [db.schema/part-of]}]
  [:div "Part of "
   [util/coll-links part-of]])

(defn see-also-links [{:keys [db.schema/see-also]}]
  (when-let [see-also see-also]
    [:div "See also "
     [util/attr-links see-also]]))

(defn unhandled-fields [{:keys [db/unique] :as entity}]
  (cond-> (dissoc entity :db/id :db.schema/part-of :db.schema/see-also :db.schema/deprecated? :db/ident :db/doc)
    (= unique :db.unique/identity) (dissoc :db/unique)))

(defn additional-fields [entity but-fields]
  (when-let [fields (seq (apply dissoc (unhandled-fields entity) but-fields))]
    [:dl.mt-4.rounded-lg.p-4.bg-gray-300.grid.grid-cols-2.gap-4
     (for [[field value] (sort-by first fields)]
       ^{:key field}
       [:<>
        [:dt (pr-str field)]
        [:dd (pr-str value)]])]))

(defn additional-attribute-fields [entity]
  [additional-fields entity [:db/valueType :db/cardinality :db.schema/references]])

(defn additional-constant-fields [entity]
  [additional-fields entity []])

(defmulti panel (fn [entity]
                  (if (:db/valueType entity)
                    :attribute
                    :constant)))

(defmethod panel :attribute [entity]
  [:div.px-4
   [:div.sm:px-0.sm:flex
    [:div.sm:w-4of6.mb-4
     [:div.font-bold
      [entity/header entity]]
     [part-of entity]
     [entity/doc-str entity]
     [see-also-links entity]]
    [:div.sm:text-right.flex-grow
     [entity/value-type entity]]]
   [additional-attribute-fields entity]])

(defmethod panel :constant [entity]
  [:div.px-4.sm:px-0.sm:flex
   [:div.sm:w-4of6.mb-4
    [:div.font-bold
     [entity/header entity]]
    [part-of entity]
    [entity/doc-str entity]
    [see-also-links entity]]
   [:div
    [additional-constant-fields entity]]])

(defn page [parameters]
  (let [attr (<sub [::by-ident (keyword (:id (:path parameters)))])]
    [panel attr]))
