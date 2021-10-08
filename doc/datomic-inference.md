# Infer

Here's an idea.
What if you queried Datomic for unused attributes? 
Coudn't you infer which attributes were `:db.schema/deprecated?` instead of maintaining a list of them by hand?
And the same for `:db.schema/references`...
Couldn't you just look at the entities to which each `:db.type/ref` refers?

Well... yes, you could.
And Schema Voyager provides tools to do this.
But, there's a catch.

## Caveats

Inference queries are very large.
They might have to look at every entity in your database.
On large databases, they may swamp a query group.

> NOTE: if your database has many heterogeneous tuples (with `:db/tupleTypes`) be especially careful.
Inferring references in this case will lead to many large queries instead of just a few.

## Resolving the dilemma

So, inference sounds appealing, but perilous.
How should we proceed?

Well, first of all, we should be kind to our production servers.
If you want to use the inference tools, run them on a separate query group, one that doesn't serve production traffic.
Or better yet, don't run them on a production database at all.
Use a test database, or even better, a [`:dev-local`](https://docs.datomic.com/cloud/dev-local.html) database.
(That's what all the sample code in this documentation does.)

Second, you probably shouldn't run inference more than a few times.
Instead, print out the inferences that Schema Voyager makes (see below for how), move them to a file, and maintain that file by hand.
That is, use the inference tools to kick-start your supplemental schema EDN file, but don't rely on it for the long term.

Keep in mind that whichever database you use, inference is only as good as the data it can query.
If your `:dev-local` database isn't using an attribute that _is_ used in production, the inference tools may falsly report that the attribute is deprecated.

## Usage

### print-inferences

So, you've decided to use the inference tools, and you promise to do it responsibly (Schema Voyager and its maintainers are not responsible for any damage caused by using these tools).

There's a command line tool for printing inferences.
Use its output as the start of your supplemental schema file.
First ensure you have an [alias](/doc/installation-and-usage.md#As-an-alias) for Schema Voyager.

```sh
clojure -X:schema print-inferences \
  '{:datomic/db-name "my-db",
    :datomic/client-config {:server-type :dev-local, :system "my-system"}, 
    :datomic/infer #{:all}}'
```

The argument is a [Datomic source](/doc/sources.md#Datomic-source), with an additional key `:datomic/infer`.

Below are valid values for `:datomic/infer`.
You can include more than one:

* **`:all`** - make all inferences
  * **`:deprecations`** - infer deprecations based on attributes that are defined but not currently used
  * **`:references`** - infer references for attributes that are `:db.type/ref` or `:db.type/tuple`, based on the entities to which they refer
    * **`:plain-references`** - infer references for attributes that are `:db.type/ref`
    * **`:tuple-references`** - infer references for attributes that are `:db.type/tuple`
      * **`:homogeneous-tuple-references`** - infer references for attributes that have `:db/tupleType`
      * **`:heterogeneous-tuple-references`** - infer references for attributes that have `:db/tupleTypes`
  
### standalone

You can also use `:datomic/infer` with [`schema-voyager.cli/standalone`](/doc/installation-and-usage.md).
However, this it isn't recommended, because `standalone` is designed to run whenever your schema updates.
If it is ever run against a production database with inference enabled, you may experience the problems described above.

## Future work

Inference hasn't been tested on large Datomic databases.
I've never been brave enough!
If you have experience reports, positive or negative, I'd be curious to hear.

It may be possible to make inference faster (by sampling, limiting, etc.), but this work hasn't been done.
