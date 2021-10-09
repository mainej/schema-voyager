(ns schema-voyager.html.util
  (:require [reitit.frontend.easy :as rfe]))

(def slash [:span.text-gray-500 "/"])
(def asterisk [:span.text-blue-600 "*"])

(defn stop [e]
  (.stopPropagation e))

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
    [char-abbr {:class [:bg-green-300 :text-green-800]} "Enumeration"]))

(defn coll-name
  ([coll] [coll-name {} coll])
  ([props {:keys [db.schema.collection/name db.schema.collection/type]}]
   [:span (update props :class conj (case type
                                      :aggregate :text-purple-700
                                      :enum :text-green-600
                                      :text-blue-600))
    (pr-str name)]))

(defn coll-name*
  ([coll] [coll-name* {} coll])
  ([props coll]
   [:span.whitespace-nowrap [coll-name props coll] slash asterisk]))

(defn ident-name
  ([ident coll-type] [ident-name {} ident coll-type])
  ([{:keys [coll-props ident-props]} ident coll-type]
   [:span.whitespace-nowrap
    [coll-name coll-props {:db.schema.collection/name (keyword (namespace ident))
                           :db.schema.collection/type coll-type}]
    slash
    [:span.text-blue-600 ident-props (name ident)]]))

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

(defn or-list [items]
  (if (= 1 (count items))
    (first items)
    (vec
     (concat [:span]
             (interpose ", " (butlast items))
             [" or " (last items)]))))

(def link :a.hover:underline.focus:outline-none.focus:underline)

(defn coll-link [coll]
  [:span.whitespace-nowrap
   [link {:href     (coll-href coll)
          :on-click stop}
    [coll-name* coll]]
   " "
   [aggregate-abbr coll]])

(defn coll-links [colls]
  [or-list (map coll-link colls)])

(defn coll-pipe-links [colls]
  [pipe-list (map coll-link colls)])

(defn attr-deprecated-abbr [{:keys [db.schema/deprecated?]}]
  (when deprecated?
    [:<> " "
     [char-abbr {:class [:bg-gray-300 :text-gray-800]} "Deprecated"]]))

(defn sole [coll]
  (when (= 1 (count coll))
    (first coll)))

(defn attr-link [{:keys [db/ident db.schema/part-of] :as attr}]
  [:span.whitespace-nowrap
   (if-let [coll (sole part-of)]
     ;; link to both the sole collection and the attribute
     [:span
      [link {:href (coll-href coll)} [coll-name coll]]
      slash
      [link {:href (attr-href attr)} [:span.text-blue-600 (name ident)]]]
     ;; link to just the attribute
     [link {:href (attr-href attr)}
      [ident-name ident (or (sole (distinct (map :db.schema.collection/type part-of)))
                            ;; part of both enum and aggregate, so render in default color
                            :unknown)]])
   [attr-deprecated-abbr attr]])

(defn attr-links [attributes]
  [or-list (map attr-link attributes)])

(def lock-closed
  [:svg.inline.fill-none.text-purple-700.stroke-current.stroke-2.w-4.h-4 {:viewBox "0 0 24 24"}
   [:title ":db.unique/identity"]
   [:path {:stroke-linecap "round" :stroke-linejoin "round" :d "M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"}]])

(def deprecated-pill
  [:span.inline-block.px-2.rounded-full.bg-gray-400.text-xs.uppercase.tracking-wide "deprecated"])
