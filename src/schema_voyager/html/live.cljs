(ns schema-voyager.html.live
  (:require [schema-voyager.data :as data]
            [schema-voyager.html.routes :as routes]
            [schema-voyager.html.util :refer [<sub >dis] :as util]
            [shadow.resource :as resource]
            [datascript.core :as d]
            [re-posh.core :as rp]
            [re-frame.core :as re-frame]
            [reagent.dom]
            ))

(def debug?
  ^boolean goog.DEBUG)

(def mbrainz-db
  (let [schema (data/read-string (resource/inline "mbrainz-schema.edn"))
        enums  (data/read-string (resource/inline "mbrainz-enums.edn"))
        supp   (data/read-string (resource/inline "mbrainz-supplemental.edn"))]
    (data/process (data/empty-db) (-> schema
                                      (data/join enums)
                                      (data/join supp)))))

(def mbrainz-conn (d/conn-from-db mbrainz-db))

(rp/connect! mbrainz-conn)

(defn dev-setup []
  (when debug?
    (enable-console-print!)
    (println "dev mode")))

(re-frame/reg-sub
 ::active-route
 (fn [db _]
   (:active-route db)))

[:div
 [:header.bg-white.shadow
  [:div.max-w-7xl.mx-auto.py-6.px-4.sm:px-6.lg:px-8
   [:h2.text-3xl.font-bold.leading-tight.text-gray-900
    "\n        Dashboard\n      "]]]
 [:main
  [:div.max-w-7xl.mx-auto.py-6.sm:px-6.lg:px-8
   "<!-- Replace with your content -->"
   [:div.px-4.py-6.sm:px-0
    [:div.border-4.border-dashed.border-gray-200.rounded-lg.h-96]]
   "<!-- /End replace -->"]]]

(def voyage-icon
  [:svg.inline.fill-none.stroke-current.stroke-2.w-8.h-8 {:viewBox "0 0 24 24"}
   [:title "Schema Voyage"]
   [:path {:stroke-linecap "round" :stroke-linejoin "round" :d "M5 3v4M3 5h4M6 17v4m-2-2h4m5-16l2.286 6.857L21 12l-5.714 2.143L13 21l-2.286-6.857L5 12l5.714-2.143L13 3z"}]])

(defn main-view []
  (let [route (<sub [::active-route])]
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
         [view (:parameters route)])]]]))

(defn ^:export mount-root []
  (re-frame/clear-subscription-cache!)
  (reagent.dom/render [main-view]
                      (.getElementById js/document "app")))

(defn ^:export init []
  (routes/initialize)
  (dev-setup)
  (mount-root))
