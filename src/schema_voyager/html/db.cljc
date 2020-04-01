(ns schema-voyager.html.db
  (:require #?@(:cljs [[reitit.frontend.controllers :as rfc]
                       [reagent.core :as r]]
                :clj [[datascript.core :as ds]])
            [clojure.edn :as edn]
            [shadow.resource :as resource]))

(defn ^:private read-db-str [db-str]
  #?(:cljs (edn/read-string db-str)
     :clj (edn/read-string {:readers ds/data-readers} db-str)))

(def db
  (read-db-str (resource/inline "schema_voyager_db.edn")))

(defonce !active-route #?(:cljs (r/atom nil)
                          :clj (atom nil)))

(defn active-route []
  (deref !active-route))

(defn save-route [new-match]
  #?(:cljs
     (when new-match
       (swap! !active-route
              (fn [active-route]
                (let [controllers (rfc/apply-controllers (:controllers active-route) new-match)]
                  (assoc new-match :controllers controllers)))))))
