(ns schema-voyager.html.live
  (:require [schema-voyager.data :as data]
            [schema-voyager.html.routes :as routes]
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

(re-frame/reg-sub
 ::active-panel
 :<- [::active-route]
 (fn [route _]
   (:view (:data route))))

(defn main-panel []
  [:div.min-h-screen.min-w-screen.font-sans.text-gray-900.flex.flex-col.justify-between.items-center
   [:div.container.mx-auto.p-4
    (when-let [panel @(rp/subscribe [::active-panel])]
      [panel])]])

(defn ^:export mount-root []
  (re-frame/clear-subscription-cache!)
  (reagent.dom/render [main-panel]
                      (.getElementById js/document "app")))

(defn ^:export init []
  (routes/initialize)
  (dev-setup)
  (mount-root))
