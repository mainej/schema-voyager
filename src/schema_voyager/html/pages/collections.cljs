(ns schema-voyager.html.pages.collections
  (:require [datascript.core :as ds]
            [schema-voyager.html.db :as db]
            [schema-voyager.html.util :as util]
            [schema-voyager.html.diagrams.core :as diagrams]
            [schema-voyager.html.diagrams.query :as diagrams.query]))

(defn collections [db collection-type]
  (->> (ds/q '[:find [?coll ...]
               :in $ ?collection-type
               :where
               [?coll :db.schema.collection/type ?collection-type]
               [?coll :db.schema.pseudo/type :collection]]
             db collection-type)
       (ds/pull-many db '[*])
       (sort-by :db.schema.collection/name)))

(defn entity-specs [db]
  (->> (ds/q '[:find [?spec ...]
               :where [?spec :db.schema.pseudo/type :entity-spec]]
             db)
       (ds/pull-many db '[*])
       (sort-by :db/ident)))

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
    [collection-list (collections db/db :aggregate)]]
   [list-section
    [:span.flex.items-center.gap-x-1 "Enums" [util/aggregate-abbr {:db.schema.collection/type :enum}]]
    "Enums are collections of named constants. They usually specify the various values that an attribute may take."
    [collection-list (collections db/db :enum)]]
   (when-let [specs (seq (entity-specs db/db))]
     [list-section
      "Entity Specs"
      "Entity specs are constraints that can be placed on an entity during a transaction. They require attributes, run predicate functions for validation, or both."
      [spec-list specs]])
   [:div
    ^{:key :collections}
    [diagrams/erd (diagrams.query/colls-edges db/db)]]])
