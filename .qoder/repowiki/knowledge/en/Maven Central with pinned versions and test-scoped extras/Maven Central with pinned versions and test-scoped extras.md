---
kind: dependency_management
name: Maven Central with pinned versions and test-scoped extras
category: dependency_management
scope:
    - '**'
source_files:
    - pom.xml
---

The project uses a single-module Maven build (`pom.xml`) with no parent POM, BOM, or `<dependencyManagement>` section. All third-party libraries are declared directly in the top-level `<dependencies>` block and resolved from Maven Central (no private registry or `settings.xml` overrides visible in the repo).

**Runtime dependencies (compile scope)**
- `com.oceanbase:oceanbase-client:2.4.1` — official OceanBase MySQL-mode JDBC driver; required for all client operations.
- `com.googlecode.json-simple:json-simple:1.1.1` — lightweight JSON parser used by the JSON virtual-table client.
- `com.github.jsqlparser:jsqlparser:4.7` — SQL AST parser used to build/alter table DDL fragments; pinned at 4.7 because 5.x requires Java 11+ while this library targets Java 8.

**Test-only dependencies (test scope)**
- `junit:junit:4.13.2` — unit/integration test framework.
- `org.testcontainers:testcontainers:1.19.8` + `org.testcontainers:oceanbase:1.19.8` — containerized OceanBase instances for integration tests.
- `org.slf4j:slf4j-api:1.7.36` + `org.slf4j:slf4j-simple:1.7.36` — logging API and console implementation, also test-scoped so the published artifact carries no runtime logging dependency.

**Versioning & update policy**
- Every dependency is pinned to an explicit version string inside `pom.xml`; there is no `${...}` property indirection, no BOM, and no dynamic range. Version bumps are applied inline and accompanied by explanatory comments (e.g., jsqlparser downgrade note, JUnit upgrade note).
- No lockfile exists (Maven does not ship one by default); reproducibility relies on exact versions plus Maven's local `~/.m2/repository` cache.
- No vendoring strategy is used — jars are downloaded on demand from Maven Central.

**Build / distribution**
- The `distributionManagement` section publishes snapshots and staged releases to Sonatype OSSRH (`s01.oss.sonatype.org`). There is no signing plugin configured in this POM, so signing must be supplied externally (e.g., via CI credentials or `~/.m2/settings.xml`).
- Three Maven profiles (`integration`, `remote-it`, `all-tests`) control which test classes run but do not alter dependency sets.

**Conventions developers should follow**
- Add new compile-time dependencies only when the main source code imports them; keep test-only libraries under `<scope>test</scope>` so the published JAR stays minimal.
- Pin every version explicitly in `pom.xml`; avoid properties unless the same version is reused across multiple artifacts.
- When upgrading a dependency that raises the minimum Java level above 8, document the change in the comment next to the version line (as done for jsqlparser) and revalidate the `maven.compiler.source/target=1.8` setting.