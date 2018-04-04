This directory contains a source code for my project at DataStax bootcamp. The project is
about implementing the 1st stage of the retail company modernization - implementing search
based on the inventory, including the type-ahead search suggestions.

The code was written for DSE 6.0 EAP2, but may, or may not, work on DSE 5.1.x...

The code is organized as following way:
- `diagrams` - some diagrams on how user perform some operations, like, search, etc.
- `cql` - CQL code. The `create-ks.cql` should be executed first to create keyspaces, and
  the rest of files could be executed in arbitrary order;
- `java` - code for populating the inventory-related tables with fake data (generated via
  `java-faker`);
- `scala` - code for performance testing using the recently-released
  [gatling-dse-plugin](https://github.com/datastax/gatling-dse-plugin);
- `rest` - simple prototype written in Clojure/Compojure that implements some APIs for
  search, etc. (type-ahead search in web interface isn't implemented although - only
  corresponding REST API).

