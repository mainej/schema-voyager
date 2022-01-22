# Change Log
All notable changes to this project will be documented in this file. This change
log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

## [Unreleased]

## [2.0.240] - 2022-01-21
### Added
* SVG diagrams can be resized to fit screen.
* Additional collection data, if any, is shown on the collection page, rendered
  similarly to additional attribute data.
### Changed
* Bump deps, upgrade Tailwind CSS

## [2.0.228] - 2021-12-14
### Changed
* Added Subresource integrity attribute to graphviz script, as per
  best-practices when using CDNs. Changed CDNs.

## [2.0.221] - 2021-10-10
### Fixed
* Fixed generation of pom.xml.

## [2.0.220] - 2021-10-10
### Changed
* Split up project into separate pieces: the core, the web, and the build.
  The impetus for this was to prevent build failures when CLJDOC analyzed a
  release. But it also clarifies which pieces are used when, and what their
  dependencies are. It also makes the jar smaller.
* **BREAKING** In addition to moving the files, also renamed some namespaces.
  This more clearly deliniates the high level responsibilities of the core code.
  The primary interface to Schema Voyager, `schema-voyager.cli`, is unchanged,
  so this renaming is unlikely to affect many users.

## [1.1.208] - 2021-09-26
Centralized and tested logic for extracting data for views, eliminating or
reducing view logic.

### Changed
* Simplified method of including Datomic's attributes in schema. If you were
  using `:coll-exclusions` and `:entity-exclusions` to include Datomic
  attributes in your schema, instead you should now add those attributes to
  `:datomic-entity-inclusions`.

## [1.0.197] - 2021-09-26
First 1.0 release.

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

[Unreleased]: https://github.com/mainej/f-form/compare/v2.0.240...main
[2.0.240]: https://github.com/mainej/f-form/compare/v2.0.228...v2.0.240
[2.0.228]: https://github.com/mainej/f-form/compare/v2.0.221...v2.0.228
[2.0.221]: https://github.com/mainej/f-form/compare/v2.0.220...v2.0.221
[2.0.220]: https://github.com/mainej/f-form/compare/v1.1.208...v2.0.220
[1.1.208]: https://github.com/mainej/f-form/compare/v1.0.197...v1.1.208
[1.0.197]: https://github.com/mainej/f-form/compare/v1.0.192-SNAPSHOT...v1.0.197
[1.0.192-SNAPSHOT]: https://github.com/mainej/f-form/compare/v0.1.186...v1.0.192-SNAPSHOT
[0.1.186]: https://github.com/mainej/f-form/compare/v0.1.171...v0.1.186
[0.1.171]: https://github.com/mainej/schema-voyager/tree/v0.1.171
