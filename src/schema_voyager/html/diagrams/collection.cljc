(ns schema-voyager.html.diagrams.collection
  (:require #?(:cljs [oz.core :as oz])
            [schema-voyager.html.util :as util]))

(defn vega [schema]
  #?(:cljs [oz/vega schema]
     :clj [:div]))

(defn coll-id [{coll-type :db.schema.collection/type
                coll-name :db.schema.collection/name}]
  (str (name coll-type) "__" (name coll-name)))

(defn coll-datum [{coll-type :db.schema.collection/type
                   coll-name :db.schema.collection/name
                   :as       coll}]
  {:id       (coll-id coll)
   :name     (pr-str coll-name)
   :colltype (name coll-type)
   :href     (util/coll-href coll)})

(defn force-graph
  "Generates a force-directed graph of collections and their relationships.
  `references` is a seq of pairs of related collections: `[source-collection
  target-collection]`."
  [references]
  (let [nodes               (->> references
                                 (mapcat identity)
                                 (map #(select-keys % [:db.schema.collection/name
                                                       :db.schema.collection/type]))
                                 distinct
                                 (map coll-datum))
        node-idx-by-node-id (zipmap (map :id nodes) (range))
        links               (->> references
                                 (map (fn [[source target]]
                                        {:source (get node-idx-by-node-id (coll-id source))
                                         :target (get node-idx-by-node-id (coll-id target))})))]
    [vega
     {:$schema "https://vega.github.io/schema/vega/v5.json"
      :height  400
      :width   400
      :padding 5
      :signals [{:name "cx", :update "width / 2"}
                {:name "cy", :update "height / 2"}
                (cond-> {:name "jiggle" :value 100}
                  (> (count references) 3)
                  (assoc :bind {:input "range", :min 25, :max 500, :step 10}))]
      :data    [{:name   "link-data"
                 :values links}
                {:name      "forces"
                 :values    nodes
                 :transform [{:type          "force"
                              :iterations    300
                              :velocityDecay 0.4
                              :forces        [{:force "center"
                                               :x     {:signal "cx"}
                                               :y     {:signal "cy"}}
                                              {:force  "collide"
                                               :radius 8}
                                              {:force    "nbody"
                                               :strength -45}
                                              {:force    "link"
                                               :links    "link-data"
                                               :distance {:signal "jiggle"}}]}]}]
      :scales  [{:name   "color"
                 :type   "ordinal"
                 :range  ["#6b46c1" "#38a169"] ;; purple-700 green-600
                 :domain ["aggregate" "enum"]}]
      :marks   [;; edges
                {:type      "path"
                 :from      {:data "link-data"}
                 :encode    {:enter  {:stroke      {:value "#4299e1"} ;; blue-500
                                      :strokeWidth {:value 0.5}}
                             ;; :update required, but why?
                             :update {}}
                 :transform [{:type    "linkpath"
                              :shape   "line"
                              :sourceX "datum.source.x"
                              :sourceY "datum.source.y"
                              :targetX "datum.target.x"
                              :targetY "datum.target.y"}]}
                ;; arrow-heads
                {:type   "symbol"
                 :from   {:data "link-data"}
                 :encode {:enter  {:fill        {:value "#4299e1"} ;; blue-500
                                   :size        {:value 300}
                                   :strokeWidth {:value 0}
                                   :shape       {:value "wedge"}}
                          :update {:x     {:signal "datum.target.x - ((datum.target.x - datum.source.x) / 5)"}
                                   :y     {:signal "datum.target.y - ((datum.target.y - datum.source.y) / 5)"}
                                   :angle {:signal "90 + 180 * atan2((datum.target.y - datum.source.y), (datum.target.x - datum.source.x)) / PI"}}}}
                ;; nodes
                {:type   "text"
                 :from   {:data "forces"}
                 :encode {:enter  {:fontSize {:value 12}
                                   :fill     {:scale "color", :field "colltype"}
                                   :href     {:field "href"}
                                   :text     {:field "name"}
                                   :baseline {:value "middle"}
                                   :cursor   {:value "pointer"}}
                          :update {:x {:field "x"}
                                   :y {:field "y"}}}}]}]))
