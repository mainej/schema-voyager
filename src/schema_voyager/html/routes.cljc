(ns schema-voyager.html.routes
  (:require [schema-voyager.html.pages.collections :as pages.collections]
            [schema-voyager.html.pages.enum :as pages.enum]
            [schema-voyager.html.pages.attribute :as pages.attribute]
            [schema-voyager.html.pages.aggregate :as pages.aggregate]
            [schema-voyager.html.pages.spec :as pages.spec]
            #?@(:cljs [[schema-voyager.html.db :as db]
                       [reitit.frontend :as rf]
                       [reitit.frontend.easy :as rfe]]
                :clj [[reitit.core :as r]])))

(defn- scroll-to-top
  []
  #?(:cljs (js/window.scrollTo 0 0)
     :clj nil))

(def ^:private routes
  ["/"
   {:controllers [{:identity identity
                   :start    scroll-to-top}]}
   ["" {:name :route/collections
        :view pages.collections/page}]
   ["enum/:id" {:name :route/enum
                :view pages.enum/page}]
   ["aggregate/:id" {:name :route/aggregate
                     :view pages.aggregate/page}]
   ["spec/:id" {:name :route/spec
                :view pages.spec/page}]
   ["attribute/:id" {:name :route/attribute
                     :view pages.attribute/page}]])

(def router
  #?(:cljs (rf/router routes)
     :clj (r/router routes)))

#?(:cljs
   (defn ^:dev/after-load initialize
     []
     (rfe/start! router db/save-route {:use-fragment true})))
