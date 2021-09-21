(ns release
  (:require [clojure.tools.build.api :as b]
            [clojure.string :as string]))

(def ^:private rev-count (Integer/parseInt (b/git-count-revs nil)))
(def ^:private semantic-version "0.1")
(defn- format-version [revision] (format "%s.%s" semantic-version revision))
(def ^:private version (format-version rev-count))
(def ^:private next-version (format-version (inc rev-count)))
(def ^:private tag (str "v" version))

(defn- die
  ([code message & args]
   (die code (apply format message args)))
  ([code message]
   (binding [*out* *err*]
     (println message))
   (System/exit code)))

(defn- git [command args]
  (b/process (assoc args :command-args (into ["git"] command))))

(defn- git-rev []
  (let [{:keys [exit out]} (git ["rev-parse" "HEAD"] {:out :capture})]
    (when (zero? exit)
      (string/trim out))))

(defn- git-push [params]
  (println "\nSyncing with github...")
  (when-not (and (zero? (:exit (git ["push" "origin" tag] {})))
                 (zero? (:exit (git ["push" "origin"] {}))))
    (die 15 "\nCouldn't sync with github."))
  params)

(defn- assert-changelog-updated [params]
  (println "\nChecking that CHANGELOG references tag...")
  (when-not (string/includes? (slurp "CHANGELOG.md") tag)
    (die 10 (string/join "\n"
                         ["CHANGELOG.md must include tag."
                          "  * If you will amend the current commit, use version %s"
                          "  * If you intend to create a new commit, use version %s"]) version next-version))
  params)

(defn- assert-scm-clean [params]
  (println "\nChecking that working directory is clean...")
  (let [git-changes (:out (git ["status" "--porcelain"] {:out :capture}))]
    (when-not (string/blank? git-changes)
      (println git-changes)
      (die 12 "Git working directory must be clean. Run `git commit`")))
  params)

(defn- assert-scm-tagged [params]
  (println "\nChecking that tag exists and is on HEAD...")
  (when-not (zero? (:exit (git ["rev-list" tag] {:out :ignore})))
    (die 13 "\nGit tag %s must exist. Run `bin/release/tag`" tag))
  (let [{:keys [exit out]} (git ["describe" "--tags" "--abbrev=0" "--exact-match"] {:out :capture})]
    (when-not (and (zero? exit)
                   (= (string/trim out) tag))
      (die 14 (string/join "\n"
                           [""
                            "Git tag %s must be on HEAD."
                            ""
                            "Proceed with caution, because this tag may have already been released. If you've determined it's safe, run `git tag -d %s` before re-running `bin/tag-release`."]) tag tag)))
  params)

(defn- build-template "Create the standalone_template.html file" [params]
  (println "\nBuilding standalone_template.html...")
  (b/process {:command-args ["clojure" "-X:build-template"]})
  params)

(defn- post-release-message "Suggest updating the docs to reference the new release." [params]
  (println (format "\nRelease finished. Now update doc/installation-and-usage.md to reference {:git/tag \"%s\", :git/sha \"%s\"}"
                   tag
                   (git-rev)))
  params)

#_{:clj-kondo/ignore #{:clojure-lsp/unused-public-var}}
(defn tag-release "Tag the HEAD commit for the current release." [params]
  (when-not (zero? (:exit (git ["tag" "-a" tag "-m" tag] {})))
    (die 15 "\nCouldn't create tag %s." tag))
  params)

(defn check-release
  "Check that the library is ready to be released.

  * Template resource built
  * No outstanding commits
  * Git tag for current release exists in local repo
  * CHANGELOG.md references new tag"
  [params]
  (build-template params)
  (assert-changelog-updated params)
  ;; after assertions about content, so any change can be committed/amended
  (assert-scm-clean params)
  ;; last, so that correct commit is tagged
  (assert-scm-tagged params)
  params)

#_{:clj-kondo/ignore #{:clojure-lsp/unused-public-var}}
(defn release
  "Release the library.

  * Confirm that we are ready to release
  * Ensure the tag is available on Github"
  [params]
  (check-release params)
  (git-push params)
  (post-release-message params)
  params)
