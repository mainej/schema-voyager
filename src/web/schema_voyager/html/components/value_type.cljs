(ns schema-voyager.html.components.value-type
  (:require
   [schema-voyager.html.util :as util]))

(defn- angle-span [child]
  [:span "< " child " >"])

(defn- angle-list [children]
  [angle-span [util/comma-list children]])

(def ^:private one-abbr [:abbr {:title ":db.cardinality/one"} "one"])
(def ^:private many-abbr [:abbr {:title ":db.cardinality/many"} "many"])
(def ^:private tuple-abbr [:abbr {:title ":db.type/tuple"} "tuple"])
(def ^:private tuples-abbr [:abbr {:title ":db.type/tuple"} "tuples"])
(def ^:private references-abbr [:abbr {:title ":db.type/ref"} "references"])
(def ^:private composed-abbr [:abbr {:title ":db/tupleAttrs"} "composed"])
(def ^:private homogeneous-abbr [:abbr {:title ":db/tupleType"} "homogeneous"])
(def ^:private heterogeneous-abbr [:abbr {:title ":db/tupleTypes"} "heterogeneous"])
(defn- keyword-abbr [kw]
  [:abbr {:title (pr-str kw)} (name kw)])

(defn- links-or-abbr [value-type]
  (if (keyword? value-type)
    [keyword-abbr value-type]
    [util/coll-links value-type]))

(defn- pipe-links-or-abbr [value-type]
  (if (keyword? value-type)
    [keyword-abbr value-type]
    [util/coll-pipe-links value-type]))

(defn- tuple-attrs-list [attrs]
  [angle-list (map (fn [attr]
                     ^{:key (:db/ident attr)}
                     [util/attr-link attr])
                   attrs)])

(defn- heterogeneous-types-list [value-types]
  [angle-list
   (map-indexed (fn [position value-type]
                  ^{:key position}
                  [pipe-links-or-abbr value-type])
                value-types)])

(defn- is-or-references [value-type]
  (if (keyword? value-type) "is" references-abbr))

(defn- has-or-references [value-type]
  (if (keyword? value-type) "has" references-abbr))

(defmulti p :db/valueType)
(defmulti p-composite :db/cardinality)
(defmulti p-heterogeneous :db/cardinality)
(defmulti p-homogeneous :db/cardinality)
(defmulti p-basic :db/cardinality)

(defmethod p :db.type/tuple.composite [attribute] [p-composite attribute])
(defmethod p :db.type/tuple.homogeneous [attribute] [p-homogeneous attribute])
(defmethod p :db.type/tuple.heterogeneous [attribute] [p-heterogeneous attribute])
(defmethod p :default [attribute] [p-basic attribute])

(defmethod p-composite :db.cardinality/many [{:keys [db/tupleAttrs]}]
  ;; NOTE: In practice a composite tuple is probably never card many.
  [:p
   "This attribute is " many-abbr " " tuples-abbr ", each " composed-abbr " of other attributes "
   [tuple-attrs-list tupleAttrs]". "
   "It's managed by Datomic, and should not be set manually."])
(defmethod p-composite :default [{:keys [db/tupleAttrs]}]
  [:p
   "This attribute is " one-abbr " " tuple-abbr ", " composed-abbr " of other attributes "
   [tuple-attrs-list tupleAttrs]". "
   "It's managed by Datomic, and should not be set manually."])

(defmethod p-homogeneous :db.cardinality/many [{:keys [db/tupleType]}]
  [:p
   "This attribute has " many-abbr " values, " homogeneous-abbr " " tuples-abbr " of variable length. "
   "Each member of each tuple " (is-or-references tupleType) " one "
   [links-or-abbr tupleType]])
(defmethod p-homogeneous :default [{:keys [db/tupleType]}]
  [:p
   "This attribute has " one-abbr " value, a " homogeneous-abbr " " tuple-abbr " of variable length. "
   "Each member of the tuple " (is-or-references tupleType) " one "
   [links-or-abbr tupleType]])

(defmethod p-heterogeneous :db.cardinality/many [{:keys [db/tupleTypes]}]
  [:p "This attribute has " many-abbr " values, " heterogeneous-abbr " " tuples-abbr " of length "
   (count tupleTypes) ". Each tuple is of type "
   [heterogeneous-types-list tupleTypes]])
(defmethod p-heterogeneous :default [{:keys [db/tupleTypes]}]
  [:p "This attribute has " one-abbr " value, a " heterogeneous-abbr " " tuple-abbr " of length "
   (count tupleTypes) ". The tuple is of type "
   [heterogeneous-types-list tupleTypes]])

(defmethod p-basic :db.cardinality/many [{:keys [db/valueType]}]
  [:p "This attribute " (has-or-references valueType) " " many-abbr " " [links-or-abbr valueType] " values."])
(defmethod p-basic :default [{:keys [db/valueType]}]
  [:p "This attribute " (has-or-references valueType) " " one-abbr " " [links-or-abbr valueType] " value."])

(defmulti shorthand-span :db/valueType)

(defmethod shorthand-span :db.type/tuple.composite [{:keys [db/tupleAttrs]}]
  [tuple-attrs-list tupleAttrs])

(defmethod shorthand-span :db.type/tuple.homogeneous [{:keys [db/tupleType]}]
  [angle-span [:span [links-or-abbr tupleType] " {2,}"]])

(defmethod shorthand-span :db.type/tuple.heterogeneous [{:keys [db/tupleTypes]}]
  [heterogeneous-types-list tupleTypes])

(defmethod shorthand-span :default [{:keys [db/valueType]}]
  [pipe-links-or-abbr valueType])

(defn shorthand [{:keys [db/cardinality] :as attribute}]
  (let [many? (= :db.cardinality/many cardinality)]
    [:span
     (when many? "[")
     [shorthand-span attribute]
     (when many? "]")]))
