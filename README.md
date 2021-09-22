# Schema Voyager

Schema Voyager is a tool for documenting and exploring your [Datomic](https://www.datomic.com/) databases.
It imports your schema then displays it in a way that offers you and your team insights about the history and usage of your database.

The best way to get a feel for Schema Voyager is to play with it.
Try exploring the live [mbrainz schema](https://focused-kepler-9497ed.netlify.app).

If you'd prefer a few screenshots, here's what it looks like.
The app starts with an overview of your schema, with attributes grouped into related collections.

![Screenshot of the listing of aggregate and enum collections on the Schema Voyager homepage](/doc/collections.png)

A configurable interactive diagram shows references between collections.

![Screenshot of the connections diagram on the Schema Voyager homepage](/doc/collections_diagram.png)

Drill in to a collection to see the attributes it contains, as well as a more focused diagram of how those attributes are connected to other collections.

![Screenshot of the attributes list and the connections diagram on the Schema Voyager aggregate page](/doc/aggregate.png)


Drill further into an attribute, to see its properties.

![Screenshot of the attribute details on the Schema Voyager attribute page](/doc/attribute.png)

## Big Picture

Let's establish a mental model of how Schema Voyager works.

Presumably you can see where Schema Voyager gets some of its data.
When you transact schema, you give attributes a `:db/ident` along with their `:db/valueType` and `:db/cardinality`.
You can imagine how Schema Voyager would query a Datomic database for data about attributes by looking up all the `:db/ident`s.
It could group similar attributes together and display their data.

But there's more going on here.
Somehow Schema Voyager knows not only that `:track/artists` is a `:db.type/ref` attribute, but also that it refers to entities with attributes in the `:artist` namespace.
If you poked around in the [mbrainz schema](https://focused-kepler-9497ed.netlify.app) you might have noticed `:track/artistCredit` has been deprecated and superseded by `:track/artists`.
References and deprecations aren't part of the regular schema that Datomic defines, so how does Schema Voyager know about these things?

Enter **supplemental properties**.
Schema Voyager defines supplemental properties that can be assigned to an attribute, properties which specify what the attribute references and whether it's deprecated, among other characteristics.

> Typically you'll maintain these supplemental properties in a separate [file](/doc/sources.md#file-source) by hand (don't worry, it's not hard!) though see how to [infer](/doc/datomic-inference.md) supplemental properties from an unfamiliar running database.

It's these supplemental properties that turn Schema Voyager into a living document about how your schema is being used, how attributes reference entities, which attributes are deprecated, and more.


## Further Documentation

OK, sounds good.
How do we get started?

1. Start by learning how Schema Voyager ingests schema data from different [sources](/doc/sources.md).
2. Then learn about the [supplemental properties](/doc/annotation.md) that define references, deprecations, and other supplemental schema data you can add.
3. When you're ready, read the [installation and usage documentation](/doc/installation-and-usage.md) to learn how to convert your schema data into a web page.
4. Read the docs on [exploring and sharing](/doc/exploring-and-sharing.md) for details about exploring this page and sharing it with others.


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
