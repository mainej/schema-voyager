(ns schema-voyager.html.pages.collections
  (:require [schema-voyager.html.db :as db]
            [schema-voyager.html.util :as util]
            [schema-voyager.html.diagrams.core :as diagrams]))

(defn collection-list [collection]
  [:ul
   (for [{:keys [db/id] :as coll} collection]
     ^{:key id}
     [:li
      [util/link {:href (util/coll-href coll)}
       [util/coll-name* coll]]])])

(defn spec-list [specs]
  [:ul
   (for [{:keys [db/id] :as spec} specs]
     ^{:key id}
     [:li
      [util/link {:href (util/spec-href spec)}
       [util/spec-name spec]]])])

(defn list-section [title description l]
  [:div.md:flex.px-4.sm:px-0
   [:div.md:w-1|4
    [:h1.small-caps title]
    [:p.my-4.font-light.text-gray-700.mr-4 description]]
   [:div.md:w-3|4 l]])

(defn page []
  [:div.space-y-8
   [list-section
    [:span.flex.items-center.gap-x-1 "Aggregates" [util/aggregate-abbr {:db.schema.collection/type :aggregate}]]
    "Aggregates are collections of attributes that often co-exist on an entity. They are analogous to a SQL table, though some attributes may appear on many aggregates."
    [collection-list (db/collections-by-type :aggregate)]]
   (when-let [enums (seq (db/collections-by-type :enum))]
     [list-section
      [:span.flex.items-center.gap-x-1 "Enums" [util/aggregate-abbr {:db.schema.collection/type :enum}]]
      "Enums are collections of named constants. They usually specify the various values that an attribute may take."
      [collection-list enums]])
   (when-let [specs (seq (db/entity-specs))]
     [list-section
      "Entity Specs"
      "Entity specs are constraints that can be placed on an entity during a transaction. They require attributes, run predicate functions for validation, or both."
      [spec-list specs]])
   [:div
    ^{:key :collections}
    [diagrams/erd (db/colls-edges)]]])
