(ns schema-voyager.html.routes
  (:require [re-frame.core :as re-frame]
            [schema-voyager.html.pages.collections :as pages.collections]
            [schema-voyager.html.pages.enum :as pages.enum]
            [schema-voyager.html.pages.attribute :as pages.attribute]
            [schema-voyager.html.pages.aggregate :as pages.aggregate]
            [reitit.frontend :as rf]
            [reitit.frontend.easy :as rfe]
            [reitit.frontend.controllers :as rfc]))


(def ^:private routes
  ["/"
   ["" {:name :route/collections
        :view pages.collections/page}]
   ["enum/:id" {:name :route/enum
                :view pages.enum/page}]
   ["aggregate/:id" {:name :route/aggregate
                     :view pages.aggregate/page}]
   ["attribute/:id" {:name :route/attribute
                     :view pages.attribute/page}]])

(def ^:private router
  (rf/router routes))

(defn ^:dev/after-load initialize
  []
  (rfe/start!
   router
   #(re-frame/dispatch [::navigating %])
   {:use-fragment true}))

(re-frame/reg-event-fx
 ::navigating
 re-frame/trim-v
 (fn [{:keys [db]} [new-match]]
   {::navigating {:active-route (:active-route db)
                  :new-match    new-match}}))

(defn- navigating [{:keys [active-route new-match]}]
  (when new-match
    (let [controllers (rfc/apply-controllers (:controllers active-route) new-match)
          new-match   (assoc new-match :controllers controllers)]
      (re-frame/dispatch [::navigated new-match])
      new-match)))

(re-frame/reg-fx ::navigating navigating)

(re-frame/reg-event-db
 ::navigated
 (fn [db [_ new-match]]
   (assoc db :active-route new-match)))

;; Triggering navigation from events.
(re-frame/reg-fx
 :visit
 (fn [route]
   (apply rfe/push-state route)))
