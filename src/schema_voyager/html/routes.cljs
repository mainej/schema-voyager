(ns schema-voyager.html.routes
  (:require [schema-voyager.html.pages.collections :as pages.collections]
            [schema-voyager.html.pages.enum :as pages.enum]
            [schema-voyager.html.pages.attribute :as pages.attribute]
            [schema-voyager.html.pages.aggregate :as pages.aggregate]
            [schema-voyager.html.db :as db]
            [reitit.frontend :as rf]
            [reitit.frontend.easy :as rfe]))

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
  (rfe/start! router db/save-route {:use-fragment true}))
