### CSS

#### development

```bash
	NODE_ENV=development npx postcss -c postcss.config.js -o target/styles.css assets/css/styles.css
```

#### production

```bash
	NODE_ENV=production npx postcss -c postcss.config.js -o target/styles.css assets/css/styles.css
```

### REPL

```emacs
cider-jack-in-cljs
  shadow-cljs
  shadow
  app
```

Respond 'y' to view app at http://localhost:8080.

### Release

Compile with optimizations with `release` sub-command:

```bash
yarn release
yarn html
yarn serve # serving target/ on http://localhost:8080
```
