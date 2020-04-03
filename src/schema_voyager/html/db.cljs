(ns schema-voyager.html.db
  (:require [reitit.frontend.controllers :as rfc]
            [reagent.core :as r]
            [clojure.edn :as edn]
            [shadow.resource :as resource]))

(def db
  (edn/read-string (resource/inline "schema_voyager_db.edn")))

(defonce !active-route (r/atom nil))

(defn active-route []
  (deref !active-route))

(defn save-route [new-match]
  (when new-match
    (swap! !active-route
           (fn [active-route]
             (let [controllers (rfc/apply-controllers (:controllers active-route) new-match)]
               (assoc new-match :controllers controllers))))))
