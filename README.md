# Schema Voyager

Schema Voyager is a tool for documenting and exploring your [Datomic](https://www.datomic.com/) databases.
It imports your schema then displays it in a way that offers you and your team insights about the history and usage of your database.

The best way to get a feel for Schema Voyager is to play with it.
Try exploring the live [mbrainz schema](https://mainej.github.io/schema-voyager/mbrainz-schema.html).

If you'd prefer a few screenshots, here's what it looks like.
The app starts with an overview of your schema, with attributes grouped into related collections.

<img src="/doc/collections.png" alt="Screenshot of the listing of aggregate and enum collections on the Schema Voyager homepage" width="450"/>

A configurable interactive diagram shows references between collections.

<img src="/doc/collections_diagram.png" alt="Screenshot of the connections diagram on the Schema Voyager homepage" width="1004"/>

Drill in to a collection to see the attributes it contains, as well as a more focused diagram of how those attributes are connected to other collections.

<img src="/doc/aggregate.png" alt="Screenshot of the attributes list and the connections diagram on the Schema Voyager aggregate page" width="637"/>

Drill further into an attribute, to see its properties.

<img src="/doc/attribute.png" alt="Screenshot of the attribute details on the Schema Voyager attribute page" width="638"/>

## Big Picture

Let's establish a mental model of how Schema Voyager works.

Presumably you can see where Schema Voyager gets some of its data.
When you transact schema into Datomic, you give attributes a `:db/ident` along with a `:db/valueType` and `:db/cardinality`.
You can imagine how Schema Voyager would query Datomic for this data by looking up all the `:db/ident`s.
It could group similar attributes together and display their data.

But there's more going on here.
Somehow Schema Voyager knows not only that `:track/artists` is a `:db.type/ref` attribute, but also that it refers to entities with attributes in the `:artist` namespace.
If you poked around in the [mbrainz schema](https://mainej.github.io/schema-voyager/mbrainz-schema.html) you might have noticed `:track/artistCredit` has been deprecated and superseded by `:track/artists`.
References and deprecations aren't part of the regular schema that Datomic defines, so how does Schema Voyager know about these things?

Enter **supplemental properties**.
Schema Voyager defines supplemental properties that can be assigned to an attribute, properties which specify what the attribute references and whether it's deprecated, among other characteristics.

It's these supplemental properties that turn Schema Voyager into a living document about how your schema is being used, how attributes reference entities, which attributes are deprecated, and more.


## Further Documentation

OK, sounds good.
How do we get started?

1. Start by learning [how to define](/doc/annotation.md) references, deprecations, and other supplemental properties.
2. Then learn about [where to store](/doc/sources.md) your schema data, and how Schema Voyager reads these sources.
3. When you're ready, learn [how to invoke](/doc/installation-and-usage.md) Schema Voyager to convert your schema data into an interactive web page.

After you're up and running, [explore and share](/doc/exploring-and-sharing.md) your web page.
Or [infer](/doc/datomic-inference.md) references and deprecations, kickstarting your supplemental schema.


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
  And to [jsdelivr](https://cdn.jsdelivr.net/) for hosting the WASM.
* [shadow-cljs](http://shadow-cljs.org/) makes it a joy to write CLJS files alongside CLJ files.
  Several client/server interaction tasks I had been putting off turned out to be trivial with shadow-cljs.

## License

Copyright © Jacob Maine.
All rights reserved.

The use and distribution terms for this software are covered by the Eclipse Public License 2.0 (https://opensource.org/licenses/EPL-2.0) which can be found in the file LICENSE at the root of this distribution.
By using this software in any fashion, you are agreeing to be bound by the terms of this license.
You must not remove this notice, or any other, from this software.
