# Explore

Now the fun part!

## Create web page

After ingesting your schema, generate an HTML page for it:

```sh
yarn --prod run standalone
```

When it's done, open `target/standalone.html` in your browser.
Have fun getting to know your schema!

## Share web page

`target/standalone.html` is a single file containing HTML, CSS and JS.
It embeds your entire schema and doesn't need to communicate with a server.
Therefore it can be committed, emailed, hosted on Netlify or a server of your choice, or otherwise shared anywhere.

## Share ERDs

Within the HTML you're sure to notice the diagrams of collections and their relationships.
These diagrams can be exported as SVG files.
Open the configuration menu in the upper left of any diagram and click Export SVG.
