(ns schema-voyager.html.pages.attribute
  (:require [schema-voyager.html.db :as db]
            [schema-voyager.html.components.value-type :as value-type]
            [schema-voyager.html.diagrams.core :as diagrams]
            [schema-voyager.html.util :as util]))

(defn doc-str [{:keys [db/doc]}]
  (when doc
    [:p.italic doc]))

(defn part-of [{:keys [db.schema/part-of]}]
  [:div.text-gray-600 "Part of " [util/coll-links part-of]])

(defn see-also-links [{:keys [db.schema/see-also]}]
  (when (seq see-also)
    [:div "See also " [util/attr-links see-also]]))

(defn seen-by-links [{:keys [db.schema/noted-by]}]
  (when (seq noted-by)
    [:div "Noted by " [util/attr-links noted-by]]))

(defn details-section [{:keys [db/doc db.schema/see-also db.schema/noted-by] :as attribute}]
  (when (or doc (seq see-also) (seq noted-by))
    [:div.p-4.sm:p-6.space-y-6
     [doc-str attribute]
     [see-also-links attribute]
     [seen-by-links attribute]]))

(defn unhandled-fields [{:keys [db/unique] :as attribute}]
  (cond-> (dissoc attribute
                  :db/id :db/ident :db/doc :db/valueType :db/cardinality
                  :db/tupleAttrs :db/tupleType :db/tupleTypes
                  :db.schema/part-of :db.schema/noted-by :db.schema/see-also :db.schema/deprecated?
                  :db.schema/references :db.schema/tuple-references
                  :db.schema.pseudo/type)
    (= unique :db.unique/identity) (dissoc :db/unique)))

(defn additional-fields [attribute]
  (when-let [fields (seq (unhandled-fields attribute))]
    [:dl.divide-y
     (for [[field value] (sort-by first fields)]
       ^{:key field}
       [:div.sm:flex.p-4.sm:p-6
        [:dt.sm:w-1|3 (pr-str field)]
        [:dd (pr-str value)]])]))

(defn diagram [attr]
  ^{:key (:db/id attr)}
  [diagrams/erd (db/attr-edges attr)])

(defn header [{:keys [db/ident db/unique db.schema/deprecated?]} coll-type]
  [:h1.font-bold.flex.items-center.space-x-2
   [util/ident-name {:coll-props {:class [:font-normal]}} ident coll-type]
   (when (= :db.unique/identity unique)
     util/primary-key)
   (when deprecated?
     util/deprecated-pill)])

(defmulti panel :db.schema.pseudo/type)

(defmethod panel :attribute [attribute]
  [:div.max-w-4xl.space-y-6
   [:div.px-4.sm:px-0
    [:div.sm:flex.sm:space-x-6
     [:div.space-y-4
      [header attribute :aggregate]
      [part-of attribute]]
     [value-type/p attribute]]]
   [:div.sm:shadow-lg.overflow-hidden.sm:rounded-lg.bg-white.divide-y
    [details-section attribute]
    [additional-fields attribute]]
   [diagram attribute]])

(defmethod panel :constant [constant]
  [:div.max-w-4xl.space-y-6
   [:div.px-4.sm:px-0.space-y-4
    [header constant :enum]
    [part-of constant]]
   [:div.sm:shadow-lg.overflow-hidden.sm:rounded-lg.bg-white.divide-y
    [details-section constant]
    [additional-fields constant]]])

(defn page [parameters]
  (let [attr (db/attribute-by-ident (keyword (:id (:path parameters))))]
    [panel attr]))
