(ns schema-voyager.html.components.entity
  (:require [schema-voyager.html.util :as util]
            [schema-voyager.html.components.attributes :as attributes]))

(defn field-definition-list [fields]
  [:dl
   (for [[field value] (sort-by first fields)]
     ^{:key field}
     [:<>
      [:dt (pr-str field)]
      [:dd (pr-str value)]])])

(defn reference-list [colls]
  [:ul
   (for [coll colls]
     [:li
      [:a {:href (util/coll-href coll)}
       (pr-str
        (:db.schema.collection/name coll))]])])

(defn see-also-links [{:keys [db.schema/see-also]}]
  (when-let [see-also see-also]
    [:div "see also"
     [attributes/links see-also]]))

(defmulti panel (fn [entity]
                  (if (:db/valueType entity)
                    :attribute
                    :constant)))

(defmethod panel :attribute [{:keys [db/ident db/doc db/valueType db/cardinality db.schema/references] :as entity}]
  (let [many? (= :db.cardinality/many cardinality)]
    [:section
     [:h1 (pr-str ident)]
     (when-let [doc-str doc]
       [:p doc-str])
     [:p
      (when many? "[")
      (if (seq references)
        [reference-list references]
        valueType)
      (when many? "]")]
     [see-also-links entity]
     (when-let [additional-fields (seq (dissoc entity :db/id :db.schema/part-of :db.schema/see-also :db/ident :db/doc :db/valueType :db/cardinality :db.schema/references))]
       [field-definition-list additional-fields])]))

(defmethod panel :constant [{:keys [db/ident db/doc] :as entity}]
  [:section
   [:h1 (pr-str ident)]
   (when-let [doc-str doc]
     [:p doc-str])
   [see-also-links entity]
   (when-let [additional-fields (seq (dissoc entity :db/id :db.schema/part-of :db.schema/see-also :db/ident :db/doc))]
     [field-definition-list additional-fields])])
