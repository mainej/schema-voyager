(ns schema-voyager.html.util
  (:require [re-frame.core :as re-frame]
            [reitit.frontend.easy :as rfe]))

(def <sub (comp deref re-frame/subscribe))
(def >dis re-frame/dispatch)
(def href rfe/href)

(defn coll-href [coll]
  (href (keyword :route (:db.schema.collection/type coll))
        {:id (:db.schema.collection/name coll)}))

(defn attr-href [attr]
  (href :route/attribute {:id (:db/ident attr)}))

(def attr-link-pull
  [:db/ident
   :db.schema/deprecated?
   {:db.schema/part-of ['*]}])

(defn char-abbr [opts title]
  [:div.inline-flex.items-center.justify-center.w-6.h-6.leading-none.text-xs.font-bold.rounded-full opts [:abbr {:title title} (first title)]])

(defn aggregate-abbr [{:keys [db.schema.collection/type]}]
  (if (= :aggregate type)
    [char-abbr {:class [:bg-purple-200 :text-purple-800]} "Aggregate"]
    [char-abbr {:class [:bg-green-200 :text-green-800]} "Enumeration"]))

(defn coll-name [{:keys [db.schema.collection/name]}]
  (str (pr-str name) "/*"))

(def link :a.text-blue-500.hover:underline)

(defn link-list [f coll]
  (->> coll
       (map f)
       (interpose ", ")
       (into [:span])))

(defn coll-links [colls]
  (link-list (fn [coll]
               [link {:href (coll-href coll)}
                (coll-name coll)
                " "
                [aggregate-abbr coll]])
             colls))

(defn attr-deprecated-abbr [{:keys [db.schema/deprecated?]}]
  (when deprecated?
    [:<> " "
     [char-abbr {:class [:bg-gray-300 :text-gray-800]} "Deprecated"]]))

(defn attr-links [attributes]
  (link-list (fn [{:keys [db/ident db.schema/part-of] :as attr}]
               (if (= 1 (count part-of))
                 [:span
                  [link {:href (coll-href (first part-of))}
                   ":" (namespace ident)]
                  "/"
                  [link {:href (attr-href attr)} (name ident)]
                  [attr-deprecated-abbr attr]]
                 [link {:href (attr-href attr)}
                  (pr-str ident)
                  [attr-deprecated-abbr attr]]))
             attributes))
