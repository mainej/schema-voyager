The primary way of using Schema Voyager is through `schema-voyager.cli/standalone`.
You tell this function where your sources of schema data are, it pulls in those sources, and creates a standalone web page where you can explore your schema.
See the [sources documentation](/doc/sources.md) for details on specifying sources.
See the [exploration documentation](/doc/exploring-and-sharing.md) for how to use the generated web page.

## As an alias

In most cases you will be working on a team and several developers will need to be able to re-generate the standalone web page.
The easiest way to share the command to do this is to create an alias that invokes `schema-voyager.cli/standalone`.

```clojure
;; deps.edn
{,,,
 :aliases {:schema {:replace-deps {io.github.mainej/schema-voyager {:git/tag "v0.1.171", :git/sha "65469634"}}
                    :ns-default   schema-voyager.cli
                    ;; This example demonstrates using a few file sources, but any type of source is available.
                    :exec-args    {:sources [{:file/name "resources/main-schema.edn"}
                                             {:file/name "resources/supplemental-schema.edn"}]}}}}
```

Execute the alias like so:

```sh
clojure -X:schema standalone
```

This will generate a file called `schema-voyager.html` in the current directory, by default.
You can modify the location of the file by setting `:output-path`:

```clojure
;; deps.edn
{,,,
 :aliases {:schema {:replace-deps {io.github.mainej/schema-voyager {:git/tag "v0.1.171", :git/sha "65469634"}}
                    :ns-default   schema-voyager.cli
                    :exec-args    {:sources     [{:file/name "resources/main-schema.edn"}
                                                 {:file/name "resources/supplemental-schema.edn"}]
                                   ;; save to target/schema.html instead
                                   :output-path "target/schema.html"}}}}
```

## As an alias, with Datomic

If you need to include a [Datomic source](/doc/sources.md#Datomic-source), as many people do, you must depend on `com.datomic/client-cloud` and/or `com.datomic/dev-local`.

```clojure
;; deps.edn
{,,,
 :aliases {:schema {:replace-deps {io.github.mainej/schema-voyager {:git/tag "v0.1.171", :git/sha "65469634"}
                                   com.datomic/client-cloud        {:mvn/version "0.8.113"}
                                   com.datomic/dev-local           {:mvn/version "0.9.235"}}
                    :ns-default   schema-voyager.cli
                    :exec-args    {:sources [{:datomic/db-name "my-db-name",
                                              :datomic/client-config {:server-type :dev-local, :system "my-system"}}
                                             {:file/name "resources/my-supplemental-schema.edn"}]}}}}
```

## Different sources

If you want to quickly test a different set of sources, you can provide them at the command line:

```sh
clojure -X:schema standalone \
  :sources '[{:file/name "resources/main-schema.edn"}
             {:file/name "resources/supplemental-schema.edn"}]'
```

## As a tool

EXPERIMENTAL: you can install Schema Voyager as a Clojure Tool.

* Find tool versions: `clj -X:deps find-versions :lib io.github.mainej/schema-voyager`
* Install tool with `clj -Ttools install io.github.mainej/schema-voyager '{:git/tag "VERSION"}' :as schema-voyager`
* Invoke tool with `clj -Tschema-voyager standalone :sources '[<SOURCE>, ...]'`

## As a script

You can create a script which calls `schema-voyager.cli/standalone` directly.

```clojure
(ns my.ns
  (:require [schema-voyager.cli :as cli]))

(def schema
  [{:db/ident :track/name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}])

(defn -main [_]
  (cli/standalone {:sources [{:static/data schema}]}))
```

This example demonstrates using a static source, but any [type of source](/doc/sources.md) is available.

What's next?
[Explore](/doc/exploring-and-sharing.md) your schema.
