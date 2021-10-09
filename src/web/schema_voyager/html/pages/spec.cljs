(ns schema-voyager.html.pages.spec
  (:require [schema-voyager.html.db :as db]
            [schema-voyager.html.util :as util]))

(defn- preds-list [preds]
  [:ul.list-disc.m-4.font-mono
   (for [pred preds]
     ^{:key pred}
     [:li (pr-str pred)])])

(defn- attrs-list [attrs]
  [:ul.list-disc.m-4
   (for [attr attrs]
     ^{:key (:db/ident attr)}
     [:li [util/attr-link attr]])])

(defn doc-str [{:keys [db/doc]}]
  (when doc
    [:p.italic doc]))

(defn- details-section [{:keys [db/doc db.entity/attrs db.entity/preds] :as spec}]
  [:div.divide-y
   (when doc
     [:div.px-4.py-6.sm:p-8
      [doc-str spec]])
   [:div.px-4.py-6.sm:p-8.space-y-8
    [:div "When placed on an entity, " [util/spec-name spec] "..."]
    (when (seq attrs)
      [:div "Requires the attributes:" [attrs-list attrs]])
    (when (seq preds)
      [:div "Validates:" [preds-list preds]])]])

(defn- header [spec]
  [:h1.font-bold
   [util/spec-name spec]])

(defn page [parameters]
  (let [spec (db/entity-spec-by-ident (keyword (:id (:path parameters))))]
    [:div.max-w-4xl.space-y-6
     [:div.px-4.sm:px-0
      [header spec]]
     [:div.sm:shadow-lg.overflow-hidden.sm:rounded-lg.bg-white
      [details-section spec]]]))
