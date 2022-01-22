(ns schema-voyager.html.components.toggle
  (:require [headlessui-reagent.core :as ui]))

(defn toggle [{:keys [checked on-change]} label]
  [ui/switch-group
   [ui/switch {:checked   checked
               :on-change on-change
               :class     [:relative
                           :inline-flex
                           :flex-shrink-0
                           :h-4
                           :w-6
                           :border-2
                           :border-transparent
                           :rounded-full
                           :transition-colors
                           (if checked :bg-teal-400 :bg-gray-200)
                           :focus:outline-none
                           :focus:ring-2
                           :focus:ring-offset-1
                           :focus:ring-teal-400]}
    [:span {:class [:h-3
                    :w-3
                    :rounded-full
                    :bg-white
                    :transition-transform
                    (if checked :translate-x-2 :translate-x-0)]}]]
   [ui/switch-label {:class :cursor-pointer} label]])
