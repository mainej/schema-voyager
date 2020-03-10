(ns schema-voyager.html.components.attributes
  (:require [schema-voyager.html.util :as util]))

(defn links [attributes]
  (prn attributes)
  [:ul
   (for [{:keys [db/id db/ident db.schema/part-of]} attributes

         coll part-of]
     ^{:key id}
     [:li
      [:a {:href (util/coll-href coll)}
       (pr-str ident)]])])
