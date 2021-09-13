(ns schema-voyager.html.components.toggle
  (:require [schema-voyager.html.util :as util]))

(defn handlers [on-change]
  {:on-click    on-change
   :on-key-down (fn [e]
                  (when (contains? #{" " "Enter"} (.-key e))
                    ;; prevent space from scrolling page
                    (util/prevent e)
                    (on-change)))})

(defn span [{:keys [checked] :as props}]
  [:span
   (-> props
       (dissoc :checked)
       (assoc :aria-checked (pr-str checked)
              :tabIndex     "0"
              :role         "checkbox"
              :class        [:relative
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
                             :focus:ring-teal-400]))
   [:span
    {:class       [:h-3
                   :w-3
                   :rounded-full
                   :bg-white
                   :transform
                   :transition-transform
                   (if checked :translate-x-2 :translate-x-0)]
     :aria-hidden "true"}]])

