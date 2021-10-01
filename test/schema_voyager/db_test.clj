(ns schema-voyager.db-test
  (:require [schema-voyager.db :as db]
            [schema-voyager.cli :as cli]
            [clojure.walk :as walk]
            [clojure.test :as t]))

(defn sanitize [result]
  (walk/postwalk (fn [x] (if (map? x)
                           (dissoc x :db/id :db.schema.pseudo/type)
                           x))
                 result))

(defn ingest [sources]
  (cli/ingest-into-db {:sources sources}))

(t/deftest ingestion-derivation
  (let [db (ingest [{:static/data [{:db/ident :an.enum/a-constant}
                                   {:db/ident :an.enum/another-constant}
                                   {:db/ident       :an.agg/an-attr
                                    :db/valueType   :db.type/long
                                    :db/cardinality :db.cardinality/one}
                                   {:db/ident       :an.agg/another-attr
                                    :db/valueType   :db.type/long
                                    :db/cardinality :db.cardinality/one}
                                   {:db/ident        :an.agg/guard
                                    :db.entity/attrs [:an.agg/an-attr :an.agg/another-attr]
                                    :db.entity/preds 'entity-preds/valid?}]}])]
    (t/testing "constants"
      (t/is (= {:db/ident          :an.enum/a-constant
                :db.schema/part-of [#schema/enum :an.enum]}
               (sanitize (db/attribute-by-ident db :an.enum/a-constant)))))
    (t/testing "attributes"
      (t/is (= {:db/cardinality    :db.cardinality/one
                :db/ident          :an.agg/an-attr
                :db/valueType      :db.type/long
                :db.schema/part-of [#schema/agg :an.agg]}
               (sanitize (db/attribute-by-ident db :an.agg/an-attr)))))
    (t/testing "entity specs"
      (t/is (= {:db/ident        :an.agg/guard
                :db.entity/attrs [{:db/ident          :an.agg/an-attr
                                   :db.schema/part-of [#schema/agg :an.agg]}
                                  {:db/ident          :an.agg/another-attr
                                   :db.schema/part-of [#schema/agg :an.agg]}]
                :db.entity/preds ['entity-preds/valid?]}
               (sanitize (db/entity-spec-by-ident db :an.agg/guard)))))
    (t/testing "collection unification"
      (t/is (= (:db.schema/part-of (db/attribute-by-ident db :an.enum/a-constant))
               (:db.schema/part-of (db/attribute-by-ident db :an.enum/another-constant))))
      (t/is (= (:db.schema/part-of (db/attribute-by-ident db :an.agg/an-attr))
               (:db.schema/part-of (db/attribute-by-ident db :an.agg/another-attr)))))))

(t/deftest supplemental-attribute-props
  (let [db (ingest [{;; base schema
                     :static/data [{:db/ident       :track/artists
                                    :db/valueType   :db.type/ref
                                    :db/cardinality :db.cardinality/many}
                                   {:db/ident       :track/artistCredit
                                    :db/valueType   :db.type/string
                                    :db/cardinality :db.cardinality/one}

                                   {:db/ident       :car.make/name
                                    :db/valueType   :db.type/string
                                    :db/cardinality :db.cardinality/one}
                                   {:db/ident       :timestamp/updated-at
                                    :db/valueType   :db.type/inst
                                    :db/cardinality :db.cardinality/one}


                                   {:db/ident       :address/country
                                    :db/valueType   :db.type/ref
                                    :db/cardinality :db.cardinality/one}
                                   {:db/ident       :address/region
                                    :db/valueType   :db.type/ref
                                    :db/cardinality :db.cardinality/one}
                                   {:db/ident       :country/name
                                    :db/valueType   :db.type/string
                                    :db/cardinality :db.cardinality/one}
                                   {:db/ident       :country/alpha-3
                                    :db/valueType   :db.type/string
                                    :db/cardinality :db.cardinality/one}
                                   {:db/ident :region.usa/new-york}
                                   {:db/ident :region.usa/california}
                                   {:db/ident :region.can/quebec}


                                   {:db/ident       :post/ranked-comments
                                    :db/valueType   :db.type/tuple
                                    :db/tupleTypes  [:db.type/long :db.type/ref]
                                    :db/cardinality :db.cardinality/many}

                                   {:db/ident       :label/top-artists
                                    :db/valueType   :db.type/tuple
                                    :db/tupleType   :db.type/ref
                                    :db/cardinality :db.cardinality/one
                                    :db/doc         "References to the top selling 0-5 artists signed to this label."}
                                   ]}
                    ;; supplemental schema
                    {:static/data [{:db/ident              :track/artistCredit
                                    :db.schema/deprecated? true
                                    :db.schema/see-also    [{:db/ident :track/artists}]}

                                   {:db/ident          :car.make/name
                                    :db.schema/part-of [#schema/agg :car]}
                                   {:db/ident          :timestamp/updated-at
                                    :db.schema/part-of [#schema/agg :post
                                                        #schema/agg :comment]}

                                   {:db/ident             :address/country
                                    :db.schema/references [#schema/agg :country]}
                                   {:db/ident             :address/region
                                    :db.schema/references [#schema/enum :region.usa
                                                           #schema/enum :region.can]}

                                   {:db/ident                   :post/ranked-comments
                                    :db.schema/tuple-references [{:db.schema.tuple/position 1
                                                                  :db.schema/references     [#schema/agg :comment]}]}

                                   {:db/ident             :label/top-artists
                                    :db.schema/references [#schema/agg :artist]}
                                   ]}])]
    (t/testing "see-also and deprecated?"
      (t/is (= {:db.schema/deprecated? true
                :db.schema/see-also    [{:db/ident          :track/artists
                                         :db.schema/part-of [#schema/agg :track]}]}
               (-> (db/attribute-by-ident db :track/artistCredit)
                   sanitize
                   (select-keys [:db.schema/deprecated? :db.schema/see-also]))))
      (t/is (= [{:db/ident              :track/artistCredit
                 :db.schema/deprecated? true
                 :db.schema/part-of     [#schema/agg :track]}]
               (-> (db/attribute-by-ident db :track/artists)
                   sanitize
                   :db.schema/noted-by))))
    (t/testing "override part-of"
      (t/is (= [#schema/agg :car]
               (-> (db/attribute-by-ident db :car.make/name)
                   sanitize
                   :db.schema/part-of)))
      (t/is (= [#schema/agg :post
                #schema/agg :comment]
               (-> (db/attribute-by-ident db :timestamp/updated-at)
                   sanitize
                   :db.schema/part-of))))
    (t/testing "references"
      (t/is (= [#schema/agg :country]
               (-> (db/attribute-by-ident db :address/country)
                   sanitize
                   :db/valueType)))

      (t/is (= [#schema/enum :region.usa
                #schema/enum :region.can]
               (-> (db/attribute-by-ident db :address/region)
                   sanitize
                   :db/valueType))))
    (t/testing "heterogeneous tuple references"
      (let [attr (sanitize (db/attribute-by-ident db :post/ranked-comments))]
        (t/is (= :db.type/tuple.heterogeneous
                 (:db/valueType attr)))
        (t/is (= [:db.type/long [#schema/agg :comment]]
                 (:db/tupleTypes attr)))))
    (t/testing "homogeneous tuple references"
      (let [attr (sanitize (db/attribute-by-ident db :label/top-artists))]
        (t/is (= :db.type/tuple.homogeneous
                 (:db/valueType attr)))
        (t/is (= [#schema/agg :artist]
                 (:db/tupleType attr)))))))

(t/deftest attr-page-query
  (let [db (ingest [{:static/data [{:db/ident       :a/zzz
                                    :db/valueType   :db.type/uuid
                                    :db/unique      :db.unique/identity
                                    :db/cardinality :db.cardinality/one}

                                   {:db/ident              :a/aaa
                                    :db/valueType          :db.type/string
                                    :db/cardinality        :db.cardinality/one
                                    :db.schema/deprecated? true}

                                   {:db/ident       :a/zzz+aaa
                                    :db/valueType   :db.type/tuple
                                    :db/cardinality :db.cardinality/one
                                    :db/tupleAttrs  [:a/zzz :a/aaa]}]}])]
    ;; there are lots of other things to test about an attr page query, but most
    ;; of it is done above
    (t/is (= {:db/cardinality    :db.cardinality/one
              :db/ident          :a/zzz+aaa
              :db/tupleAttrs     [{:db/ident          :a/zzz
                                   :db.schema/part-of [#schema/agg :a]}
                                  {:db/ident              :a/aaa
                                   :db.schema/deprecated? true
                                   :db.schema/part-of     [#schema/agg :a]}]
              :db/valueType      :db.type/tuple.composite
              :db.schema/part-of [#schema/agg :a]}
             (sanitize (db/attribute-by-ident db :a/zzz+aaa))))))

(def agg-with-incoming-and-outgoing-references
  [{:db/ident       :b/id
    :db/cardinality :db.cardinality/one
    :db/valueType   :db.type/uuid}

   {:db/ident             :a/bs
    :db/cardinality       :db.cardinality/many
    :db/valueType         :db.type/ref
    :db.schema/references [#schema/agg :b]}
   {:db/ident                   :a/pos+b
    :db/cardinality             :db.cardinality/one
    :db/valueType               :db.type/tuple
    :db/tupleTypes              [:db.type/long :db.type/ref]
    :db.schema/tuple-references [{:db.schema.tuple/position 1
                                  :db.schema/references     #schema/agg :b}]}

   {:db/ident       :a/zzz
    :db/valueType   :db.type/uuid
    :db/unique      :db.unique/identity
    :db/cardinality :db.cardinality/one}

   {:db/ident              :a/aaa
    :db/valueType          :db.type/string
    :db/cardinality        :db.cardinality/one
    :db.schema/deprecated? true}

   {:db/ident       :a/zzz+aaa
    :db/valueType   :db.type/tuple
    :db/cardinality :db.cardinality/one
    :db/tupleAttrs  [:a/zzz :a/aaa]}

   {:db/ident             :c/a
    :db/valueType         :db.type/ref
    :db/cardinality       :db.cardinality/one
    :db.schema/references [#schema/agg :a]}
   {:db/ident                   :c/pos+a
    :db/cardinality             :db.cardinality/one
    :db/valueType               :db.type/tuple
    :db/tupleTypes              [:db.type/long :db.type/ref]
    :db.schema/tuple-references [{:db.schema.tuple/position 1
                                  :db.schema/references     #schema/agg :a}]}
   ])

(t/deftest collection-query
  (let [db   (ingest [{:static/data agg-with-incoming-and-outgoing-references}])
        coll (sanitize (db/collection-by-type-and-name db :aggregate :a))]
    (t/is (= [;; unique first
              {:db/ident       :a/zzz
               :db/valueType   :db.type/uuid
               :db/unique      :db.unique/identity
               :db/cardinality :db.cardinality/one}
              ;; alphabetical second
              {:db/ident       :a/bs
               :db/cardinality :db.cardinality/many
               ;; includes references
               :db/valueType   [#schema/agg :b]}
              {:db/ident       :a/pos+b
               :db/cardinality :db.cardinality/one
               :db/valueType   :db.type/tuple.heterogeneous
               ;; includes tuple references
               :db/tupleTypes  [:db.type/long [#schema/agg :b]]}
              ;;includes tupleAttrs
              {:db/ident       :a/zzz+aaa
               :db/valueType   :db.type/tuple.composite
               :db/cardinality :db.cardinality/one
               :db/tupleAttrs  [{:db/ident          :a/zzz
                                 :db.schema/part-of [#schema/agg :a]}
                                {:db/ident              :a/aaa
                                 :db.schema/deprecated? true
                                 :db.schema/part-of     [#schema/agg :a]}]}

              ;; deprecated last
              {:db/ident              :a/aaa
               :db/valueType          :db.type/string
               :db/cardinality        :db.cardinality/one
               :db.schema/deprecated? true}]
             (map #(dissoc % :db.schema/part-of)
                  (:db.schema.collection/attributes coll))))
    (t/is (= [;; alphabetical
              ;; from references
              {:db/ident          :c/a
               :db.schema/part-of [#schema/agg :c]}
              ;; from tuple references
              {:db/ident          :c/pos+a
               :db.schema/part-of [#schema/agg :c]}]
             (:db.schema.collection/referenced-by-attrs coll)))))

(t/deftest homepage-queries
  (let [db (ingest [{:static/data [{:db/ident       :z/a
                                    :db/valueType   :db.type/string
                                    :db/cardinality :db.cardinality/one}
                                   {:db/ident       :a/a
                                    :db/valueType   :db.type/string
                                    :db/cardinality :db.cardinality/one}
                                   {:db/ident :e/c}
                                   {:db/ident :z/c}
                                   {:db/ident        :s/s
                                    :db.entity/attrs [:a/a :e/c]}]}])]
    (t/is (= [;; alphabetical
              #schema/agg :a
              #schema/agg :z]
             (sanitize (db/collections-by-type db :aggregate))))
    (t/is (= [;; alphabetical
              #schema/enum :e
              #schema/enum :z]
             (sanitize (db/collections-by-type db :enum))))
    (t/is (= [{:db/ident :s/s}]
             (sanitize (db/entity-specs db))))))

(defn- sanitize-edge-query [edges]
  (->> edges
       sanitize
       (map (fn [[src attr dest]]
              [src (:db/ident attr) dest]))
       set))

(t/deftest full-diagram-query
  (let [db (ingest [{:static/data (conj agg-with-incoming-and-outgoing-references
                                        {:db/ident              :a/dep
                                         :db/valueType          :db.type/ref
                                         :db/cardinality        :db.cardinality/one
                                         :db.schema/references  [#schema/agg :b]
                                         :db.schema/deprecated? true}
                                        {:db/ident             :z/b
                                         :db/valueType         :db.type/ref
                                         :db/cardinality       :db.cardinality/one
                                         :db.schema/references [#schema/agg :b]})}])]
    ;; * regular and tuple references
    ;; * excluding deprecated attributes
    (t/is (= #{[#schema/agg :a :a/pos+b #schema/agg :b]
               [#schema/agg :a :a/bs    #schema/agg :b]
               [#schema/agg :c :c/a     #schema/agg :a]
               [#schema/agg :c :c/pos+a #schema/agg :a]
               [#schema/agg :z :z/b     #schema/agg :b]}
             (sanitize-edge-query (db/colls-edges db))))))

(t/deftest collection-diagram-query
  (let [db (ingest [{:static/data (conj agg-with-incoming-and-outgoing-references
                                        {:db/ident              :a/dep
                                         :db/valueType          :db.type/ref
                                         :db/cardinality        :db.cardinality/one
                                         :db.schema/references  [#schema/agg :b]
                                         :db.schema/deprecated? true}
                                        {:db/ident             :z/b
                                         :db/valueType         :db.type/ref
                                         :db/cardinality       :db.cardinality/one
                                         :db.schema/references [#schema/agg :b]})}])]
    ;; same as full-diagram-query, but only edges where this collection is
    ;; either the source or dest
    (t/is (= #{[#schema/agg :a :a/pos+b #schema/agg :b]
               [#schema/agg :a :a/bs    #schema/agg :b]
               [#schema/agg :c :c/a     #schema/agg :a]
               [#schema/agg :c :c/pos+a #schema/agg :a]}
             (sanitize-edge-query (db/coll-edges db #schema/agg :a))))))

(t/deftest attribute-diagram-query
  (let [db (ingest [{:static/data agg-with-incoming-and-outgoing-references}])]
    ;; only edges for this attribute, either regular or tuple references
    (t/is (= #{[#schema/agg :a :a/bs #schema/agg :b]}
             (sanitize-edge-query (db/attr-edges db {:db/ident :a/bs}))))
    (t/is (= #{[#schema/agg :a :a/pos+b #schema/agg :b]}
             (sanitize-edge-query (db/attr-edges db {:db/ident :a/pos+b}))))))
