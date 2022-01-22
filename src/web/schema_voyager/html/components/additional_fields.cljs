(ns schema-voyager.html.components.additional-fields)

(defn additional-fields [fields]
  (when (seq fields)
    [:dl.divide-y
     (for [[field value] (sort-by first fields)]
       ^{:key field}
       [:div.sm:flex.p-4.sm:p-6
        [:dt.sm:w-1|3 (pr-str field)]
        [:dd (pr-str value)]])]))
