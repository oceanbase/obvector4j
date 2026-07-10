---
kind: configuration_system
name: No runtime configuration system — direct constructor injection
category: configuration_system
scope:
    - '**'
source_files:
    - src/main/java/com/oceanbase/obvector4j/ObVecClient.java
    - pom.xml
---

This repository does not implement a runtime configuration system. The ObVecClient and ObVecJsonClient constructors accept connection parameters (URI, user, password) directly as method arguments; there is no config file loader, environment-variable resolver, properties parser, or feature-flag mechanism in the main source code. All build-time settings are declared in pom.xml (Maven properties, profiles for unit/integration/remote test execution), but these are consumed by the Maven toolchain, not at application runtime. Test helpers read environment variables (OCEANBASE_URI, OCEANBASE_USER, OCEANBASE_PASSWORD) only to bootstrap Testcontainers or remote IT suites — this is test-only behavior and not part of the library's public API. Consequently, consumers must supply all runtime configuration explicitly when constructing clients.