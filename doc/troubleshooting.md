
## Missing Datomic deps

When using a [Datomic source](/doc/sources.md#datomic-source), you'll need to add Datomic dependencies.
Since not all projects that use Schema Voyager will need to connect to Datomic, it is not one of the default dependencies.

If you don't you'll see an error referencing Datomic.
These errors manifest in a few forms.
In all cases, the fix is to ensure the deps are included in your deps.edn.

### Could not locate datomic/client/api__init.class, datomic/client/api.clj or datomic/client/api.cljc on classpath.

Diagnosis: You don't have com.datomic/client-cloud on your classpath.

### Could not locate datomic/dev_local/impl__init.class, datomic/dev_local/impl.clj or datomic/dev_local/impl.cljc on classpath. Please check that namespaces with dashes use underscores in the Clojure file name.

Diagnosis: You are trying to use a `:dev-local` server but you don't have com.datomic/dev-local on your classpath.

### :server-type must be one of :ion, :cloud, or :peer-server

Diagnosis: Same as above, you are trying to use a `:dev-local` server but you don't have com.datomic/dev-local on your classpath.
