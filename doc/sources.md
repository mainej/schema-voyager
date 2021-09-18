## What kind of data is Schema Voyager interested in?

The core data for Schema Voyager are the [properties of an attribute](https://docs.datomic.com/cloud/schema/defining-schema.html), like `:db/valueType` and `:db/cardinality`.
These are the properties Datomic needs to run.
Schema Voyager is also interested in supplemental properties, such as deprecations and references.
For a deep dive into which supplemental properties are available and what they do, see the [annotation documentation](doc/annotations.md).

## Where is your data?

Your schema data is stored in many places.
A running Datomic database is the authority on what attributes and constants are installed, and what their core properties are.
To augment this, you will maintain a file of supplemental properties.
You can imagine other places where you might store data (see below for more options) but the point is, you have many sources of schema data.

Schema Voyager calls each of these sources ...wait for it... a "source".
A source is anything from which a vector of schema data can be extracted.
You might expect to extract basic properties from a "Datomic source":

```clojure
[{:db/ident       :person/given-name
  :db/valueType   :db.type/string
  :db/cardinality :db.cardinality/one}]
```

And extract supplemental properites from a "file source":

```clojure
[{:db/ident              :person/given-name
  :db.schema/deprecated? true}]
```

Read on to learn about the sources Schema Voyager understands and how to merge them.

## How to use with `schema-voyager.cli`

The primary interface for working with sources is `schema-voyager.cli/ingest`.
You can read the [installation documentation](doc/installation.md) to learn different ways to invoke `schema-voyager.cli/ingest`, but for now, let's focus on what it does.

`schema.voyager.cli/ingest` accepts a vector of sources.
(See below for how to specify each type of source.)
It extracts schema data from each source in turn, then merges all the data.

As an example, the above schema about `:person/given-name` came from two sources, a Datomic source and a file source.
`schema.voyager.cli/ingest` would merge these two sources of data into a single entity:

```clojure
[{;; From Datomic
  :db/ident              :person/given-name
  :db/valueType          :db.type/string
  :db/cardinality        :db.cardinality/one
  ;; From file
  :db.schema/deprecated? true}]
```

`schema-voyager.cli/ingest` then processes this data to derive whatever missing properties it can:

```clojure
[{:db/ident              :person/given-name
  :db/valueType          :db.type/string
  :db/cardinality        :db.cardinality/one
  :db.schema/deprecated? true
  ;; Derived
  :db.schema/part-of     [#schema/agg :person]}]
```

Finally, this data is saved for later consumption by the web page which displays your schema.

## Types of sources

Schema Voyager knows how to ingest four types of sources.

### Datomic source

A "Datomic source" extracts schema data from a running Datomic database.

You would extract from a Datomic source like this:
```clojure
(schema-voyager.cli/ingest
  {:sources [{:datomic/db-name "my-db-name",
              :datomic/client-config {:server-type :dev-local,
                                      :system "my-system"}}]})
```

This pulls and classifies all the `:db/ident`s from the database.
By default, it does not extract any supplemental schema, though it can.
See the docs on [Datomic inference](doc/datomic-inference.md) for details, being sure to heed the warnings.
Generally, you should prefer "file sources" with supplemental schema over Datomic inference.

Since not all projects that use Schema Voyager will need to connect to Datomic, it is not one of the default dependencies.
This can lead to errors when ingesting from a Datomic source.
See the [troubleshooting docs](doc/troubleshooting.md) for how to fix.

### file source

A "file source" reads schema data (usually supplemental schema) from an EDN file.

```clojure
(schema-voyager.cli/ingest 
  {:sources [{:file/name "path/to/supplemental-schema.edn"}]})
```

### static source

A "static source" is just some literal schema data.

```clojure
(schema-voyager.cli/ingest 
  {:sources [{:static/data [{:db/ident       :proposed.ns/proposed.field
                             :db/valueType   :db.type/long
                             :db/cardinality :db.cardinality/one}]}]})
```
  

### function source

A "function source" specifies a function which returns schema data.

```clojure
(ns my.ns)

(defn person-schema [{:keys [fields]}]
  (map (fn [field]
         {:db/ident       field
          :db/valueType   :db.type/string
          :db/cardinality :db.cardinality/one})
       fields))

;; elsewhere

(schema-voyager.cli/ingest 
  {:sources [{:fn/name my.ns/person-schema
              :fn/args {:fields [:person/name
                                 :person/contact-number]}}]})
```

The `:fn/name` will be resolved and called with a single argument, either the value provided in `:fn/args`, or an empty hashmap.

## merging sources

You can specify many sources.
Properties from later sources will be merged with those from earlier sources.
Thus, later sources can add or override properties defined in earlier sources by re-using a `:db/ident` instead of re-specifying the entire attribute.

Observe this example carefully.
Most projects will use something like this, with a pair of sources, a Datomic source augmented by a supplemental file source.

```clojure
(schema-voyager.cli/ingest 
  {:sources [{:datomic/db-name       "my-db-name",
              :datomic/client-config {:server-type :dev-local, :system "my-system"}}
             {:file/name "path/to/supplemental-schema.edn"}]})
```

## Where should I store my schema and annotations?

If you're documenting an existing database, 99% of the time you'll combine a Datomic source with a file source.
The Datomic source will extract attributes, but not do any inference of supplemental properties.
The file source will supplement the attributes with references and deprecations.

If you're doing database design, before a database actually exists, you can probably get by with a single file source.

The static and fn sources will rarely be needed, unless schema-voyager is being used as part of a larger script.

Technically it's possible to install supplemental properties in Datomic, then transact them into Datomic directly on new or existing attributes.
For example, after transacting this:

```clojure
[{:db/ident       :db.schema/deprecated?
  :db/valueType   :db.type/boolean
  :db/cardinality :db.cardinality/one
  :db/doc         "Whether this attribute or constant has fallen out of use. Often used with :db.schema/see-also, to point to a new way of storing some data."}]
```

You could transact this directly into Datomic:

```clojure
[{:db/ident              :person/given-name
  :db.schema/deprecated? true}]
```

From experience, this is tempting but tends to fall out of date.
If someone forgets to add an annotation when an attribute is first installed, or if an annotation needs to be changed, it feels expensive to craft another migration which adds or retracts the right annotation data.
It's cheap, however, to update a file full of annotations.
If you're intent on trying this route, see `schema-voyager.data/metaschema` for the schema you would need to install.
