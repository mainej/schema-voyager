# Schema Voyager

Schema Voyager is a tool for exploring and documenting the schema of your [Datomic](https://www.datomic.com/) databases.

To get a feel for Schema Voyager, try exploring the [mbrainz schema](https://focused-kepler-9497ed.netlify.app).
Or see the [quick start guide](#quick-start) to preview _your_ schema in Schema Voyager.

## Overview

There are two steps to using Schema Voyager.
First, you load (a.k.a ["ingest"](#ingest)) your schema into Schema Voyager.
Second, you [explore](#explore) that data from a web page.

To enhance your exploration, give Schema Voyager more information about the history and usage of your database by [annotating](#annotate) your schema.

Afterward, you can [export](#export) Schema Voyager's data in various formats.

## Quick Start

There are a few command-line tools to help you quickly ingest and explore your schema.
These will give you a taste of Schema Voyager's capabilities, but be sure to read the detailed [usage](#usage) instructions to learn more.

### Ingest from Datomic (Quick Start)

If you have a running Datomic database:

```sh
clojure -A:ingest:datomic -m ingest.datomic my-db-name \
  '{:server-type :ion, :region "us-east-1", :system "my-system", :endpoint "http://entry.my-system.us-east-1.datomic.net:8182/", :proxy-port 8182}'
```

### Infer from Datomic (Quick Start)

Alternatively, you can ask Schema Voyager to also infer deprecated attributes and references between attributes by inspecting how they are used in a database.
```sh
clojure -A:ingest:datomic -m ingest.datomic my-db-name \
  '{:server-type :ion, :region "us-east-1", :system "my-system", :endpoint "http://entry.my-system.us-east-1.datomic.net:8182/", :proxy-port 8182} \
  --infer references --infer deprecations
```
> **WARNING** Inferences can be slow and expensive.
Avoid running them on a query group that is serving critical traffic.
Or, preferably, [avoid inference](#infer-from-datomic).

For more options related to inference, refer to the command-line help:
```sh
clojure -A:ingest:datomic -m ingest.datomic --help
```

### Ingest from files (Quick Start)

If instead your schema is in one or more files:

```sh
clojure -A:ingest -m ingest.files resources/db/schema-1.edn resources/db/schema-2.edn
```

### Explore data (Quick Start)

After ingesting schema, explore it from a web page that Schema Voyager generates.

```sh
yarn --prod run standalone
open target/standalone.html
```

Take a look around.
You should find details about all the attributes in your schema.
If you asked Schema Voyager to `--infer references`, you can also navigate between and see diagrams of the references within your database.

Where do you go from here?
The [usage](#usage) section explains other ways to ingest and explore.
The [annotation](#annotate) section explains how you can enrich Schema Voyager with references and many other details about your schema.
And the [export](#export) section explains how you can save and share your schema.

## Usage

Let's go into a bit more detail.

You will use Schema Voyager in two phases.
First you will load your schema into a DataScript DB and save that DB into a file.
This is called "ingestion".
Then you will generate an HTML document from which to view that data.
This is called "exploration".

### Ingest

One of Schema Voyager's goals is to be flexible about where it gets schema from, where it "ingests" from.

It can [ingest from a running Datomic database](#ingest-from-datomic), which is useful to see what your schema _is_.

But it can also be used as a tool to iterate on a new schema design, or as a way to discuss design options with a team member.
So, to see what your schema _could be_, you can [ingest static data](#ingest-static-data) that looks essentially like what you would pass to `d/transact`, as if you were installing schema.

And finally, you'll get the most value out of Schema Voyager if you [annotate](#annotate) your schema.
The most common way to do this is to save annotations in a .clj or .edn file.
Schema Voyager can [ingest directly from edn files](#ingest-from-files).
It includes a [data reader](#collections) to keep the files compact.

#### Ingestion Scripts

Though you may want to follow the [quick start](#quick-start) instructions at first, more mature projects will manage a script which specifies how to do the ingestion.
For an example script, see [dev/ingest/projects/mbrainz.clj](dev/ingest/projects/mbrainz.clj).

To start your own ingestion script file, first note that Schema Voyager is not (currently) designed to be used as a dependency in your code.
Instead, you should clone this repo and pull your schema into it.

After cloning the repo, create a file `my_project.clj` in Schema Voyager alongside [dev/ingest/projects/mbrainz.clj](dev/ingest/projects/mbrainz.clj)
(The `dev/ingest/projects/` directory is `.gitignore`'d, so you don't have to worry about accidentally commiting this file to Schema Voyager.)
This file should have a namespace:

```clojure
(ns ingest.projects.my-project)
```

This namespace will be a scratchpad for loading your schema.

It should follow these steps:

```clojure
(ns ingest.projects.my-project
  (:require [schema-voyager.export :as export]
            [schema-voyager.data :as data]
            [schema-voyager.ingest.core :as ingest]))

;; Steps 1. and 2. See below for details
(defn schema-data [] ,,,)

(defn -main []
  (->> (schema-data)
       ;; 3.
       data/process
       ;; 4.
       ingest/into-db
       ;; 5.
       export/save-db))
```

1. Ingest from all sources, not necessarily in this order:
    * [Use static txn data](#ingest-static-data).
    * [Ingest from a file](#ingest-from-files).
    * [Ingest from a Datomic DB](#ingest-from-datomic).
1. [Join multiple sources](#join-sources).
1. Process the joined data.
1. Load the processed data into a DataScript DB.
1. Save the DataScript DB to a location where the [exploration](#explore) tools can pick it up.

#### Run an ingestion script

As files' content changes or as new schema gets transacted, you will need to re-run the ingestion script.
You can do this from a REPL in your namespace. Or if the namespace has a `-main` function, you can re-run from a terminal:

```sh
clojure -A:ingest -m ingest.projects.my-project
```

#### Ingest static data

You can ingest from static data, which is useful for experimenting with new schema, or for adding [supplemental properties](#annotate):

```clojure
(defn schema-data []
  [{:db/ident       :file/uuid
    :db/type        :db.type/uuid
    :db/cardinality :db.cardinality/one
    :db/unique      :db.unique/identity
    :db/doc         "A system-assigned ID for the file."}])
```

#### Ingest from files

If you have files that contain attribute definitions or [supplemental properties](#annotate), ingest with `schema-voyager.ingest.file/ingest`.

```clojure
(ns ingest.projects.my-project
  (:require [schema-voyager.ingest.file :as ingest.file]))

(defn schema-data []
  (ingest.file/ingest "../path/to/schema.edn"))
```

#### Ingest from Datomic

Datomic supports introspection of its schema by storing attributes as entities.
Schema Voyager can query a running Datomic for these entities with `schema-voyager.ingest.datomic/ingest`.

```clojure
(ns ingest.projects.my-project
  (:require [schema-voyager.ingest.datomic :as ingest.datomic]))

(defn schema-data []
  (let [db (ingest.datomic/datomic-db {:server-type :ion
                                       :region      "us-east-1"
                                       :system      "my-system"
                                       :endpoint    "http://entry.my-system.us-east-1.datomic.net:8182/"
                                       :proxy-port  8182}
                                      "my-system-db")]
    (ingest.datomic/ingest db)))
```

> **NOTE**: If you require `schema-voyager.ingest.datomic`, you will need to have `datomic.client.api` on your classpath.
This project provides an alias `:datomic` which will pull in a version of `com.datomic/client-cloud`.

> ```sh
> clojure -A:ingest:datomic -m ingest.projects.my-project
> ```

> If the provided version of `datomic.client.api` isn't right for your project, consider using `-Sdeps` to get the appropriate version.

#### Infer from Datomic

You can also ask Schema Voyager to infer deprecations and inter-attribute references by inspecting how attributes are used.

```clojure
(defn schema-data []
  (data/join (ingest.datomic/ingest db)
             (ingest.datomic/infer-references db)
             (ingest.datomic/infer-deprecations db)))
```

See the documentation for `schema-voyager.ingest.datomic` for more inference options.

> **WARNING** These inference tools may help kick start your supplemental properties, but they are imperfect.
Inferences can be slow and expensive.
Avoid running them on a query group that is serving critical traffic.

> Because of this caveat, and because often you will need domain knowledge to identify missing references or to audit deprecations, it's preferable to avoid inference by maintaining [references](#dbschemareferences) and [deprecations](#dbschemadeprecated) in a file by hand.

#### Join sources

The properties of an attribute might be defined incrementally over [several sources](#ingest), partially in Datomic, partially in a file, and partially in static data.
When combining several sources Schema Voyager merges data that share a `:db/ident`, so later sources can augment or override earlier sources.

Join data from several sources with `schema-voyager.data/join`:

```clojure
(data/join (ingest.datomic/ingest db)
           (ingest.file/ingest "supplemental-properties.edn")
           experimental-schema)
```

Or from a sequence of sources with `schema-voyager.data/join-all`:


```clojure
(data/join-all (map ingest.file/ingest file-names))
```

### Explore

Now the fun part!

More adventurous technical users might enjoy exploring Schema Voyager's DataScript DB directly in a Clojure REPL.
(See the `comment` section of [`dev/ingest/projects/mbrainz.clj`](dev/ingest/projects/mbrainz.clj) for a sample.)
But most users will want something easier.
So, after ingesting, Schema Voyager can generate a web app from which to explore the schema.

#### Explore Standalone Web Page

To generate a standalone HTML document for the web app:

```sh
yarn --prod run standalone
```

Then open `target/standalone.html` in your browser.

The app starts with an overview of all the aggregates, enums and entity specs in your schema.

![homepage](doc/collections.png)

There is a diagram of references between collections.

![connections diagram](doc/collections_diagram.png)

Drill into an aggregate or enum to see the attributes or constants that it contains, as well as a more focused diagram of how it's connected to other collections.

![aggregate](doc/aggregate.png)

Drill further into an attribute or constant, to see its properties.

![attribute](doc/attribute.png)

#### Explore Live Web Page

If you wish to see updates as you change the DB file, you need a live version of the JS.

> This is a bit more involved.
I spend a lot of time in this mode while developing Schema Voyager.
And it might be useful when you're iterating on a new schema design.
But it's definitely a more advanced use-case.

First you need HTML and CSS files.

```sh
yarn run html # only once, or if assets/index.html has changed
yarn run watch-css
```

Then you need a running JS server.
You can start the server from from your terminal or editor.

From the terminal:

```sh
yarn run watch-js
```

From an Emacs ClojureScript REPL:

```emacs
M-x cider-jack-in-cljs
<choose shadow-cljs>
<choose shadow>
<choose :app>
```

Trigger a run of the [ingestion script](#ingestion-scripts), either from a CLJ REPL or from the command line.
After everything is loaded, open [http://localhost:8080](http://localhost:8080).
When the ingestion script is re-run, changes will be reflected on the page after a short delay.

## Annotate

The core data for Schema Voyager are the [properties of an attribute](https://docs.datomic.com/cloud/schema/defining-schema.html), like `:db/valueType` and `:db/cardinality`, that have been (or will be) transacted into a Datomic database.
Datomic needs these properties to run.
However, Datomic [recommends](https://docs.datomic.com/cloud/best.html#annotate-schema) that you annotate schema with information that it doesn't need to run, but which helps explain the history and structure of the database.

Schema Voyager introduces [supplemental properties](#supplemental-properties) for annotating attributes.

Though the annotation step is optional, it's an excellent way to enrich and document your schema.
Without any annotation, Schema Voyager will show the main properties of an attribute like the name, type, cardinality, uniqueness constraints and other properties.
But with annotation, it can show much more—whether an attribute has been deprecated, which entities it refers to, and more.

To learn all the ways to annotate your schema, it's useful to understand some Schema Voyager [terminology](#terminology) first.

### Terminology

Schema Voyager is interested in the `:db/ident`s in your schema.
It introduces terminology for different type of idents and their various parts.

```
           aggregate ─────────────────────────────┐
            ▲                                     │
            │                                     │
           ─┴─────                                ├──► collection
{:db/ident :medium/format :db/type :db.type/ref}  │
           ─┬────────────                         │
            │                                     │
            ▼                                     │
           attribute ─────────────────────────────┼──┐
                                                  │  │
           enum ──────────────────────────────────┘  │
            ▲                                        ├──► ident
            │                                        │
           ─┴─────                                   │
{:db/ident :medium.format/dvd}                       │
           ─┬────────────────                        │
            │                                        │
            ▼                                        │
           constant  ────────────────────────────────┘
```

#### idents

While processing the idents, Schema Voyager classifies them into one of three types: attributes, constants and entity specs.

First, there are [**attributes**](https://docs.datomic.com/cloud/schema/defining-schema.html), the main part of any schema.
Their defining characteristic is that they have a [`:db/valueType`](https://docs.datomic.com/cloud/schema/schema-reference.html#db-valuetype).
An example atribute is `:track/name`, which has the following schema:
```clojure
{:db/ident :track/name, :db/valueType :db.type/string ,,,}
```

Second, there are [**constants**](https://docs.datomic.com/cloud/best.html#idents-for-enumerated-types), members of an enumerated type.
They are standalone entities, with a `:db/ident` but no `:db/valueType`. An example is `:medium.format/dvd`, with the following schema:
```clojure
{:db/ident :medium.format/dvd}
```

Schema Voyager treats attributes and constants very similarly, and some of this documentation refers to them collectively as attributes.

Finally, there are [**entity specs**](https://docs.datomic.com/cloud/schema/schema-reference.html#entity-specs).
These are special entities that have a `:db/ident` as well as `:db.entity/attrs` or `:db.entity/preds`.
They are used to trigger entity-level validations within the transactor.
```clojure
{:db/ident :score/guard
 :db.entity/attrs [:score/low :score/high]
 :db.entity/preds 'datomic.samples.entity-preds/scores-are-ordered?}
 ```

#### collections

Schema Voyager groups collections of idents that share a namespace into what it calls **collections**.

> In Datomic an entity usually consists of several attributes that share a namespace.
For example, a track entity might contain the `:track/artist` and `:track/duration` attributes, among others.
In the majority of the database world, the namespace `:track` would be called a "table".
(The attributes would be called "columns" and the entities that use them "rows".)
However, Datomic itself does not use the word "table" nor does it introduce its own terminology for this concept.

There are two types of collections.

An **aggregate** is a collection of attributes, what the SQL world would call a "table".
For example, `:track` is the aggregate that contains `:track/artist` and `:track/duration`.

An **enum** is a collection of constants.
For example, `:medium.format` is the enum that contains `:medium.format/cd` and `:medium.format/dvd`.

Internally, when Schema Voyager needs to reference aggregates or enums they are represented as hash maps:
```clojure
{:db.schema.collection/type :aggregate
 :db.schema.collection/name :artist}
{:db.schema.collection/type :enum
 :db.schema.collection/name :medium.format}
```

This structure is a bit verbose, so within Clojure code, there are helpers to construct collections:
```clojure
(schema-voyager.data/aggregate :artist) ;; =>   #:db.schema/collection{:type :aggregate, :name :artist}
(schema-voyager.data/enum :medium.format) ;; => #:db.schema/collection{:type :enum,      :name :medium.format}
```

It's also common to reference collections from EDN files, so Schema Voyager provides tagged literals which can be read with `schema-voyager.data/read-string`.
Because supplemental properties are often stored in EDN files, `schema-voyager.ingest.file/ingest` uses `schema-voyager.data/read-string`.
In an EDN file:

```clojure
#schema/agg :artist         ;; => #:db.schema.collection{:type :aggregate, :name :artist}
#schema/enum :medium.format ;; => #:db.schema.collection{:type :enum,      :name :medium.format}
```

Collections can be described by adding `:db/doc` strings to them in the annotations. In this case, you may need the long-hand:

```clojure
{:db.schema.collection/type :aggregate
 :db.schema.collection/name :artist
 :db/doc                    "A person or group who contributed to a release or track."}
```


### Supplemental Properties

With this terminology in hand, it's time to learn how to annotate your schema.

To annotate you add supplemental properties, most of which are in the `:db.schema` namespace, directly to the attributes.
Because [sources can be joined](#join-sources) you can fetch the main properties from Datomic, but augment them with supplemental properties in a file or somewhere else.
Typically if an attribute is fetched from Datomic, annotations for it stored in a file need only the `:db/ident` of the attribute.
For example, to annotate that an attribute has been deprecated and replaced by another attribute:

```clojure
;; Was the person's first name, but since not every country follows the
;; given/family name pattern, has been replaced by :person/full-name. See
;; migration 5 which merged given+family into full name.
{:db/ident              :person/given-name
 :db.schema/deprecated? true
 :db.schema/see-others  [:person/full-name]}
```
It's up to you whether to transact this supplemental data, or leave it in an EDN or CLJ file.
From experience, this data stays more up-to-date if it's kept _out_ of Datomic.

For an example of supplemental properties, see [resources/mbrainz-supplemental.edn](resources/mbrainz-supplemental.edn).
That file augments the schema defined in [resources/mbrainz-schema.edn](resources/mbrainz-schema.edn) and [resources/mbrainz-enums.edn](resources/mbrainz-enums.edn).
It does not take advantage of every one of the Schema Voyager supplemental properties, but is a good introduction.

#### :db.schema/deprecated?

When an attribute has fallen out of use, mark it as such:

```clojure
{:db/ident              :track/artistCredit
 :db.schema/deprecated? true}
```

Schema Voyager will de-emphasize deprecated attributes in various parts of the UI.

#### :db.schema/references

For attributes that are `{:db/valueType :db.type/ref}`, it's possible to annotate which collections the attribute references.
Adding references is one of the best ways to enrich your schema, and will enable many features when exploring your data.
Schema Voyager uses references to link attributes to other collections, and to draw relationships in the diagrams.
To specify that `:address/country` refers to entities with `:country/name` and `:country/alpha-3`, supplement your schema thus:

```clojure
{:db/ident             :address/country
 :db.schema/references [(schema-voyager.data/aggregate :country)]}
```

To indicate that `:address/region` might refer to either a U.S. state like `:region.usa/new-york` or Canadian province like `:region.can/quebec`.

```clojure
{:db/ident             :address/region
 :db.schema/references [(schema-voyager.data/enum :region.usa)
                        (schema-voyager.data/enum :region.can)]}
```

#### :db.schema/tuple-references

One or more elements of a [heterogenous tuple](https://docs.datomic.com/cloud/schema/schema-reference.html#heterogeneous-tuples) may be a `:db.type/ref`.
To annotate the collections to which those refs refer, use `:db.schema/tuple-references`. For example if you had this attribute:

```clojure
{:db/ident       :post/ranked-comments
 :db/valueType   :db.type/tuple
 :db/tupleTypes  [:db.type/long :db.type/ref]
 :db/cardinality :db.cardinality/many
 :db/doc         "Pairs where the first element is a rank for a comment and the second element is a link to the comment itself. Used to sort the comments within a post."}
```

You might supplement it with this annotation:

```clojure
{:db/ident                   :post/ranked-comments
 :db.schema/tuple-references [{:db.schema.tuple/position 1
                               :db.schema/references     [(schema-voyager.data/aggregate :comment):]}]}
```

`:db.schema.tuple/position` is the position at which a ref appears in a tuple.
It is zero-indexed.

> **NOTE**: Use `:db.schema/references` to define the references of a [homogeneous tuple](https://docs.datomic.com/cloud/schema/schema-reference.html#homogeneous-tuples):

> ```clojure
> {:db/ident             :label/top-artists
>  :db/valueType         :db.type/tuple
>  :db/tupleType         :db.type/ref
>  :db/cardinality       :db.cardinality/one
>  :db/doc               "References to the top selling 0-5 artists signed to this label."
>  :db.schema/references [(schema-voyager.data/aggregate :artist)]}
> ```

#### :db.schema/part-of

Attributes and constants are part of one or more collections.
By default, Schema Voyager will derive the appropriate collection from the ident's namespace.
It will put both the attributes `:artist/name` and `:artist/startYear` in the `:artist` aggregate and the constant `:medium.format/dvd` in the `:medium.format` enum.
So, most of the time you won't need to specify `:db.schema/part-of`.

```clojure
{:db/ident          :artist/name
 :db/valueType      :db.type/string
 ;; UNNECESSARY, this is the default for an *attribute* named :artist/name
 :db.schema/part-of [(schema-voyager.data/aggregate :artist)]}

{:db/ident          :medium.format/dvd
 ;; UNNECESSARY, this is the default for a *constant* named :medium.format/dvd
 :db.schema/part-of [(schema-voyager.data/enum :medium.format)]}
```

However, there are exceptions.
The namespace of an attribute does not always match its usage.
So, if you need to, you can override the default collection.

For example, some attributes are used alongside attributes in a different namespace:

```clojure
;; :car.make/name appears directly on :car entities
{:db/ident          :car.make/name
 :db/valueType      :db.type/string
 :db.schema/part-of [(schema-voyager.data/aggregate :car)]}
```

Others are used on many different aggregates:

```clojure
;; :timestamp/updated-at appears on both posts and comments
{:db/ident          :timestamp/updated-at
 :db/valueType      :db.type/inst
 :db.schema/part-of [(schema-voyager.data/aggregate :post)
                     (schema-voyager.data/aggregate :comment)]}
```

#### :db.schema/see-also

It may help to understand an attribute by learning about one or more other attributes.


```clojure
{:db/ident           :track/artistCredit
 :db.schema/see-also [:track/artist]}
```

> **NOTE**: you can refer to collections without predefining them, but the same is not true of attribute references.
You may have to use tempids to create see-also references between attributes.


> ```clojure
> {:db/id    "attr--track-artists"
>  :db/ident :track/artists}
> {:db/ident           :track/artistCredit
>  :db.schema/see-also ["attr--track-artists"]}
> ```

## Export

So, you've ingested some carefully annotated schema and have been exploring it.
You'll want to share the insights you're having.
There are several ways to export data from Schema Voyager.

### Export DataScript

[Ingestion scripts](#ingestion-scripts) save the full DataScript DB in EDN format to `resources/schema_voyager_db.edn`.
You may share or commit this file elsewhere.

### Export Standalone Web Page

After saving the DataScript file, you can [generate a standalone HTML document](#explore-standalone-web-page), a web app to explore the data.
This file, which is named `target/standalone.html`, embeds the data from `resources/schema_voyager_db.edn` (via `shadow.resource/inline`).
Since it doesn't need to communicate with a server, it can be committed, emailed or otherwise shared anywhere.

### Export ERD Diagrams

Within the HTML there are diagrams of collections and their relationships.
These can be exported as SVG files.
Open the configuration menu in the upper left of any diagram.

### Host Web App

The HTML, CSS and compiled JS can be hosted on Netlify or a server of your choice.

You can host the standalone file by making `target/standalone.html` the `index.html` on your server.
Alternatively, you can serve seperate HTML, JS and CSS files.
For this option:

```sh
yarn --prod run clean
yarn --prod run html # generates index.html
yarn --prod run compile-css # generates a minified version of the CSS
yarn --prod run compile-js # generates a Closure-optimized version of the JS
```

Then copy `target/*` into the root directory of the server.

## Alternatives

* [Hodur](https://github.com/hodur-org/hodur-engine) provides excellent [visualizations](https://github.com/hodur-org/hodur-visualizer-schema) of Datomic schema, but also expects to have greater control over schema installation and management.

## Acknowlegements

* [DataScript](https://github.com/tonsky/datascript) makes it much easier to design and import deeply interconnected data without worrying about how those connections might later be explored.
  It's very useful to export an entire database from Clojure, then read and manipulate it from ClojureScript.
  Also, it feels right to use Datalog to navigate Datomic data.
* Though the HTML app isn't very dynamic, it was nice, as always, to build it with [reagent](https://reagent-project.github.io/) and [reitit](https://metosin.github.io/reitit/).
* [Tailwind CSS](https://tailwindcss.com/) makes writing CSS _fun_.
  Easy refactoring of styles, small CSS files, and the pages look nice, right?
* It's amazing what goes into running GraphViz on the web.
  Thanks to [Dorothy](https://github.com/cemerick/dorothy) for the tricky bits of DOT.
  And to [HPCC-Systems](https://github.com/hpcc-systems/hpcc-js-wasm) for compiling GraphViz for the browser.
  Judging from all the projects I tried but failed to get working, this is no small feat.
  And to [unpkg](https://unpkg.com/) for hosting the WASM.
* [shadow-cljs](http://shadow-cljs.org/) makes it a joy to write CLJS files alongside CLJ files.
  Several client/server interaction tasks I had been putting off turned out to be trivial with shadow-cljs.

## License

Copyright © Jacob Maine.
All rights reserved.
The use and distribution terms for this software are covered by the Eclipse Public License 2.0 (https://opensource.org/licenses/EPL-2.0) which can be found in the file LICENSE at the root of this distribution.
By using this software in any fashion, you are agreeing to be bound by the terms of this license.
You must not remove this notice, or any other, from this software.
