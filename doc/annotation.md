# Annotate

The core data for Schema Voyager are the [properties of an attribute](https://docs.datomic.com/cloud/schema/defining-schema.html), like `:db/valueType` and `:db/cardinality`, that have been (or will be) transacted into a Datomic database.
Datomic needs these properties to run.
However, Datomic [recommends](https://docs.datomic.com/cloud/best.html#annotate-schema) that you annotate schema with information that it doesn't need to run, but which helps explain the history and structure of the database.

Schema Voyager introduces [supplemental properties](#supplemental-properties) for annotating attributes.

Though the annotation step is optional, it's an excellent way to enrich and document your schema.
Without any annotation, Schema Voyager will show the details of an attribute—the name, type, cardinality, uniqueness constraints and other properties.
But with annotation, it can show much more—whether an attribute has been deprecated, which entities it refers to, and more.

To learn all the ways to annotate your schema, it's useful to understand some Schema Voyager [terminology](#terminology) first.

## Terminology

Schema Voyager is interested in the `:db/ident`s in your schema.
It introduces terminology for different type of idents and their various parts.

Here's a pictorial summary.
Details are below.

``` clojure
;          aggregate ───────────────────────────────────┐
;           ▲                                           │
;           │                                           │
;          ─┴─────                                      ├──► collection
{:db/ident :medium/format, :db/valueType :db.type/ref}; │
;          ─┬────────────                               │
;           │                                           │
;           ▼                                           │
;          attribute ───────────────────────────────────┼──┐
;                                                       │  │
;          enum ────────────────────────────────────────┘  │
;           ▲                                              ├──► ident
;           │                                              │
;          ─┴─────                                         │
{:db/ident :medium.format/dvd};                            │
;          ─┬────────────────                              │
;           │                                              │
;           ▼                                              │
;          constant  ──────────────────────────────────────┘
```

### idents

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
{:db/ident        :score/guard
 :db.entity/attrs [:score/low :score/high]
 :db.entity/preds 'datomic.samples.entity-preds/scores-are-ordered?}
 ```

### collections

Schema Voyager groups collections of idents that share a namespace into what it calls **collections**.

> In Datomic an entity usually consists of several attributes that share a namespace.
For example, a track entity might contain the `:track/artist` and `:track/duration` attributes, among others.
In the majority of the database world, the namespace `:track` would be called a "table".
However, Datomic itself does not use the word "table" nor does it introduce its own terminology for this concept.
(To extend the analogy, attributes would be called "columns" and entities "rows". But, we digress.)

There are two types of collections.

An **aggregate** is a collection of attributes, what the SQL world would call a "table".
For example, `:track` is the aggregate that contains the attributes `:track/artist` and `:track/duration`.

An **enum** is a collection of constants.
For example, `:medium.format` is the enum that contains the constants `:medium.format/cd` and `:medium.format/dvd`.

When referencing aggregates and enums, generally you'll write them like this:
```clojure
#schema/agg :artist
#schema/enum :medium.format
```

Internally, Schema Voyager installs data-readers which expand these tagged literals:
```clojure
#schema/agg :artist         ;; => #:db.schema.collection{:type :aggregate, :name :artist}
#schema/enum :medium.format ;; => #:db.schema.collection{:type :enum,      :name :medium.format}
```

Occasionally you'll need to add further information about a collection.
For example, collections can be described by annotating them with a `:db/doc`. 
In these cases, you can use the longer form:

```clojure
;; schema.edn
{:db.schema.collection/type :aggregate
 :db.schema.collection/name :artist
 :db/doc                    "A person or group who contributed to a release or track."}
 
;; schema.clj
(assoc #schema/agg :artist :db/doc "A person or group who contributed to a release or track.")
```


## Supplemental Properties

With this terminology in hand, it's time to learn how to annotate your schema.

To annotate you add supplemental properties, most of which are in the `:db.schema` namespace, directly to attributes.

For example, suppose you've installed the following schema for people's names.
```clojure
[{:db/ident       :person/given-name
  :db/valueType   :db.type/string
  :db/cardinality :db.cardinality/one}
 {:db/ident       :person/family-name
  :db/valueType   :db.type/string
  :db/cardinality :db.cardinality/one}
 {:db/ident       :person/full-name
  :db/valueType   :db.type/string
  :db/cardinality :db.cardinality/one}]
```

To annotate that an attribute has been deprecated and replaced by another attribute, you might add the following supplemental annotation:

```clojure
[;; Since not every country follows the given/family name pattern,
 ;; :person/given-name and :person/family-name have been replaced by
 ;; :person/full-name. See migration 5 which merged given+family into full name.
 {:db/ident              :person/given-name
  :db.schema/deprecated? true
  :db.schema/see-also    [{:db/ident :person/full-name}]}
 {:db/ident              :person/family-name
  :db.schema/deprecated? true
  :db.schema/see-also    [{:db/ident :person/full-name}]}]
```

For an example of supplemental properties, see [resources/mbrainz-supplemental.edn](resources/mbrainz-supplemental.edn).
That file augments the schema defined in [resources/mbrainz-schema.edn](resources/mbrainz-schema.edn) and [resources/mbrainz-enums.edn](resources/mbrainz-enums.edn).
It does not take advantage of every one of the Schema Voyager supplemental properties, but is a good introduction.

### :db.schema/references

For attributes that are `{:db/valueType :db.type/ref}`, annotate which collections the attribute references with `:db.schema/references`.
Adding references is one of the best ways to enrich your schema, and will enable many features when exploring your data.
Schema Voyager uses references to link attributes to other collections, and to draw relationships in the diagrams.

Suppose you've installed the following street address schema:
```clojure
[{:db/ident       :address/country
  :db/valueType   :db.type/ref
  :db/cardinality :db.cardinality/one
  :db/doc         "A reference to the country in which this address is found."}
 {:db/ident       :address/region
  :db/valueType   :db.type/ref
  :db/cardinality :db.cardinality/one
  :db/doc         "A reference to the geographic region in which this address is found. The region should be in the :address/country (not enforced)."}
 {:db/ident       :country/name
  :db/valueType   :db.type/string
  :db/cardinality :db.cardinality/one
  :db/doc         "The display name of a country, like 'United States of America."}
 {:db/ident       :country/alpha-3
  :db/valueType   :db.type/string
  :db/cardinality :db.cardinality/one
  :db/doc         "The 3-character code for the country, like USA."}
  {:db/ident :region.usa/new-york
   :db/doc   "A constant representing the state of New York, in the USA."}
  {:db/ident :region.usa/california
   :db/doc   "A constant representing the state of California, in the USA."}
  {:db/ident :region.can/quebec
   :db/doc   "A constant representing the province of Quebec, in Canada."}]
```
This defines two aggregates, `:address`, and `:country`, and two enums `:region.usa` and `:region.can`.

To specify that `:address/country` refers to a country and `:address/region` might refer to either a U.S. state or Canadian province, supplement your schema thus:

```clojure
[{:db/ident             :address/country
  :db.schema/references [#schema/agg :country]}
 {:db/ident             :address/region
  :db.schema/references [#schema/enum :region.usa
                         #schema/enum :region.can]}]
```

### :db.schema/tuple-references

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
                               :db.schema/references     [#schema/agg :comment]}]}
```

`:db.schema.tuple/position` is the position at which a ref appears in a tuple.
It is zero-indexed.

[Homogeneous tuples](https://docs.datomic.com/cloud/schema/schema-reference.html#homogeneous-tuples) are easier.
They're annotated the same as regular `:db.type/ref` attributes:

```clojure
;; schema.edn
[{:db/ident             :label/top-artists
  :db/valueType         :db.type/tuple
  :db/tupleType         :db.type/ref
  :db/cardinality       :db.cardinality/one
  :db/doc               "References to the top selling 0-5 artists signed to this label."}]
;; annotation.edn
[{:db/ident             :label/top-artists
  :db.schema/references [#schema/agg :artist]}]
```

### :db.schema/part-of

Attributes and constants are part of one or more collections.
By default, Schema Voyager will derive the appropriate collection from the ident's namespace.
It will put both the attributes `:artist/name` and `:artist/startYear` in the `:artist` aggregate and the constant `:medium.format/dvd` in the `:medium.format` enum.
So, most of the time you won't need to specify `:db.schema/part-of`.

```clojure
{:db/ident          :artist/name
 :db/valueType      :db.type/string
 ;; UNNECESSARY, this is the default for an *attribute* named :artist/name
 :db.schema/part-of [#schema/agg :artist]}

{:db/ident          :medium.format/dvd
 ;; UNNECESSARY, this is the default for a *constant* named :medium.format/dvd
 :db.schema/part-of [#schema/enum :medium.format]}
```

However, there are exceptions.
The namespace of an attribute does not always match its usage.
So, if you need to, you can override the default collection.

For example, some attributes are used alongside attributes in a different namespace:

```clojure
;; :car.make/name appears directly on :car entities
{:db/ident          :car.make/name
 :db/valueType      :db.type/string
 :db.schema/part-of [#schema/agg :car]}
```

Others are used on many different aggregates:

```clojure
;; :timestamp/updated-at appears on both posts and comments
{:db/ident          :timestamp/updated-at
 :db/valueType      :db.type/inst
 :db.schema/part-of [#schema/agg :post
                     #schema/agg :comment]}
```

### :db.schema/deprecated?

When an attribute has fallen out of use, annotate it with the `:db.schema/deprecated?` supplemental property:

```clojure
{:db/ident              :track/artistCredit
 :db.schema/deprecated? true}
```

Schema Voyager will de-emphasize deprecated attributes in various parts of the HTML UI.

### :db.schema/see-also

It may help to understand an attribute by learning about one or more other attributes.
For instance, you may want to point to an attribute that supersedes a deprecated attribute.

```clojure
{:db/ident              :track/artistCredit
 :db.schema/deprecated? true
 :db.schema/see-also    [{:db/ident :track/artist}]}
```
