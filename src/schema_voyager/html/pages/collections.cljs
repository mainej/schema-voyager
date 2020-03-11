(ns schema-voyager.html.pages.collections
  (:require [datascript.core :as d]
            [schema-voyager.html.db :as db]
            [schema-voyager.html.util :as util :refer [<sub >dis]]))

(defn collections [db collection-type]
  (->> (d/q '[:find [?coll ...]
              :in $ ?collection-type
              :where [?coll :db.schema.collection/type ?collection-type]]
            db collection-type)
       (d/pull-many db '[*])
       (sort-by :db.schema.collection/name)))

(defn collection-list [collection]
  [:ul
   (for [{:keys [db/id] :as coll} collection]
     ^{:key id}
     [:li
      [util/link {:href (util/coll-href coll)}
       [util/coll-name* coll]]])])

(defn list-section [title description collections]
  [:div.md:flex.mb-6
   [:div.md:w-1of4
    [:h1.small-caps title]
    [:p.my-4.font-light.text-gray-700.mr-4 description]]
   [:div.md:w-3of4
    [collection-list collections]]])

(defn page []
  [:div.px-4.sm:px-0
   [list-section
    "Aggregates"
    "Aggregates are collections of attributes that often co-exist on an entity. They are analogous to a SQL table, though some attributes may appear on many aggregates."
    (collections db/mbrainz-db :aggregate)]
   [list-section
    "Enums"
    "Enums are collections of named constants. They usually specify the various values that an attribute may take."
    (collections db/mbrainz-db :enum)]])
