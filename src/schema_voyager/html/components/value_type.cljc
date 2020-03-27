(ns schema-voyager.html.components.value-type
  (:require [datascript.core :as d]
            [schema-voyager.html.db :as db]
            [schema-voyager.html.components.entity :as entity]
            [schema-voyager.html.util :as util]))

(defn angle-span [child]
  [:span "< " child " >"])

(defn- tuple-span [children]
  [angle-span
   [util/comma-list
    (map-indexed (fn [i child]
                   ^{:key i}
                   child)
                 children)]])

(defn- attrs-span [{:keys [db/tupleAttrs]}]
  (let [attrs (d/pull-many db/db util/attr-link-pull tupleAttrs)]
    [tuple-span (map util/attr-link attrs)]))

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
    (let [tuple?     (= :db.type/tuple valueType)
          ref?       (= :db.type/ref valueType)
          tuple-type (entity/tuple-type attribute)]
      [cardinality
       (cond tuple? tuple-type
             ref?   valueType
             :else  ::default)
       (cond
         ref?
         (if (seq references) :referred :not-referred)

         (= :db.type.tuple/homogeneous tuple-type)
         (if (= :db.type/ref tupleType)
           (if (seq references) :referred :not-referred)
           ::default)

         :else
         ::default)])))

;; composite tuples
(defmethod p [:db.cardinality/one :db.type.tuple/composite ::default] [attribute]
  [:p
   "This attribute is " card-one-abbr " " tuple-abbr ", " composed-abbr " of other attributes "
   [attrs-span attribute]". "
   "It is managed by Datomic, and should not be set manually."])
(defmethod p [:db.cardinality/many :db.type.tuple/composite ::default] [attribute]
  ;; NOTE: In practice a composite tuple is probably never card many.
  [:p
   "This attribute is " card-many-abbr " " tuples-abbr ", each " composed-abbr " of other attributes "
   [attrs-span attribute]". "
   "It is managed by Datomic, and should not be set manually."])

;; homogeneous tuples
(def ^:private homogeneous-one-span [:span "This attribute has " card-one-abbr " value, a " homogeneous-abbr " " tuple-abbr " of variable length. "])
(def ^:private homogeneous-many-span [:span "This attribute has " card-many-abbr " values, " homogeneous-abbr " " tuples-abbr " of variable length. "])
(def ^:private homogeneous-one-ref-span [:span "Each member of the " tuple-abbr " is a " reference-abbr ": "])
(def ^:private homogeneous-many-ref-span [:span "Each member of each " tuple-abbr " is a " reference-abbr ": "])
(def ^:private homogeneous-one-scalar-span [:span "Each member of the " tuple-abbr " is a scalar: "])
(def ^:private homogeneous-many-scalar-span [:span "Each member of each " tuple-abbr " is a scalar: "])
(defn- homogeneous-referred-span [{:keys [db.schema/references]}]
  [:span [angle-span [:span [util/coll-links references] " {2,}"]]])
(defn- homogeneous-not-referred-span [{:keys [db/tupleType]}]
  [:span [angle-span [:span [keyword-abbr tupleType] " {2,}"]]])
(defmethod p [:db.cardinality/one :db.type.tuple/homogeneous :referred] [attribute]
  [:p homogeneous-one-span homogeneous-one-ref-span [homogeneous-referred-span attribute] "."])
(defmethod p [:db.cardinality/one :db.type.tuple/homogeneous :not-referred] [attribute]
  [:p homogeneous-one-span homogeneous-one-ref-span [homogeneous-not-referred-span attribute] "."])
(defmethod p [:db.cardinality/one :db.type.tuple/homogeneous ::default] [attribute]
  [:p homogeneous-one-span homogeneous-one-scalar-span [homogeneous-not-referred-span attribute] "."])
(defmethod p [:db.cardinality/many :db.type.tuple/homogeneous :referred] [attribute]
  [:p homogeneous-many-span homogeneous-many-ref-span [homogeneous-referred-span attribute] "."])
(defmethod p [:db.cardinality/many :db.type.tuple/homogeneous :not-referred] [attribute]
  [:p homogeneous-many-span homogeneous-many-ref-span [homogeneous-not-referred-span attribute] "."])
(defmethod p [:db.cardinality/many :db.type.tuple/homogeneous ::default] [attribute]
  [:p homogeneous-many-span homogeneous-many-scalar-span [homogeneous-not-referred-span attribute] "."])

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
    [tuple-span
     (map-indexed (fn [position kw]
                    (let [refs (get refs-by-position position)]
                      (if (and (= :db.type/ref kw)
                               refs)
                        [util/coll-links refs]
                        [keyword-abbr kw])))
                  tupleTypes)]))
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
(defmethod p [:db.cardinality/one :db.type/ref :not-referred] [attribute]
  [:p "This attribute " references-abbr " " card-one-abbr " value."])
(defmethod p [:db.cardinality/many :db.type/ref :referred] [{:keys [db.schema/references]}]
  [:p
   "This attribute " references-abbr " " card-many-abbr " values, of type "
   [util/coll-links references] "."])
(defmethod p [:db.cardinality/many :db.type/ref :not-referred] [attribute]
  [:p "This attribute " references-abbr " " card-many-abbr " values."])

;; scalars
(defmethod p [:db.cardinality/one ::default ::default] [{:keys [db/valueType]}]
  [:p "This attribute has " card-one-abbr " " [keyword-abbr valueType] " value."])
(defmethod p [:db.cardinality/many ::default ::default] [{:keys [db/valueType]}]
  [:p "This attribute has " card-many-abbr " " [keyword-abbr valueType] " values."])

(defmulti shorthand-span
  (fn [{:keys [db/valueType db.schema/references db/tupleType] :as attribute}]
    (let [tuple?     (= :db.type/tuple valueType)
          ref?       (= :db.type/ref valueType)
          tuple-type (entity/tuple-type attribute)]
      [(cond tuple? tuple-type
             ref?   valueType
             :else  ::default)
       (cond
         ref?
         (if (seq references) :referred :not-referred)

         (= :db.type.tuple/homogeneous tuple-type)
         (if (and (= :db.type/ref tupleType)
                  (seq references))
           :referred
           ::default)

         :else
         ::default)])))

;; composite tuples
(defmethod shorthand-span [:db.type.tuple/composite ::default] [attribute]
  [attrs-span attribute])

;; homogeneous tuples
(defmethod shorthand-span [:db.type.tuple/homogeneous :referred] [attribute]
  [homogeneous-referred-span attribute])
(defmethod shorthand-span [:db.type.tuple/homogeneous ::default] [attribute]
  [homogeneous-not-referred-span attribute])

;; heterogeneous tuples
(defmethod shorthand-span [:db.type.tuple/heterogeneous ::default] [attribute]
  [heterogeneous-types attribute])

;; references
(defmethod shorthand-span [:db.type/ref :referred] [{:keys [db.schema/references]}]
  [util/coll-links references])
(defmethod shorthand-span [:db.type/ref :not-referred] [attribute]
  "ref")

;; scalars
(defmethod shorthand-span [::default ::default] [{:keys [db/valueType]}]
  [keyword-abbr valueType])

(defn shorthand [{:keys [db/cardinality] :as entity}]
  (let [many? (= :db.cardinality/many cardinality)]
    [:span
     (when many? "[")
     [shorthand-span entity]
     (when many? "]")]))
