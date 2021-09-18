# Advanced exploration

You can use Schema Voyager as a live tool, which responds to updates to the
schema ingestion process as soon as they occur.

> This is a bit more involved.
I spend a lot of time in this mode while developing Schema Voyager and it might be useful when you're iterating on a new schema design, but it's definitely a more advanced use-case.

## live web page

First you need a live version of the JS.
This JS will update as the underlying Datascript DB changes.

You can start a JS server from from your terminal or editor.

From the terminal:

```sh
yarn run watch-js
```

Or, from an Emacs ClojureScript REPL:

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
That is, you can't use the typical standalone HTML.

```sh
yarn run html # only once, or if assets/index.html has changed
```

Also compile the CSS file:
```sh
yarn run watch-css
```

After everything is loaded, open [http://localhost:8080](http://localhost:8080).

## re-running ingestion

After you change your schema, [trigger](doc/installation.md) a re-run of the ingestion.
When it has finished, after a short delay changes will be reflected on the page.

## datascript

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
