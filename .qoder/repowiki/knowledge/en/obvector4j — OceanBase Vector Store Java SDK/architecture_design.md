All source lives in one flat package tree under a single `pom.xml`; children are layered by responsibility rather than split into artifacts:
- `schema` owns the canonical in-memory table/column/metric model and version-gating helpers; every other child imports it to stay consistent with server capabilities.
- `sql_model` provides JDBC-bound value wrappers consumed by both `json_table` (for INSERT/UPDATE rows) and `hybrid_search` (for query parameter binding).
- `filter` builds a composable AST that `hybrid_search` translates into either legacy RRF SQL or the OceanBase 4.6.0+ HYBRID_SEARCH clause.
- `json_table` exposes typed column/value types used by the client's JSON virtual-table API.
- `tests` exercises the whole stack against a Testcontainers-managed OceanBase instance via profiles defined in this POM (`unit`, `integration`, `remote-it`, `all-tests`).
The root POM centralises shared dependencies (oceanbase-client 2.4.1, json-simple, jsqlparser 4.7 for Java 8 compatibility, SLF4J, Testcontainers), enforces Java 8 source/target, and wires Surefire test profiles that each child's test classes target by path pattern.