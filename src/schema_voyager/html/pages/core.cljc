(ns schema-voyager.html.pages.core
  (:require [schema-voyager.html.util :as util]))

(def voyage-icon
  [:svg.inline.fill-none.stroke-current.stroke-2.w-8.h-8 {:viewBox "0 0 24 24"}
   [:title "Schema Voyage"]
   [:path {:stroke-linecap "round" :stroke-linejoin "round" :d "M5 3v4M3 5h4M6 17v4m-2-2h4m5-16l2.286 6.857L21 12l-5.714 2.143L13 21l-2.286-6.857L5 12l5.714-2.143L13 3z"}]])

(defn page [route]
  [:div.min-w-screen.font-sans.text-gray-900
   [:header.bg-gray-900.shadow
    [:div.max-w-7xl.mx-auto.py-6.px-4.sm:px-6.lg:px-8
     [:h1.text-3xl.font-bold.leading-tight.text-white
      (if (= :route/collections (:name (:data route)))
        voyage-icon
        [:a {:href (util/href :route/collections)} voyage-icon])]]]
   [:main.bg-gray-200.min-h-screen
    [:div.max-w-7xl.mx-auto.py-6.sm:px-6.lg:px-8
     (when-let [view (:view (:data route))]
       [view (:parameters route)])]]])
