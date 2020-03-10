(ns schema-voyager.html.util
  (:require [re-frame.core :as re-frame]
            [reitit.frontend.easy :as rfe]))

(def <sub (comp deref re-frame/subscribe))
(def >dis re-frame/dispatch)
(def href rfe/href)

(defn coll-href [coll]
  (href (keyword :route (:db.schema.collection/type coll))
        {:id (name (:db.schema.collection/name coll))}))

(def attr-link-pull
  [:db/ident
   {:db.schema/part-of ['*]}])
