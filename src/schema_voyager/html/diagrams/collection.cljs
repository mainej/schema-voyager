(ns schema-voyager.html.diagrams.collection
  (:require [oz.core :as oz]))

(defn datum [{:keys     [ db.schema.collection/type]
              coll-name :db.schema.collection/name}]
  {:id       coll-name
   :name     (str (name coll-name) "/*")
   :colltype (name type)})

(defn radial-tree [{:keys [db.schema/_part-of db.schema/_references]
                    :as   coll}]
  (when-let [references (seq (mapcat :db.schema/references _part-of))]
    [:div.mt-5
     [oz/vega
      {:description "An example of a radial layout for a node-link diagram of hierarchical data."
       :$schema     "https://vega.github.io/schema/vega/v5.json"
       :height      320
       :width       320
       :padding     5
       :signals     [{:name "originY", :update "height / 2"}]
       :data        [{:name      "tree"
                      :values    (let [root (datum coll)]
                                   (concat
                                    [root]
                                    (for [coll references]
                                      (assoc (datum coll) :parent (:id root)))))
                      :transform [{:type      "stratify"
                                   :key       "id"
                                   :parentKey "parent"}
                                  {:type   "tree"
                                   :method "tidy" ;; ["tidy" "cluster"]
                                   :size   [1 90]
                                   :as     ["alpha" "radius" "depth" "children"]}
                                  {:as   "angle"
                                   :type "formula", :expr "(180 * datum.alpha + 270) % 360"}
                                  {:as   "radians"
                                   :type "formula", :expr "PI * datum.angle / 180"}
                                  {:as   "leftside"
                                   :type "formula", :expr "inrange(datum.angle, [90, 270])"}
                                  {:as   "x"
                                   :type "formula", :expr "datum.radius * cos(datum.radians)"}
                                  {:as   "y"
                                   :type "formula", :expr "datum.radius * sin(datum.radians) + originY"}]}
                     {:name      "links"
                      :source    "tree"
                      :transform [{:type "treelinks"}
                                  {:type    "linkpath"
                                   :shape   "diagonal" ;; ["line" "curve" "diagonal" "orthogonal"]
                                   :orient  "radial"
                                   :sourceX "source.radians"
                                   :sourceY "source.radius"
                                   :targetX "target.radians"
                                   :targetY "target.radius"}]}]
       :scales      [{:name   "color"
                      :type   "ordinal"
                      :range  ["#6b46c1" "#38a169"] ;; purple-700 green-600
                      :domain ["aggregate" "enum"]}]
       :marks       [{:type   "path"
                      :from   {:data "links"}
                      :encode {:update {:x      {:value 0}
                                        :y      {:signal "originY"}
                                        :path   {:field "path"}
                                        :stroke {:value "#ccc"}}}}
                     {:type   "symbol"
                      :from   {:data "tree"}
                      :encode {:enter  {:size   {:value 100}
                                        :stroke {:value "#fff"}}
                               :update {:x    {:field "x"}
                                        :y    {:field "y"}
                                        :fill {:scale "color"
                                               :field "colltype"}}}}
                     {:type   "text"
                      :from   {:data "tree"}
                      :encode {:enter  {:text     {:field "name"}
                                        :fontSize {:value 16}
                                        :baseline {:value "middle"}}
                               :update {:x     {:field "x"}
                                        :y     {:field "y"}
                                        :dx    {:signal "(datum.leftside ? -1 : 1) * 6"}
                                        :angle {:signal "datum.leftside ? datum.angle - 180 : datum.angle"}
                                        :align {:signal "datum.leftside ? 'right' : 'left'"}}}}]}]]))
