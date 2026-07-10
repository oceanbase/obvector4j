---
kind: logging_system
name: No Centralized Logging System
category: logging_system
scope:
    - '**'
source_files:
    - src/main/java/com/oceanbase/obvector4j/ObVecJsonClient.java
---

This repository does not implement a centralized logging system. The only structured logging present is in `ObVecJsonClient`, which uses the JDK built-in `java.util.logging.Logger` with an adjustable `Level` constructor parameter and emits `logger.info(...)` messages for JSON-table lifecycle events (create, alter, drop). All other production code — notably `ObVecClient` and its subclasses — relies exclusively on `e.printStackTrace()` and occasional `System.out.println` calls for error reporting and diagnostics. There are no SLF4J/Logback/Log4j2 dependencies, no log-level configuration files, no structured log fields, and no unified logger abstraction across modules. Test code additionally wires Testcontainers' `Slf4jLogConsumer` to route Docker container logs through SLF4J, but this is isolated to tests and does not affect library runtime behavior.