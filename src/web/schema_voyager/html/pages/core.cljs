(ns schema-voyager.html.pages.core
  (:require
   ["@heroicons/react/outline/SparklesIcon" :as SparklesIcon]
   [schema-voyager.html.util :as util]))

(def voyage-icon
  [:> SparklesIcon {:class [:stroke-2 :w-text :h-text]}])

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
