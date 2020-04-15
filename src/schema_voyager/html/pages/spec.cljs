(ns schema-voyager.html.pages.spec
  (:require [datascript.core :as ds]
            [schema-voyager.html.db :as db]
            [schema-voyager.html.util :as util]))

(defn- by-ident [ident]
  (ds/pull db/db ['*] [:db/ident ident]))

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
  [:div.stack-border-y
   (when doc
     [:div.px-4.py-6.sm:p-8
      [doc-str spec]])
   [:div.px-4.py-6.sm:p-8.stack-my-8
    [:div "When placed on an entity, " [util/spec-name spec] "..."]
    (when attrs
      [:div
       "Requires the attributes:"
       [attrs-list (ds/pull-many db/db util/attr-link-pull attrs)]])
    (when preds
      [:div
       "Validates:"
       [preds-list
        (cond
          (sequential? preds) preds
          preds               [preds])]])]])

(defn- header [spec]
  [:h1.font-bold
   [util/spec-name spec]])

(defn page [parameters]
  (let [spec (by-ident (keyword (:id (:path parameters))))]
    [:div.max-w-4xl.stack-my-6
     [:div.px-4.sm:px-0
      [header spec]]
     [:div.sm:shadow-lg.overflow-hidden.sm:rounded-lg.bg-white
      [details-section spec]]]))
