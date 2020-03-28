(ns schema-voyager.html.util
  #?(:cljs (:require [reitit.frontend.easy :as rfe])))

(def nbsp "\u00A0")

(def href
  #?(:cljs rfe/href
     ;; TODO: is there a way to generate hrefs in CLJ, without creating a
     ;; circular dependency with the routes ns?
     :clj (constantly nil)))

(defn visit [route]
  #?(:cljs (apply rfe/push-state route)
     :clj nil))

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
  [:span.whitespace-no-wrap [coll-name coll] "/" [:span.text-blue-500 "*"]])

(defn ident-name
  ([ident]
   [:span.text-blue-500 (pr-str ident)])
  ([ident coll-type] [ident-name {} ident coll-type])
  ([{:keys [coll-props ident-props]} ident coll-type]
   [:span.whitespace-no-wrap
    [coll-name coll-props {:db.schema.collection/name (keyword (namespace ident))
                           :db.schema.collection/type coll-type}]
    "/"
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
         :class    :whitespace-no-wrap
         :on-click #(.stopPropagation %)}
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
      [:span.whitespace-no-wrap
       [link {:href (coll-href coll)}
        [coll-name coll]]
       "/"
       [link {:href (attr-href attr)}
        [:span.text-blue-500 (name ident)]]
       [attr-deprecated-abbr attr]])
    [link {:class :whitespace-no-wrap
           :href  (attr-href attr)}
     [ident-name ident]
     [attr-deprecated-abbr attr]]))

(defn attr-links [attributes]
  (pipe-list (map attr-link attributes)))

(def lock-closed
  [:svg.inline.fill-none.stroke-purle-700.stroke-2.w-4.h-4 {:viewBox "0 0 24 24"}
   [:title ":db.unique/identity"]
   [:path {:stroke-linecap "round" :stroke-linejoin "round" :d "M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"}]])

(defn unique-span [{:keys [db/unique]}]
  (when (= :db.unique/identity unique)
    [:span.ml-2.text-purple-700 lock-closed]))

(def deprecated-pill
  [:span.inline-block.px-2.rounded-full.bg-gray-400.text-xs "DEPRECATED"])

(defn deprecated-span [{:keys [db.schema/deprecated?]}]
  (when deprecated?
    [:span.ml-2.inline-block.px-2.rounded-full.bg-gray-400.text-xs "DEPRECATED"]))
