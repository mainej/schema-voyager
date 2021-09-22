# Advanced exploration

You can use Schema Voyager as a live tool, which responds to updates to the schema ingestion process as soon as they occur.
If you're hacking on Schema Voyager itself, it's also a good way to see updates to the CLJS.

> This is a bit more involved.
I spend a lot of time in this mode while developing Schema Voyager and it might be useful when you're iterating on a new schema design, but it's definitely more advanced.

## Pre-requisites

To work in this mode, you have to be in Schema Voyager's directory, not your project's directory.
First do a `git checkout` of Schema Voyager's repo.

You need an HTML file which will reference the JS as it changes.
That is, you can't use the typical standalone web page created by `schema-voyager.cli/standalone`.

```sh
bin/dev/html # only once, or if resources/assets/index.html has changed
```

Also compile the CSS:
```sh
bin/dev/css --minify
```

Or, if you're hacking on Schema Voyager, and will be changing CSS classes:
```sh
bin/dev/css --watch
```

Now you need a live version of the JS.

You have some options about how to start a JS server.
You can do it from your terminal or from an editor.

From the terminal:

```sh
bin/dev/js/watch
```

Or, you can start shadow-cljs from your editor. For example, from an Emacs ClojureScript REPL, started in Schema Voyager's repo:

```emacs
M-x cider-jack-in-cljs
<choose shadow-cljs>
<choose shadow>
<choose :app>
```

> To speed up the restart time of `bin/dev/js/watch` run the following in a separate terminal.
> Wait for it to report that an nREPL server has been started before running `bin/dev/js/watch`.
> 
> ```sh
> bin/dev/js/server
> ```

## Ingest schema data

Instead of running `standalone`, run `ingest` with the sources you usually use:

```sh
clojure -X:cli ingest \
  :sources '[{:file/name "resources/main-schema.edn"}
             {:file/name "resources/supplemental-schema.edn"}]'
```

Every run of this will update an EDN file.
The EDN file contains your schema in a DataScript DB, which is slurped into the JS.

> If this command line gets unwieldy, you can extend _Schema Voyager's_ `:cli` alias in its deps.edn.
Use the approaches described in [the usage docs](installation-and-usage.md#As-an-alias) to define the alias (perhaps adding Datomic dependencies).
Then invoke `clojure -X:cli ingest` instead of `clojure -X:cli standalone`.

After everything is loaded, open [http://localhost:8080](http://localhost:8080).

## Re-running ingestion

As you change your schema, [re-run](#ingest-schema-data) the ingestion.
When it has finished and after a short delay, changes will be reflected on the page.

## DataScript

More adventurous technical users might enjoy exploring the DataScript DB directly in a Clojure REPL.

You can ingest the usual [sources](/doc/sources.md) into an in-memory DB:
```clojure
(def db (schema-voyager.cli/ingest-into-db {:sources [,,,]}))
```

Alternatively, `schema-voyager.cli/ingest` saves the db in `resources/schema_voyager_db.edn`.
This is the db used by the web page.
It can be read with:

```clojure
(edn/read-string (io/resource "schema_voyager_db.edn"))
```
