# Advanced exploration

You can use Schema Voyager as a live tool, which responds to updates to the
schema ingestion process as soon as they occur.

> This is a bit more involved.
I spend a lot of time in this mode while developing Schema Voyager and it might be useful when you're iterating on a new schema design, but it's definitely a more advanced use-case.

## Pre-requisites

To work in this mode, you have to be in Schema Voyager's directory, not your project's directory.
First do a `git checkout` of Schema Voyager's repo.

## ingest schema data

Instead of running `standalone`, run `ingest` with the sources you usually use:

```sh
clojure -X:cli ingest \
  :sources '[{:file/name "resources/main-schema.edn"}
             {:file/name "resources/supplemental-schema.edn"}]'
```

Every run of this will update an EDN file.
The EDN file contains your schema in a DataScript DB.

If this command line gets unwieldy, you can create an alias in Schema Voyager's deps.edn.
Use the approaches described in [the usage docs](installation-and-usage.md#As-an-alias) to define the alias (perhaps adding Datomic dependencies).
Substitute `schema-voyager.cli/ingest` where those docs mention `schema-voyager.cli/standalone`.

## live web page

Now you need a live version of the JS.
The JS will update as the underlying DataScript DB changes.

You have some options about how to start a JS server. You can do it from your terminal or from an editor.

From the terminal:

```sh
yarn run watch-js
```

Or, you can start shadow-cljs from your editor. For example, from an Emacs ClojureScript REPL, started in Schema Voyager's repo:

```emacs
M-x cider-jack-in-cljs
<choose shadow-cljs>
<choose shadow>
<choose :app>
```

> You can speed up the restart time of `yarn run watch-js` by running the following in a separate terminal.
> Wait for it to report that an nREPL server has been started before running watch-js.
> 
> ```sh
> yarn run js-server
> ```

Then you need an HTML file which will reference the JS as it changes.
That is, you can't use the typical standalone web page.

```sh
yarn run html # only once, or if resources/assets/index.html has changed
```

Also compile the CSS file:
```sh
yarn run compile-css
```

Or, if you're hacking on Schema Voyager, and will be changing CSS classes:
```sh
yarn run watch-css
```

After everything is loaded, open [http://localhost:8080](http://localhost:8080).

## re-running ingestion

As you change your schema, [re-run](#ingest-schema-data) the ingestion.
When it has finished and after a short delay, changes will be reflected on the page.

## DataScript

More adventurous technical users might enjoy exploring the DataScript DB directly in a Clojure REPL.

You can ingest the usual [sources](doc/sources.md) into an in-memory DB:
```clojure
(def db (schema-voyager.cli/ingest-into-db {:sources [,,,]}))
```

Alternatively, `schema-voyager.cli/ingest` saves the db in `resources/schema_voyager_db.edn`.
This is the db used by the web page.
It can be read with:

```clojure
(edn/read-string (io/resource "schema_voyager_db.edn"))
```
