(ns schema-voyager.html.util
  (:require [reitit.frontend.easy :as rfe]))

(def nbsp "\u00A0")

(def slash [:span.text-gray-500 "/"])
(def asterisk [:span.text-blue-500 "*"])

(defn stop [e]
  (.stopPropagation e))

(defn prevent [e]
  (.preventDefault e))

(def href rfe/href)

(defn visit [route]
  (apply rfe/push-state route))

(defn coll-href [coll]
  (href (keyword :route (:db.schema.collection/type coll))
        {:id (:db.schema.collection/name coll)}))

(defn attr-route [{:keys [db/ident]}]
  [:route/attribute {:id ident}])

(defn attr-href [attr]
  (apply href (attr-route attr)))

(defn spec-href [{:keys [db/ident]}]
  (href :route/spec {:id ident}))

(defn char-abbr [opts title]
  [:span.inline-flex.items-center.justify-center.w-6.h-6.leading-none.text-xs.font-bold.rounded-full
   opts
   [:abbr {:title title} (first title)]])

(defn aggregate-abbr [{:keys [db.schema.collection/type]}]
  (if (= :aggregate type)
    [char-abbr {:class [:bg-purple-200 :text-purple-800]} "Aggregate"]
    [char-abbr {:class [:bg-green-200 :text-green-800]} "Enumeration"]))

(defn coll-name
  ([coll] [coll-name {} coll])
  ([props {:keys [db.schema.collection/name db.schema.collection/type]}]
   [:span (update props :class conj (if (= :aggregate type)
                                      :text-purple-700
                                      :text-green-600))
    (pr-str name)]))

(defn coll-name* [coll]
  [:span.whitespace-nowrap [coll-name coll] slash asterisk])

(defn ident-name
  ([ident]
   [:span.text-blue-500 (pr-str ident)])
  ([ident coll-type] [ident-name {} ident coll-type])
  ([{:keys [coll-props ident-props]} ident coll-type]
   [:span.whitespace-nowrap
    [coll-name coll-props {:db.schema.collection/name (keyword (namespace ident))
                           :db.schema.collection/type coll-type}]
    slash
    [:span.text-blue-500 ident-props (name ident)]]))

(defn spec-name [spec]
  [:span.text-orange-500 (pr-str (:db/ident spec))])

(defn pipe-list [items]
  (->> items
       (interpose " | ")
       (into [:span])))

(defn comma-list [items]
  (->> items
       (interpose ", ")
       (into [:span])))

(def link :a.hover:underline)

(defn coll-link [coll]
  [link {:href     (coll-href coll)
         :class    :whitespace-nowrap
         :on-click stop}
   [coll-name* coll]
   " "
   [aggregate-abbr coll]])

(defn coll-links [colls]
  (pipe-list (map coll-link colls)))

(defn attr-deprecated-abbr [{:keys [db.schema/deprecated?]}]
  (when deprecated?
    [:<> " "
     [char-abbr {:class [:bg-gray-300 :text-gray-800]} "Deprecated"]]))

(def attr-link-pull
  [:db/ident
   :db.schema/deprecated?
   {:db.schema/part-of ['*]}])

(defn attr-link [{:keys [db/ident db.schema/part-of] :as attr}]
  (if (= 1 (count part-of))
    (let [coll (first part-of)]
      [:span.whitespace-nowrap
       [link {:href (coll-href coll)}
        [coll-name coll]]
       slash
       [link {:href (attr-href attr)}
        [:span.text-blue-500 (name ident)]]
       [attr-deprecated-abbr attr]])
    [link {:class :whitespace-nowrap
           :href  (attr-href attr)}
     [ident-name ident]
     [attr-deprecated-abbr attr]]))

(defn attr-links [attributes]
  (pipe-list (map attr-link attributes)))

(def lock-closed
  [:svg.inline.fill-none.text-purple-700.stroke-current.stroke-2.w-4.h-4 {:viewBox "0 0 24 24"}
   [:title ":db.unique/identity"]
   [:path {:stroke-linecap "round" :stroke-linejoin "round" :d "M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"}]])

(def deprecated-pill
  [:span.inline-block.px-2.rounded-full.bg-gray-400.text-xs "DEPRECATED"])
