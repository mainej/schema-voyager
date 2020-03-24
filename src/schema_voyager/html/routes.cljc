(ns schema-voyager.html.routes
  (:require [schema-voyager.html.pages.collections :as pages.collections]
            [schema-voyager.html.pages.enum :as pages.enum]
            [schema-voyager.html.pages.attribute :as pages.attribute]
            [schema-voyager.html.pages.aggregate :as pages.aggregate]
            [schema-voyager.html.db :as db]
            #?@(:cljs [[reitit.frontend :as rf]
                       [reitit.frontend.easy :as rfe]]
                :clj [[reitit.core :as r]])))

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
  #?(:cljs (rf/router routes)
     :clj (r/router routes)))

(defn ^:dev/after-load initialize
  []
  #?(:cljs
     (rfe/start! router db/save-route {:use-fragment true})))
