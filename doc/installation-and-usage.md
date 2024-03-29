# Installation

The primary way of using Schema Voyager is through `schema-voyager.cli/standalone`.
You tell this function where your [sources](/doc/sources.md) of [schema data](/doc/annotation.md) are.
It pulls in those sources and creates a standalone web page where you can explore your schema.

> In the docs below, replace VERSION with the latest release coordinates provided by Clojars: [![Clojars Project](https://img.shields.io/clojars/v/com.github.mainej/schema-voyager.svg)](https://clojars.org/com.github.mainej/schema-voyager)

## As an alias

In many cases you'll be working on a team.
Several developers will each need to re-generate the standalone web page.
The easiest way to share the command to do this is to create a `deps.edn` alias that invokes `schema-voyager.cli`.

```clojure
;; deps.edn
{,,,
 :aliases {:schema {:replace-deps {com.github.mainej/schema-voyager {:mvn/version "VERSION"}}
                    :ns-default   schema-voyager.cli
                    ;; This example demonstrates using a few file sources, but any type of source is available.
                    :exec-args    {:sources [{:file/name "resources/main-schema.edn"}
                                             {:file/name "resources/supplemental-schema.edn"}]}}}}
```

Execute the alias like so:

```sh
clojure -X:schema standalone
```

This will generate a file called `schema-voyager.html`, a standalone web page you can use to [explore](/doc/exploring-and-sharing.md) your schema.

## To a different file

The HTML file is placed in the current directory, by default.
You can modify the location by passing `:output-path` to `schema-voyager.cli/standalone`:

```clojure
;; deps.edn
{,,,
 :aliases {:schema {:replace-deps {com.github.mainej/schema-voyager {:mvn/version "VERSION"}}
                    :ns-default   schema-voyager.cli
                    :exec-args    {:sources     [{:file/name "resources/main-schema.edn"}
                                                 {:file/name "resources/supplemental-schema.edn"}]
                                   ;; save to target/schema.html instead
                                   :output-path "target/schema.html"}}}}
```

## As an alias, with Datomic

If you need to include a [Datomic source](/doc/sources.md#datomic-source), as many people do, you must depend on `com.datomic/client-cloud` and/or `com.datomic/dev-local`.

```clojure
;; deps.edn
{,,,
 :aliases {:schema {:replace-deps {com.github.mainej/schema-voyager {:mvn/version "VERSION"}
                                   com.datomic/client-cloud         {:mvn/version "0.8.113"}
                                   com.datomic/dev-local            {:mvn/version "0.9.235"}}
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

Similarly, you can override the location of the generated file with `:output-path '"another/path/schema.html"'`

## As a script

Alternatively, scripts can call `schema-voyager.cli/standalone` directly.

```clojure
(ns my.ns
  (:require [schema-voyager.cli :as cli]))

(def schema
  [{:db/ident :track/name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}])

(defn -main []
  (cli/standalone {:sources [{:static/data schema}]}))
```

This example demonstrates using a static source, but any [type of source](/doc/sources.md) is available.

## Where to go from here

It's time! [Explore](/doc/exploring-and-sharing.md) your schema.
