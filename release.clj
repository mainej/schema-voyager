(ns release
  (:require [clojure.tools.build.api :as b]
            [org.corfield.build :as bb]
            [clojure.string :as string]))

(def ^:private lib 'com.github.mainej/schema-voyager)
(def ^:private rev-count (Integer/parseInt (b/git-count-revs nil)))
(def ^:private semantic-version "1.1")
(defn- format-version [revision] (format "%s.%s" semantic-version revision))
(def ^:private version (format-version rev-count))
(def ^:private next-version (format-version (inc rev-count)))
(def ^:private tag (str "v" version))
(def ^:private basis (b/create-basis {:root    nil
                                      :user    nil
                                      :project "deps.edn"}))

(defn- die
  ([message & args]
   (die (apply format message args)))
  ([message]
   (binding [*out* *err*]
     (println message))
   (System/exit 1)))

(defn- git [command args]
  (b/process (assoc args :command-args (into ["git"] command))))

(defn- git-rev []
  (let [{:keys [exit out]} (git ["rev-parse" "HEAD"] {:out :capture})]
    (when (zero? exit)
      (string/trim out))))

(defn- git-push [params]
  (println "\nSyncing with github...")
  (when-not (and (zero? (:exit (git ["push" "origin" tag] {})))
                 (zero? (:exit (git ["push" "origin"] {})))
                 (zero? (:exit (git ["push" "origin" "gh-pages"] {}))))
    (die "\nCouldn't sync with github."))
  params)

(defn- assert-clojars-creds [params]
  (when-not (System/getenv "CLOJARS_USERNAME")
    (die "\nMissing required CLOJARS_* credentials."))
  params)

(defn- assert-changelog-updated [params]
  (println "\nChecking that CHANGELOG references tag...")
  (when-not (string/includes? (slurp "CHANGELOG.md") tag)
    (die (string/join "\n"
                      ["CHANGELOG.md must include tag."
                       "  * If you will amend the current commit, use version %s"
                       "  * If you intend to create a new commit, use version %s"]) version next-version))
  params)

(defn- scm-clean?
  ([] (scm-clean? {}))
  ([opts]
   (string/blank? (:out (git ["status" "--porcelain"] (assoc opts :out :capture))))))

(defn- assert-scm-clean [params]
  (println "\nChecking that working directory is clean...")
  (when-not (scm-clean?)
    (die "\nGit working directory must be clean. Run `git commit`"))
  params)

(defn- assert-scm-tagged [params]
  (println "\nChecking that tag exists and is on HEAD...")
  (when-not (zero? (:exit (git ["rev-list" tag] {:out :ignore})))
    (die "\nGit tag %s must exist. Run `bin/release/tag`" tag))
  (let [{:keys [exit out]} (git ["describe" "--tags" "--abbrev=0" "--exact-match"] {:out :capture})]
    (when-not (and (zero? exit)
                   (= (string/trim out) tag))
      (die (string/join "\n"
                        [""
                         "Git tag %s must be on HEAD."
                         ""
                         "Proceed with caution, because this tag may have already been released. If you've determined it's safe, run `git tag -d %s` before re-running `bin/tag-release`."]) tag tag)))
  params)

(defn- build-template "Create the template html file" [params]
  (println "\nBuilding template html...")
  (when-not (zero? (:exit (b/process {:command-args ["clojure" "-X:build-template"]})))
    (die "\nCould not build template html."))
  params)

(defn- copy-template-to-jar-resources [params]
  ;; Usually the template file is ignored. During a release we put it in the
  ;; classes directory so that it is available when Schema Voyager is used as a
  ;; dependency. (It's used by `standalone`, the primary interface into Schema
  ;; Voyager). This lets us keep the large template file out of version control.

  ;; IMPORTANT: keep this in sync with
  ;; `schema-voyager.build.standalone-html/template-file-name`
  ;; We intentionally do not depend on `schema-voyager.build.standalone-html`,
  ;; so that the release can be run as `clojure -T:release`
  (b/copy-file {:src    "resources/standalone-template.html"
                :target "target/classes/standalone-template.html"})
  params)

(defn- build-standalone-example [params]
  (b/process {:command-args ["clojure" "-X:cli" "standalone" (str params)]}))

(defn- build-standalone-examples [params]
  (println "\nBuilding sample projects for GitHub Pages...")
  ;; expects _site to have been initialized:
  ;; git fetch origin gh-pages
  ;; git worktree add _site gh-pages
  (when-not (-> {:sources     [{:file/name "resources/mbrainz-schema/schema.edn"}
                               {:file/name "resources/mbrainz-schema/enums.edn"}
                               {:file/name "resources/mbrainz-schema/supplemental.edn"}]
                 :output-path "_site/mbrainz-schema.html"}
                build-standalone-example
                :exit
                zero?)
    (die "\nCouldn't create mbrainz-schema.html"))
  ;; A meta view of Datomic's schema
  (when-not (-> {:sources     [{:file/name "resources/datomic-schema/schema.edn"}
                               {:file/name "resources/datomic-schema/fixes.edn"}
                               {:file/name "resources/datomic-schema/supplemental.edn"}]
                 :output-path "_site/datomic-schema.html"}
                build-standalone-example
                :exit
                zero?)
    (die "\nCouldn't create datomic-schema.html"))
  ;; A meta view of Schema Voyager. Shows Datomic properties and supplemental
  ;; Schema Voyager properties and their relationships.
  (when-not (-> {:sources     [{:file/name "resources/datomic-schema/schema.edn"}
                               {:file/name "resources/datomic-schema/fixes.edn"}
                               {:file/name "resources/datomic-schema/supplemental.edn"}
                               {:file/name "resources/schema-voyager-schema/schema.edn"}
                               {:file/name "resources/schema-voyager-schema/supplemental.edn"}]
                 :output-path "_site/schema-voyager-schema.html"}
                build-standalone-example
                :exit
                zero?)
    (die "\nCouldn't create schema-voyager-schema.html"))
  (when-not (or (scm-clean? {:dir "./_site"})
                (zero? (:exit (git ["commit" "-a" "--no-gpg-sign" "-m" "Deploy updates"]
                                   {:dir "./_site"}))))
    (die "\nCouldn't commit GitHub Pages"))
  params)

#_{:clj-kondo/ignore #{:clojure-lsp/unused-public-var}}
(defn tag-release "Tag the HEAD commit for the current release." [params]
  (when-not (zero? (:exit (git ["tag" "-a" tag "-m" tag] {})))
    (die "\nCouldn't create tag %s." tag))
  params)

(defn run-tests [params]
  (-> params bb/run-tests))

(defn check-release
  "Check that the library is ready to be released.

  * Tests pass
  * No outstanding commits
  * Git tag for current release exists in local repo
  * CHANGELOG.md references new tag"
  [params]
  (-> params
      (run-tests)
      (assert-changelog-updated)
      ;; after assertions about content, so any change can be committed/amended
      (assert-scm-clean)
      ;; last, so that correct commit is tagged
      (assert-scm-tagged)))

(defn jar
  [params]
  (-> params
      (assoc :lib lib
             :version version
             :basis   basis
             :tag     (git-rev))
      (build-template)
      (copy-template-to-jar-resources)
      ;; TODO: jar doesn't really use anything from /resources. Should it be
      ;; copied into jar?
      (bb/jar)))

#_{:clj-kondo/ignore #{:clojure-lsp/unused-public-var}}
(defn release
  "Release the library.

  * Confirm that we are ready to release
  * Build template file
  * Build JAR, including the template
  * Build the content for GitHub Pages from the template
  * Deploy the JAR to Clojars
  * Ensure everything is available on Github"
  [params]
  (let [params (assoc params :lib lib :version version)]
    (-> params
        (assert-clojars-creds)
        (bb/clean)
        (check-release)
        (jar)
        ;; After jar (and more importantly build-template), so that examples agree
        ;; with released template.
        (build-standalone-examples)
        (bb/deploy)
        (git-push))
    (println "\nDone")
    params))
