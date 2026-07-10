---
kind: build_system
name: Maven Build & CI Pipeline
category: build_system
scope:
    - '**'
source_files:
    - pom.xml
    - .github/workflows/ci.yml
---

The project uses a single-module Maven build with Java 8 compatibility, Testcontainers-based integration tests, and GitHub Actions CI.

Build toolchain and compilation: pom.xml declares maven.compiler.source/target=1.8, enforced by the maven-compiler-plugin (3.11.0) with -Xlint:-options. Javadoc is generated during the default lifecycle via maven-javadoc-plugin (3.3.1) configured to fail on warnings; sources are attached via maven-source-plugin (3.2.1). The artifact is a plain jar under com.oceanbase:obvector4j, current version 1.0.7.

Dependency management: Runtime deps include oceanbase-client:2.4.1, json-simple:1.1.1, jsqlparser:4.7 (pinned at 4.x because 5.x requires Java 11+). Test-only deps include junit:4.13.2, testcontainers:testcontainers:1.19.8 plus testcontainers:oceanbase:1.19.8, and slf4j-api/slf4j-simple:1.7.36.

Test execution profiles: Default mvn test runs only src/test/java/**/unit/**. -Pintegration adds **/integration/container/** which spins up an OceanBase container automatically via Testcontainers. -Premote-it runs **/integration/remote/** against a user-provided remote OceanBase instance. -Pall-tests combines unit and all integration suites.

CI pipeline: .github/workflows/ci.yml triggers on push/PR to main, master, develop. Uses actions/setup-java@v4 with Temurin JDK 8 and Maven cache. Executes mvn clean test -Pintegration; Testcontainers starts the OceanBase container without manual setup. Uploads Surefire reports as artifacts for 7 days.

Distribution: distributionManagement points to Sonatype OSSRH snapshots and staging repos (sonatype-nexus-snapshots, sonatype-nexus-staging). No signing plugin is declared in this POM.