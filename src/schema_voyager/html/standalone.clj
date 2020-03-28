(ns schema-voyager.html.standalone
  (:require [shadow.cljs.devtools.api :as shadow]
            [clojure.java.shell :refer (sh)]
            [hiccup.page :as hiccup.page]))

(defn yarn [& args]
  (apply sh "yarn" "--prod" args))

(defn yarn-run [& args]
  (apply yarn "run" args))

(defn clean []
  (yarn-run "clean"))

(defn optimized-js []
  (shadow/release :app))

(defn optimized-css []
  (yarn-run "css"))

(defn standalone-html []
  (spit "target/standalone.html"
        ;; IMPORTANT: keep this in sync with index.html
        (hiccup.page/html5
         [:head
          [:meta {:charset "UTF-8"}]
          [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
          [:title "Schema Voyager"]
          [:style (slurp "target/styles.css")]]
         [:body
          [:div#app]
          [:script {:type "text/javascript"} (slurp "target/main.js")]])))

(defn standalone []
  (clean)
  (optimized-js)
  (optimized-css)
  (standalone-html))

(defn debug []
  (println "yarn env: " (:out (yarn "env")))
  (println "pwd: " (:out (sh "pwd"))))
