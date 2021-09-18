## Use on the command line

The most common way to use Schema Voyager is from the command line.
Do a `git checkout` of this repository.
Then ingest your schema:

```sh
clojure -A:datomic -X:cli ingest :sources \
  '[{:datomic/db-name "my-db-name",
     :datomic/client-config {:server-type :dev-local, :system "my-system"}}
    {:file/name "resources/my-supplemental-schema.edn"}]'
```

See the [sources documentation](doc/sources.md) for details on specifying sources.

See the [exploration documentation](doc/exploring-and-sharing.md) for how to convert the ingested data into a running web page.

NOTE: If you don't need to use a Datomic source, omit the `:datomic` alias:

```sh
clojure -X:cli ingest :sources \
  '[{:file/name "resources/main-schema.edn"}
    {:file/name "resources/supplemental-schema.edn"}]'
```

## As an alias

To more easily share these commands, create an alias that invokes `schema-voyager.cli/ingest` directly.

```clojure
{,,,
 :aliases {:schema {:exec-fn   schema-voyager.cli/ingest
                    :exec-args {:sources [{:file/name "resources/schema.edn"}
                                          {:file/name "resources/supplemental.edn"}]}}}}
```

> If you include a [Datomic source](doc/sources.md#Datomic-source), you'll need to add deps on `com.datomic/client-cloud` and/or `com.datomic/dev-local`.

Re-run the ingestion like so:

```sh
clojure -X:schema
```

Unfortunately, at the moment, you have to place the alias in _Schema Voyager's_ deps.edn, not your project's.
This means maintaining a fork of schema-voyager, which isn't ideal.

TODO: this doesn't work as a project alias yet because `schema-voyager.cli/ingest` puts the Datascript DB in the project's resources directory.
But the standalone HTML script expects it to be in schema-voyager's resources directory. 
We need another script that can accept a Datascript DB file from anywhere.
Perhaps there's a way to do string substitution on the JS (maybe clojure.tools.build/copy-files) to inject the Datascript DB.
 
## As a tool

EXPERIMENTAL: you can install Schema Voyager as a Clojure Tool.
Do a `git checkout` of this repository.

```sh
clojure -Ttools install com.github.mainej/schema-voyager '{:local/root \".\"}' :as schema-voyager
clojure -Tschema-voyager ingest :sources '[<SOURCE>, ...]'
```

TODO: replace checkout and :local/root with a github or clojars install.

TODO: deal with the same problems as using schema-voyager as an alias.

## As a script

You can create a script which calls `schema-voyager.cli/ingest` directly.

```clojure
(ns my.ns
  (:require [schema-voyager.cli :as cli]))
  
(def schema
  [{:db/ident :track/name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}])
  
(defn -main [_]
  (cli/ingest {:sources [{:static/data schema}]}))
```

This example demonstrates using a static source, but any [type of source](doc/sources.md) is available.

TODO: deal with the same problems as using schema-voyager as an alias.
