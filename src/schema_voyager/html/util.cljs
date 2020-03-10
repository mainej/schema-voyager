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
  [:db/ident])

(def attribute-pull
  ['*
   {:db.schema/part-of    ['*]
    :db.schema/references ['*]
    :db.schema/see-also   attr-link-pull}])

(defn aggregate-abbr [{:keys [db.schema.collection/type]}]
  (if (= :aggregate type)
    [:div.inline-flex.items-center.justify-center.w-6.h-6.leading-none.text-xs.font-bold.rounded-full.bg-purple-200.text-purple-800 [:abbr {:title "Aggregate"} "A"]]
    [:div.inline-flex.items-center.justify-center.w-6.h-6.leading-none.text-xs.font-bold.rounded-full.bg-green-200.text-green-800 [:abbr {:title "Enumeration"} "E"]]))

(defn coll-name [{:keys [db.schema.collection/name]}]
  (str (pr-str name) "/*"))

(defn coll-links [colls]
  (into
   [:span]
   (interpose ", "
              (for [coll colls]
                [:a.text-blue-500.hover:underline {:href (coll-href coll)}
                 (coll-name coll)
                 " "
                 [aggregate-abbr coll]]))))

(defn attr-links [attributes]
  (into
   [:span]
   (interpose ", "
              (for [{:keys [db/ident] :as attr} attributes]
                [:a.text-blue-500.hover:underline {:href (attr-href attr)}
                 (pr-str ident)]))))
