(ns schema-voyager.html.pages.collections
  (:require [datascript.core :as d]
            [schema-voyager.html.db :as db]
            [schema-voyager.html.util :as util]
            [schema-voyager.html.diagrams.collection :as diagrams.collection]))

(defn collections [db collection-type]
  (->> (d/q '[:find [?coll ...]
              :in $ ?collection-type
              :where
              [?coll :db.schema.collection/type ?collection-type]
              [?coll :db.schema.pseudo/type :collection]]
            db collection-type)
       (d/pull-many db '[*])
       (sort-by :db.schema.collection/name)))

(defn entity-specs [db]
  (->> (d/q '[:find [?spec ...]
              :where [?spec :db.schema.pseudo/type :entity-spec]]
            db)
       (d/pull-many db '[*])
       (sort-by :db/ident)))

(defn references [db]
  (->> (d/q '[:find ?source-name ?source-type ?dest-name ?dest-type
              :where
              [?source-attr :db.schema/part-of ?source]
              (or-join [?source-attr ?dest]
                       [?source-attr :db.schema/references ?dest]
                       (and
                        [?source-attr :db.schema/tuple-references ?dest-tuple-ref]
                        [?dest-tuple-ref :db.schema/references ?dest]))
              (not [?source-attr :db.schema/deprecated? true])
              [?source :db.schema.collection/name ?source-name]
              [?source :db.schema.collection/type ?source-type]
              [?source :db.schema.pseudo/type :collection]
              [?dest :db.schema.collection/name ?dest-name]
              [?dest :db.schema.collection/type ?dest-type]
              [?dest :db.schema.pseudo/type :collection]]
            db)
       (map (fn [[source-name source-type dest-name dest-type]]
              [{:db.schema.collection/name source-name
                :db.schema.collection/type source-type}
               {:db.schema.collection/name dest-name
                :db.schema.collection/type dest-type}]))))

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
  [:div.md:flex.mb-6
   [:div.md:w-1of4
    [:h1.small-caps title]
    [:p.my-4.font-light.text-gray-700.mr-4 description]]
   [:div.md:w-3of4 l]])

(defn page []
  [:div.px-4.sm:px-0
   [list-section
    "Aggregates"
    "Aggregates are collections of attributes that often co-exist on an entity. They are analogous to a SQL table, though some attributes may appear on many aggregates."
    [collection-list (collections db/db :aggregate)]]
   [list-section
    "Enums"
    "Enums are collections of named constants. They usually specify the various values that an attribute may take."
    [collection-list (collections db/db :enum)]]
   (when-let [specs (seq (entity-specs db/db))]
     [list-section
      "Entity Specs"
      "Entity specs are constraints that can be placed on an entity during a transaction. They require attributes, run predicate functions for validation, or both."
      [spec-list specs]])
   [:div.mt-10
    [diagrams.collection/force-graph [800 600] (references db/db)]]])
