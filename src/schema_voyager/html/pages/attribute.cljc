(ns schema-voyager.html.pages.attribute
  (:require [datascript.core :as d]
            [schema-voyager.html.db :as db]
            [schema-voyager.html.components.entity :as entity]
            [schema-voyager.html.components.value-type :as value-type]
            [schema-voyager.html.util :as util]))

(defn by-ident [ident]
  (d/pull db/db
          ['*
           {:db.schema/part-of    ['*]
            :db.schema/references ['*]
            :db.schema/see-also   util/attr-link-pull
            :db.schema/_see-also  util/attr-link-pull}]
          [:db/ident ident]))

(defn part-of [{:keys [db.schema/part-of]}]
  [:div.text-gray-600 "Part of "
   [util/coll-links part-of]])

(defn see-also-links [{:keys [db.schema/see-also]}]
  (when (seq see-also)
    [:div.mt-6 "See also "
     [util/attr-links see-also]]))

(defn seen-by-links [{:keys [db.schema/_see-also]}]
  (when (seq _see-also)
    [:div.mt-6 "Noted by "
     [util/attr-links _see-also]]))

(defn details-section [{:keys [db/doc db.schema/see-also db.schema/_see-also] :as entity}]
  (when (or doc (seq see-also) (seq _see-also))
    [:div.p-4.sm:p-6
     [entity/doc-str entity]
     [see-also-links entity]
     [seen-by-links entity]]))

(defn unhandled-fields [{:keys [db/unique] :as entity}]
  (cond-> (dissoc entity
                  :db/id :db/ident :db/doc :db/valueType :db/cardinality
                  :db/tupleAttrs :db/tupleType :db/tupleTypes
                  :db.schema/part-of :db.schema/_see-also :db.schema/see-also :db.schema/deprecated? :db.schema/references :db.schema/tuple-references)
    (= unique :db.unique/identity) (dissoc :db/unique)))

(defn additional-fields [entity]
  (when-let [fields (seq (unhandled-fields entity))]
    [:dl
     (for [[field value] (sort-by first fields)]
       ^{:key field}
       [:div.sm:flex.border-t.p-4.sm:p-6
        [:dt.sm:w-1of3 (pr-str field)]
        [:dd (pr-str value)]])]))

(defn header [{:keys [db/ident] :as entity} coll-type]
  [:h1.mb-4.font-bold
   [util/ident-name ident coll-type]
   [entity/unique-span entity]
   [entity/deprecated-span entity]])

(defmulti panel entity/entity-type)

(defmethod panel :attribute [entity]
  [:div.max-w-4xl
   [:div.px-4.sm:px-0
    [:div.sm:flex
     [:div
      [header entity :aggregate]
      [part-of entity]]
     [:div.sm:ml-6
      [value-type/p entity]]]]
   [:div.mt-6.sm:shadow-lg.overflow-hidden.sm:rounded-lg.bg-white
    [details-section entity]
    [additional-fields entity]]])

(defmethod panel :constant [entity]
  [:div.max-w-4xl
   [:div.px-4.sm:px-0
    [header entity :enum]
    [part-of entity]]
   [:div.mt-6.sm:shadow-lg.overflow-hidden.sm:rounded-lg.bg-white
    [details-section entity]
    [additional-fields entity]]])

(defn page [parameters]
  (let [attr (by-ident (keyword (:id (:path parameters))))]
    [panel attr]))
