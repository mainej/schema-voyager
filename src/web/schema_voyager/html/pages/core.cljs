(ns schema-voyager.html.pages.core
  (:require [schema-voyager.html.util :as util]))

(def voyage-icon
  [:svg.inline.fill-none.stroke-current.stroke-2.w-text.h-text {:viewBox "0 0 24 24"}
   [:title "Schema Voyage"]
   [:path {:stroke-linecap "round" :stroke-linejoin "round" :d "M5 3v4M3 5h4M6 17v4m-2-2h4m5-16l2.286 6.857L21 12l-5.714 2.143L13 21l-2.286-6.857L5 12l5.714-2.143L13 3z"}]])

(defn page [route]
  [:div.font-sans.text-gray-900
   [:header.bg-gray-900.shadow
    [:div.mx-auto.py-6.px-4.sm:px-6.lg:px-8
     [:h1.flex.text-3xl.font-bold.leading-none.text-white
      (if (= :route/collections (:name (:data route)))
        voyage-icon
        [:a.flex.rounded-sm.focus:outline-none.focus:ring-2.focus:ring-white.focus:ring-offset-gray-900.focus:ring-offset-2 {:href (util/href :route/collections)} voyage-icon])]]]
   [:main.bg-gray-200.border-gray-200.min-h-screen
    [:div.mx-auto.py-6.sm:px-6.lg:px-8
     (when-let [view (:view (:data route))]
       [view (:parameters route)])]]])
