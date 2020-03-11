(ns schema-voyager.html.db
  (:require [reitit.frontend.controllers :as rfc]
            [clojure.edn :as edn]
            [reagent.core :as r]
            [shadow.resource :as resource]))

(def db
  (edn/read-string (resource/inline "db.edn")))

(defonce !active-route (r/atom nil))

(defn active-route []
  (deref !active-route))

(defn save-route [new-match]
  (when new-match
    (swap! !active-route
           (fn [active-route]
             (let [controllers (rfc/apply-controllers (:controllers active-route) new-match)]
               (assoc new-match :controllers controllers))))))
