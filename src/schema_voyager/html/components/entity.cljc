(ns schema-voyager.html.components.entity)

(def lock-closed
  [:svg.inline.fill-none.stroke-current.stroke-2.w-4.h-4 {:viewBox "0 0 24 24"}
   [:title ":db.unique/identity"]
   [:path {:stroke-linecap "round" :stroke-linejoin "round" :d "M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"}]])

(defn doc-str [{:keys [db/doc]}]
  (when doc
    [:p.text-gray-600.italic doc]))

(defn unique-span [{:keys [db/unique]}]
  (when (= :db.unique/identity unique)
    [:span.ml-2.text-purple-700 lock-closed]))

(defn deprecated-span [{:keys [db.schema/deprecated?]}]
  (when deprecated?
    [:span.ml-2.inline-block.px-2.rounded-full.bg-gray-400.text-xs "DEPRECATED"]))

(defn tuple-type [{:keys [db/tupleAttrs db/tupleType db/tupleTypes]}]
  (cond
    tupleAttrs :db.type.tuple/composite
    tupleType  :db.type.tuple/homogeneous
    tupleTypes :db.type.tuple/heterogeneous))

(defn entity-type [entity]
  (if (:db/valueType entity)
    :attribute
    :constant))
