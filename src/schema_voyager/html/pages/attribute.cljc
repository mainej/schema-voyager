(ns schema-voyager.html.pages.attribute
  (:require [datascript.core :as d]
            [schema-voyager.html.db :as db]
            [schema-voyager.html.components.entity :as entity]
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
    [:div "See also "
     [util/attr-links see-also]]))

(defn seen-by-links [{:keys [db.schema/_see-also]}]
  (when (seq _see-also)
    [:div "Noted by "
     [util/attr-links _see-also]]))

(defn details-section [{:keys [db/doc db.schema/see-also db.schema/_see-also] :as entity}]
  (when (or doc (seq see-also) (seq _see-also))
    [:div.p-4.sm:p-6
     [entity/doc-str entity]
     [see-also-links entity]
     [seen-by-links entity]]))

(defn unhandled-fields [{:keys [db/unique] :as entity}]
  (cond-> (dissoc entity :db/id :db.schema/part-of :db.schema/_see-also :db.schema/see-also :db.schema/deprecated? :db/ident :db/doc)
    (= unique :db.unique/identity) (dissoc :db/unique)))

(defn additional-fields [entity but-fields]
  (when-let [fields (seq (apply dissoc (unhandled-fields entity) but-fields))]
    [:dl
     (for [[field value] (sort-by first fields)]
       ^{:key field}
       [:div.sm:flex.border-t.p-4.sm:p-6
        [:dt.sm:w-1of3 (pr-str field)]
        [:dd (pr-str value)]])]))

(defn additional-attribute-fields [entity]
  [additional-fields entity [:db/valueType :db/cardinality :db.schema/references]])

(defn additional-constant-fields [entity]
  [additional-fields entity []])

(defn header [{:keys [db/ident] :as entity} coll-type]
  [:h1.mb-4.font-bold
   [util/ident-name ident coll-type]
   [entity/unique-span entity]
   [entity/deprecated-span entity]])

(defmulti panel (fn [entity]
                  (if (:db/valueType entity)
                    :attribute
                    :constant)))

(defmethod panel :attribute [entity]
  [:div
   [:div.px-4.sm:px-0.sm:flex
    [:div
     [header entity :aggregate]
     [part-of entity]]
    [:div.sm:ml-6
     [:span.text-gray-600.font-light
      "Type of "
      (if (= :db.cardinality/many (:db/cardinality entity))
        "values are"
        "value is")]
     " "
     [entity/value-type entity]]]
   [:div.mt-6.sm:shadow-lg.sm:rounded-lg.bg-white.max-w-4xl
    [details-section entity]
    [additional-attribute-fields entity]]])

(defmethod panel :constant [entity]
  [:div
   [:div.px-4.sm:px-0
    [header entity :enum]
    [part-of entity]]
   [:div.mt-6.sm:shadow-lg.sm:rounded-lg.bg-white.max-w-4xl
    [details-section entity]
    [additional-constant-fields entity]]])

(defn page [parameters]
  (let [attr (by-ident (keyword (:id (:path parameters))))]
    [panel attr]))
