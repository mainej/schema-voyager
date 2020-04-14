(ns schema-voyager.html.pages.attribute
  (:require [datascript.core :as ds]
            [schema-voyager.html.db :as db]
            [schema-voyager.html.components.value-type :as value-type]
            [schema-voyager.html.diagrams.collection :as diagrams.collection]
            [schema-voyager.html.util :as util]))

(defn by-ident [ident]
  (ds/pull db/db
           ['*
            {:db.schema/part-of          ['*]
             :db.schema/references       ['*]
             :db.schema/tuple-references ['*
                                          {:db.schema/references ['*]}]
             :db.schema/see-also         util/attr-link-pull
             :db.schema/_see-also        util/attr-link-pull}]
           [:db/ident ident]))

(defn doc-str [{:keys [db/doc]}]
  (when doc
    [:p.italic doc]))

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

(defn details-section [{:keys [db/doc db.schema/see-also db.schema/_see-also] :as attribute}]
  (when (or doc (seq see-also) (seq _see-also))
    [:div.p-4.sm:p-6
     [doc-str attribute]
     [see-also-links attribute]
     [seen-by-links attribute]]))

(defn unhandled-fields [{:keys [db/unique] :as attribute}]
  (cond-> (dissoc attribute
                  :db/id :db/ident :db/doc :db/valueType :db/cardinality
                  :db/tupleAttrs :db/tupleType :db/tupleTypes
                  :db.schema/part-of :db.schema/_see-also :db.schema/see-also :db.schema/deprecated?
                  :db.schema/references :db.schema/tuple-references
                  :db.schema.pseudo/type)
    (= unique :db.unique/identity) (dissoc :db/unique)))

(defn additional-fields [attribute]
  (when-let [fields (seq (unhandled-fields attribute))]
    [:dl
     (for [[field value] (sort-by first fields)]
       ^{:key field}
       [:div.sm:flex.border-t.p-4.sm:p-6
        [:dt.sm:w-1of3 (pr-str field)]
        [:dd (pr-str value)]])]))

(defn diagram [attr]
  ^{:key (:db/id attr)}
  [diagrams.collection/erd (diagrams.collection/q-attr db/db attr)])

(defn header [{:keys [db/ident db/unique db.schema/deprecated?]} coll-type]
  [:h1.mb-4.font-bold
   [util/ident-name {:coll-props {:class [:font-normal]}} ident coll-type]
   (when (= :db.unique/identity unique)
     [:span.ml-2 util/lock-closed])
   (when deprecated?
     [:span.ml-2 util/deprecated-pill])])

(defmulti panel :db.schema.pseudo/type)

(defmethod panel :attribute [attribute]
  [:div.max-w-4xl
   [:div.px-4.sm:px-0
    [:div.sm:flex
     [:div
      [header attribute :aggregate]
      [part-of attribute]]
     [:div.sm:ml-6
      [value-type/p attribute]]]]
   [:div.mt-6.sm:shadow-lg.overflow-hidden.sm:rounded-lg.bg-white
    [details-section attribute]
    [additional-fields attribute]]
   [:div.mt-6
    [diagram attribute]]])

(defmethod panel :constant [constant]
  [:div.max-w-4xl
   [:div.px-4.sm:px-0
    [header constant :enum]
    [part-of constant]]
   [:div.mt-6.sm:shadow-lg.overflow-hidden.sm:rounded-lg.bg-white
    [details-section constant]
    [additional-fields constant]]])

(defn page [parameters]
  (let [attr (by-ident (keyword (:id (:path parameters))))]
    [panel attr]))
