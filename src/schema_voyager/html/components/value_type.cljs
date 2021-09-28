(ns schema-voyager.html.components.value-type
  (:require [datascript.core :as ds]
            [schema-voyager.html.db :as db]
            [schema-voyager.html.util :as util]))

(defn tuple-composition [{:keys [db/tupleAttrs db/tupleType db/tupleTypes]}]
  (cond
    tupleAttrs :db.type.tuple/composite
    tupleType  :db.type.tuple/homogeneous
    tupleTypes :db.type.tuple/heterogeneous))

(defn angle-span [child]
  [:span "< " child " >"])

(defn- tuple-attrs-span [{:keys [db/tupleAttrs]}]
  (let [attrs (ds/pull-many db/db util/attr-link-pull tupleAttrs)]
    [angle-span
     [util/comma-list
      (map (fn [attr]
             ^{:key (:db/ident attr)}
             [util/attr-link attr])
           attrs)]]))

(def ^:private card-one-abbr [:abbr {:title ":db.cardinality/one"} "one"])
(def ^:private card-many-abbr [:abbr {:title ":db.cardinality/many"} "many"])
(def ^:private tuple-abbr [:abbr {:title ":db.type/tuple"} "tuple"])
(def ^:private tuples-abbr [:abbr {:title ":db.type/tuple"} "tuples"])
(def ^:private reference-abbr [:abbr {:title ":db.type/ref"} "reference"])
(def ^:private references-abbr [:abbr {:title ":db.type/ref"} "references"])
(def ^:private composed-abbr [:abbr {:title ":db/tupleAttrs"} "composed"])
(def ^:private homogeneous-abbr [:abbr {:title ":db/tupleType"} "homogeneous"])
(def ^:private heterogeneous-abbr [:abbr {:title ":db/tupleTypes"} "heterogeneous"])
(defn- keyword-abbr [kw]
  [:abbr {:title (pr-str kw)} (name kw)])

(defmulti p
  (fn [{:keys [db/cardinality db/valueType db.schema/references db/tupleType] :as attribute}]
    (let [tuple?            (= :db.type/tuple valueType)
          ref?              (= :db.type/ref valueType)
          tuple-composition (tuple-composition attribute)]
      [cardinality
       (cond tuple? tuple-composition
             ref?   valueType
             :else  ::default)
       (if (and (seq references)
                (or ref?
                    (= :db.type/ref tupleType)))
         :referred
         ::default)])))

;; composite tuples
(defmethod p [:db.cardinality/one :db.type.tuple/composite ::default] [attribute]
  [:p
   "This attribute is " card-one-abbr " " tuple-abbr ", " composed-abbr " of other attributes "
   [tuple-attrs-span attribute]". "
   "It's managed by Datomic, and should not be set manually."])
(defmethod p [:db.cardinality/many :db.type.tuple/composite ::default] [attribute]
  ;; NOTE: In practice a composite tuple is probably never card many.
  [:p
   "This attribute is " card-many-abbr " " tuples-abbr ", each " composed-abbr " of other attributes "
   [tuple-attrs-span attribute]". "
   "It's managed by Datomic, and should not be set manually."])

;; homogeneous tuples
(def ^:private homogeneous-one-span [:span
                                     "This attribute has " card-one-abbr " value, a " homogeneous-abbr " " tuple-abbr " of variable length. "
                                     "Each member of the tuple "])
(def ^:private homogeneous-many-span [:span
                                      "This attribute has " card-many-abbr " values, " homogeneous-abbr " " tuples-abbr " of variable length. "
                                      "Each member of each tuple "])
(defn- homogeneous-referred-span [{:keys [db.schema/references]}]
  [:span references-abbr " one " [util/coll-links references] "."])
(defn- homogeneous-unreferred-span [{:keys [db/tupleType]}]
  [:span "is one " [keyword-abbr tupleType] "."])

(defmethod p [:db.cardinality/one :db.type.tuple/homogeneous :referred] [attribute]
  [:p homogeneous-one-span [homogeneous-referred-span attribute]])
(defmethod p [:db.cardinality/one :db.type.tuple/homogeneous ::default] [attribute]
  [:p homogeneous-one-span [homogeneous-unreferred-span attribute]])
(defmethod p [:db.cardinality/many :db.type.tuple/homogeneous :referred] [attribute]
  [:p homogeneous-many-span [homogeneous-referred-span attribute]])
(defmethod p [:db.cardinality/many :db.type.tuple/homogeneous ::default] [attribute]
  [:p homogeneous-many-span [homogeneous-unreferred-span attribute]])

;; heterogeneous tuples
(defn- heterogeneous-one-span [{:keys [db/tupleTypes]}]
  [:span "This attribute has " card-one-abbr " value, a " heterogeneous-abbr " " tuple-abbr " of length "
   (count tupleTypes) ". The tuple is of type "])
(defn- heterogeneous-many-span [{:keys [db/tupleTypes]}]
  [:span "This attribute has " card-many-abbr " values, " heterogeneous-abbr " " tuples-abbr " of length "
   (count tupleTypes) ". Each tuple is of type "])
(defn- heterogeneous-types [{:keys [db/tupleTypes db.schema/tuple-references]}]
  (let [refs-by-position (zipmap (map :db.schema.tuple/position tuple-references)
                                 (map :db.schema/references tuple-references))]
    [angle-span
     [util/comma-list
      (map-indexed (fn [position value-type]
                     ^{:key position}
                     (let [refs (get refs-by-position position)]
                       (if (and (= :db.type/ref value-type)
                                refs)
                         [util/coll-links refs]
                         [keyword-abbr value-type])))
                   tupleTypes)]]))
(defmethod p [:db.cardinality/one :db.type.tuple/heterogeneous ::default] [attribute]
  [:p
   [heterogeneous-one-span attribute]
   [heterogeneous-types attribute]])
(defmethod p [:db.cardinality/many :db.type.tuple/heterogeneous ::default] [attribute]
  [:p
   [heterogeneous-many-span attribute]
   [heterogeneous-types attribute]])

;; references
(defmethod p [:db.cardinality/one :db.type/ref :referred] [{:keys [db.schema/references]}]
  [:p
   "This attribute " references-abbr " " card-one-abbr " value, of type "
   [util/coll-links references] "."])
(defmethod p [:db.cardinality/one :db.type/ref ::default] [_attribute]
  [:p "This attribute " references-abbr " " card-one-abbr " value."])
(defmethod p [:db.cardinality/many :db.type/ref :referred] [{:keys [db.schema/references]}]
  [:p
   "This attribute " references-abbr " " card-many-abbr " values, of type "
   [util/coll-links references] "."])
(defmethod p [:db.cardinality/many :db.type/ref ::default] [_attribute]
  [:p "This attribute " references-abbr " " card-many-abbr " values."])

;; scalars
(defmethod p [:db.cardinality/one ::default ::default] [{:keys [db/valueType]}]
  [:p "This attribute has " card-one-abbr " " [keyword-abbr valueType] " value."])
(defmethod p [:db.cardinality/many ::default ::default] [{:keys [db/valueType]}]
  [:p "This attribute has " card-many-abbr " " [keyword-abbr valueType] " values."])

(defmulti shorthand-span
  (fn [{:keys [db/valueType db.schema/references db/tupleType] :as attribute}]
    (let [tuple?            (= :db.type/tuple valueType)
          ref?              (= :db.type/ref valueType)
          tuple-composition (tuple-composition attribute)]
      [(cond tuple? tuple-composition
             ref?   valueType
             :else  ::default)
       (if (and (seq references)
                (or ref?
                    (= :db.type/ref tupleType)))
         :referred
         ::default)])))

;; composite tuples
(defmethod shorthand-span [:db.type.tuple/composite ::default] [attribute]
  [tuple-attrs-span attribute])

;; homogeneous tuples
(defmethod shorthand-span [:db.type.tuple/homogeneous :referred] [{:keys [db.schema/references]}]
  [angle-span [:span [util/coll-links references] " {2,}"]])
(defmethod shorthand-span [:db.type.tuple/homogeneous ::default] [{:keys [db/tupleType]}]
  [angle-span [:span [keyword-abbr tupleType] " {2,}"]])

;; heterogeneous tuples
(defmethod shorthand-span [:db.type.tuple/heterogeneous ::default] [attribute]
  [heterogeneous-types attribute])

;; references
(defmethod shorthand-span [:db.type/ref :referred] [{:keys [db.schema/references]}]
  [util/coll-links references])
(defmethod shorthand-span [:db.type/ref ::default] [_attribute]
  [keyword-abbr :db.type/ref])

;; scalars
(defmethod shorthand-span [::default ::default] [{:keys [db/valueType]}]
  [keyword-abbr valueType])

(defn shorthand [{:keys [db/cardinality] :as attribute}]
  (let [many? (= :db.cardinality/many cardinality)]
    [:span
     (when many? "[")
     [shorthand-span attribute]
     (when many? "]")]))
