---
kind: error_handling
name: Ad-hoc JDBC Exception Handling with Throwable Catches and printStackTrace
category: error_handling
scope:
    - '**'
source_files:
    - src/main/java/com/oceanbase/obvector4j/ObVecClient.java
    - src/main/java/com/oceanbase/obvector4j/ObVecJsonClient.java
    - src/main/java/com/oceanbase/obvector4j/hybrid/HybridSearchEngine.java
    - src/main/java/com/oceanbase/obvector4j/json_table/JsonTableMetadata.java
---

This repository does not define a dedicated error-handling framework, custom exception hierarchy, or centralized error types. Instead, it uses ad-hoc handling scattered across the two client entry points (`ObVecClient`, `ObVecJsonClient`) and a few supporting classes.

**What is used**
- Java checked exceptions: `java.sql.SQLException` is caught everywhere JDBC calls are made.
- Broad catch-all: many blocks catch `Throwable e` (not just `Exception`), then call `e.printStackTrace()` — no logging framework, no rethrow, no wrapping.
- Unchecked exceptions thrown directly by the SDK for invalid arguments / unsupported features:
  - `IllegalArgumentException` for null/empty inputs, mismatched column sizes, invalid SQL, unsupported column specs.
  - `UnsupportedOperationException` when server version < 4.6.0 or when a requested feature is unavailable.
  - `java.rmi.UnexpectedException` (imported from `java.rmi`) is thrown once in `ObVecClient` for an internal invariant violation.
- A single `java.util.logging.Logger` instance exists only on `ObVecJsonClient`; it is used for informational messages, never for error paths.
- No sentinel errors, no error-code enum, no middleware, no `try-with-resources` pattern for connection cleanup (manual rollback is attempted inside catch blocks).

**Where it lives**
- `src/main/java/com/oceanbase/obvector4j/ObVecClient.java` — every public method wraps JDBC calls in `try { ... } catch (Throwable e) { e.printStackTrace(); }` and also catches `SQLException` separately; validation throws `IllegalArgumentException`/`UnsupportedOperationException`.
- `src/main/java/com/oceanbase/obvector4j/ObVecJsonClient.java` — same pattern plus a `Logger` field; most error branches still fall back to `printStackTrace()`.
- `src/main/java/com/oceanbase/obvector4j/hybrid/HybridSearchEngine.java` — one `catch (Throwable e)` block that ignores the exception after printing.
- `src/main/java/com/oceanbase/obvector4j/json_table/JsonTableMetadata.java` — catches `Throwable` and `SQLException` around metadata refresh.
- All other packages (`filter`, `hybrid/v460/dsl`, `model`, `schema`, `util`, `version`) contain no try/catch at all; they propagate unchecked exceptions up to the callers above.

**Architecture & conventions**
- Error propagation is shallow: clients swallow `Throwable` and log via `System.err` rather than surfacing structured errors to callers.
- Validation failures are signaled immediately via unchecked exceptions at the API boundary; runtime/JDBC failures are swallowed.
- There is no consistent convention distinguishing recoverable vs. fatal errors — everything ends up as a stack trace dump.

**Rules developers should follow**
1. Do **not** add new custom exception classes unless you introduce a shared `errors` package first.
2. Prefer throwing `IllegalArgumentException` for bad arguments and `UnsupportedOperationException` for unimplemented/server-version-gated features.
3. Avoid catching `Throwable` in new code; if you must wrap JDBC calls, prefer `try-with-resources` and rethrow as a domain-specific unchecked exception rather than calling `printStackTrace()`.
4. Replace `e.printStackTrace()` with a proper logger (SLF4J/Logback) before adding any new catch blocks.