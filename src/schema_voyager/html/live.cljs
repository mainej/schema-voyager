(ns schema-voyager.html.live
  (:require [schema-voyager.html.routes :as routes]
            [schema-voyager.html.db :as db]
            [schema-voyager.html.pages.core :as page]
            [reagent.dom]))

(def debug?
  ^boolean goog.DEBUG)

(defn dev-setup []
  (when debug?
    (enable-console-print!)
    (println "dev mode")))

(defn main-view []
  [page/page (db/active-route)])

(defn ^:export mount-root []
  (reagent.dom/render [main-view]
                      (.getElementById js/document "app")))

#_{:clj-kondo/ignore #{:clojure-lsp/unused-public-var}}
(defn ^:export init []
  (routes/initialize)
  (dev-setup)
  (mount-root))
