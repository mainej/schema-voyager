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

(defn char-abbr [opts title]
  [:span.inline-flex.items-center.justify-center.w-6.h-6.leading-none.text-xs.font-bold.rounded-full
   opts
   [:abbr {:title title} (first title)]])

(defn aggregate-abbr [{:keys [db.schema.collection/type]}]
  (if (= :aggregate type)
    [char-abbr {:class [:bg-purple-200 :text-purple-800]} "Aggregate"]
    [char-abbr {:class [:bg-green-200 :text-green-800]} "Enumeration"]))

(defn coll-name [{:keys [db.schema.collection/name db.schema.collection/type]}]
  [:span {:class (if (= :aggregate type)
                   :text-purple-700
                   :text-green-600)}
   (pr-str name)])

(defn coll-name* [coll]
  [:span.whitespace-no-wrap [coll-name coll] "/" [:span.text-blue-500 "*"]])

(defn ident-name
  ([ident]
   [:span.text-blue-500 (pr-str ident)])
  ([ident coll-type]
   [:span.whitespace-no-wrap
    [coll-name {:db.schema.collection/name (keyword (namespace ident))
                :db.schema.collection/type coll-type}]
    "/"
    [:span.text-blue-500 (name ident)]]))

(def link :a.underline)

(defn link-list [f coll]
  (->> coll
       (map f)
       (interpose ", ")
       (into [:span])))

(defn coll-links [colls]
  (link-list (fn [coll]
               [link {:href     (coll-href coll)
                      :class    :whitespace-no-wrap
                      :on-click #(.stopPropagation %)}
                [coll-name* coll]
                " "
                [aggregate-abbr coll]])
             colls))

(defn attr-deprecated-abbr [{:keys [db.schema/deprecated?]}]
  (when deprecated?
    [:<> " "
     [char-abbr {:class [:bg-gray-300 :text-gray-800]} "Deprecated"]]))

(def attr-link-pull
  [:db/ident
   :db.schema/deprecated?
   {:db.schema/part-of ['*]}])

(defn attr-links [attributes]
  (link-list (fn [{:keys [db/ident db.schema/part-of] :as attr}]
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
             attributes))