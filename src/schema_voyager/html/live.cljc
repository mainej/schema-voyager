(ns schema-voyager.html.live)

(defn main! []
  (println "[main]: loading"))

(defn reload! []
  (println "[main] reloaded lib:" #_lib/c #_lib/d)
  (println "[main] reloaded:" #_a #_b))
