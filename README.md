## Hack

```bash
yarn html # optional, only once
yarn css # optional, only if css changed
```

### Terminal

```bash
yarn run js-server
yarn run watch-js # in separate terminal tab
```

### REPL

```emacs
cider-jack-in-cljs
```

Then open http://localhost:8080

## Release

Compile with optimizations:

```bash
yarn --prod compile-js
yarn --prod html
yarn --prod css
```
