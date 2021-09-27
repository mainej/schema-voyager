# Change Log
All notable changes to this project will be documented in this file. This change
log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

## [Unreleased]

## [1.0.192-SNAPSHOT] - 2021-09-26
### Changed
* Updated shadow-cljs to 2.15.10
* To avoid having to store the template HTML file (~800K) in the repo, changed
  deployment strategies. The library is now distributed as a JAR from Clojars,
  with the template file compiled into it as a resource.

## [0.1.186] - 2021-09-25

### Changed
* Moved hosting of example mbrainz schema page from Netlify to GitHub Pages.

### Fixed
* Don't show enums if a project doesn't use them
* Make diagram colors agree with attribute details

## [0.1.171] - 2021-09-21
Initial release, with support for exploring schema data from a standalone web
page. Schema data can come from many sources, including from Datomic and from
supplemental files.

This project has existed for some time, but this is the first release that can
be used from other projects without cloning this library.

[Unreleased]: https://github.com/mainej/f-form/compare/v1.0.192-SNAPSHOT...main
[1.0.192-SNAPSHOT]: https://github.com/mainej/f-form/compare/v0.1.186...v1.0.192-SNAPSHOT
[0.1.186]: https://github.com/mainej/f-form/compare/v0.1.171...v0.1.186
[0.1.171]: https://github.com/mainej/schema-voyager/tree/v0.1.171
