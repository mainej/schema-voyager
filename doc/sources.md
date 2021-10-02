## Where is your schema?

Your schema data is stored in many places.
A running Datomic database is the authority on what attributes and constants are installed, and what their core properties are.
To augment this, you're likely to maintain a file of [supplemental properties](/doc/annotation.md).
You can imagine other places where you might store data (see below for all the options) but the point is, you have many sources of schema data.

Schema Voyager calls each of these sources ...wait for it... a **"source"**.
A source is anything from which a vector of schema data can be extracted.
Given a "Datomic source", like this:

```clojure
(def datomic-source {:datomic/db-name "my-db-name",
                     :datomic/client-config {:server-type :dev-local,
                                             :system "my-system"}})
```

Schema Voyager extracts schema data by quering for idents:

```clojure
(schema-voyager.cli/extract-source datomic-source)
;; =>
[{:db/ident       :person/given-name
  :db/valueType   :db.type/string
  :db/cardinality :db.cardinality/one}
  ,,,]
```

Given a "file source":

```clojure
(def file-source {:file/name "path/to/supplemental-schema.edn"})
```

Schema Voyager extracts schema data by reading the file:

```clojure
(schema-voyager.cli/extract-source file-source)
;; => 
[{:db/ident              :person/given-name
  :db.schema/deprecated? true}
  ,,,]
```

Read on to learn about the sources Schema Voyager understands and how it merges them.

## How to use with `schema-voyager.cli`

The primary interface for working with sources is `schema-voyager.cli/standalone`.
You can read the [usage documentation](/doc/installation-and-usage.md) to learn how to invoke `schema-voyager.cli/standalone`, but for now, let's focus on what it does.

`schema-voyager.cli/standalone` is divided into two parts.
First it calls `schema-voyager.cli/ingest` which reads your schema data.
Then it turns that data into a web page.
This document focus on the first part, ingestion.

`schema.voyager.cli/ingest` accepts a vector of sources.
It extracts schema data from each source in turn, then merges all the data.

## Example of ingestion and merging

Let's follow the ingestion process.
The above example about `:person/given-name` included schema data from two sources, a Datomic source and a file source.
From the Datomic source we learned the `:db/valueType` and `:db/cardinality` of the attribute.
From the supplemental file source we also learned that this attribute has been `:db.schema/deprecated?`.
`schema.voyager.cli/ingest` merges these two sources of data into a single entity:

```clojure
[{;; From datomic-source
  :db/ident              :person/given-name
  :db/valueType          :db.type/string
  :db/cardinality        :db.cardinality/one
  ;; From file-source
  :db.schema/deprecated? true}]
```

`schema-voyager.cli/ingest` then processes this data to derive missing properties.
Since it wasn't set explicitly by prior sources, Schema Voyager derives that `:person/given-name` is part of the `:person` aggregate.

```clojure
[{:db/ident              :person/given-name
  :db/valueType          :db.type/string
  :db/cardinality        :db.cardinality/one
  :db.schema/deprecated? true
  ;; Derived
  :db.schema/part-of     [#schema/agg :person]}]
```

After merging, the resulting data is saved for later consumption by the web page which displays your schema.

Let's reiterate.
You can specify many sources.
Properties from later sources will be merged with those from earlier sources.
Thus, later sources can add or override properties defined in earlier sources by re-using a `:db/ident` instead of re-specifying the entire attribute.

```clojure
(schema-voyager.cli/ingest 
  {:sources [{:datomic/db-name       "my-db-name",
              :datomic/client-config {:server-type :dev-local, :system "my-system"}}
             {:file/name "path/to/supplemental-schema.edn"}]})
```

> Observe this example carefully.
> Most projects will use something like this, with a pair of sources, a Datomic source augmented by a supplemental file source.

## Types of sources

OK, so you should understand how sources are merged.
With that information, let's explore the sources from which Schema Voyager can extract schema data.
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

This extracts all the `:db/ident`s from the database.

> By default, it does not extract any supplemental schema, though it can.
> See the docs on [Datomic inference](/doc/datomic-inference.md) for details, being sure to heed the warnings.
> Generally, you should prefer "file sources" with supplemental schema over Datomic inference.

Schema Voyager makes a few assumptions about which attributes to extract.
If you want to exclude certain attributes (or include attributes that are excluded by default), provide `:datomic/exclusions`.
See `schema-voyager.ingest.datomic/excluded-attr?` for details.

Since not all projects will need to connect to Datomic, it is not one of the default dependencies.
This can lead to errors when ingesting from a Datomic source.
See the [troubleshooting docs](/doc/troubleshooting.md) for fixes.

### file source

A "file source" extracts (slurps) schema data (usually supplemental schema) from an EDN file.

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
  {:sources [{:fn/name 'my.ns/person-schema
              :fn/args {:fields [:person/name
                                 :person/contact-number]}}]})
```

The `:fn/name` will be resolved and called with a single argument, either the value provided in `:fn/args`, or an empty hashmap.

## Where should I store my schema and annotations?

If you're documenting an existing database, 99% of the time you'll combine a Datomic source with a file source.
The Datomic source will extract attributes.
The file source will supplement the attributes with references and deprecations.

If you're doing database design, before a database actually exists, you can probably get by with a single file source.

If you want the flexibility of defining your schema data with Clojure code, perhaps to organize attributes that all share some common properties, you may want to move your schema definition to a Clojure script which uses static and function sources.

A final note:
Technically it's possible to install supplemental properties in Datomic, then transact them into Datomic directly on new or existing attributes.
For example, after transacting this bit of schema metadata:

```clojure
[{:db/ident       :db.schema/deprecated?
  :db/valueType   :db.type/boolean
  :db/cardinality :db.cardinality/one
  :db/doc         "Whether this attribute or constant has fallen out of use. Often used with :db.schema/see-also, to point to a new way of storing some data."}]
```

You could record that an attribute is deprecated directly in Datomic:

```clojure
(d/transact conn {:tx-data [{:db/ident              :person/given-name
                             :db.schema/deprecated? true}]})
```

From experience, this is tempting but tends to fall out of date.
If someone forgets to add an annotation when an attribute is first installed, or if an annotation needs to be changed, it feels expensive to craft another migration which adds or retracts the right annotation data.
It's cheap, however, to update a file full of annotations.
If you're intent on trying this route and want to know what schema to install, see `schema-voyager.db/metaschema` and/or `resources/schema-voyager-schema/schema.edn`.

## Where to go from here

If you haven't learned how to [define supplemental properties](/doc/annotation.md), you may want to do that now.
Otherwise it's time to [install and start using](/doc/installation-and-usage.md) Schema Voyager.
