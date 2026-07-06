# 02 · Architecture

> Read in order: [01-getting-started](01-getting-started.md) → **02** → [03-hybrid-search](03-hybrid-search.md) → [04-filter](04-filter.md)  
> 中文：[02-项目与架构](../zh/02-项目与架构.md)

---

## Overview

**obvector4j** is a Java JDBC SDK for OceanBase vector store:

- Vector table CRUD and ANN search
- Hybrid search (full-text + vector, scalar filter + vector)
- Custom SQL and keyword-driven HYBRID_SEARCH DSL (4.6.0+)
- JSON virtual tables via `ObVecJsonClient`

Root package: `com.oceanbase.obvector4j`

## Package layout

| Package | Purpose |
|---------|---------|
| *(root)* | `ObVecClient`, `ObVecJsonClient` — entry points |
| `schema` | `DataType`, `ObCollectionSchema`, `IndexParam` |
| `model` | `Sqlizable`, `SqlVector`, `SqlText`, … |
| `hybrid.core` | **`HybridSearch`**, DSL builders — **≥ 4.6.0 only** (throws otherwise) |
| `hybrid.core.dsl` | `HybridDslKeys`, `HybridDslNode`, `HybridDsl` |
| `hybrid` | Version-adaptive text/scalar + vector builders |
| `filter` | Type-safe `Filter` / `FilterBuilder` |
| `version` | `OceanBaseVersion`, `supportsHybridSearchSql()` |
| `json_table` | Internal JSON virtual table implementation |
| `util` | Shared helpers (`VectorMetric`, `JdbcTypeMapper`, …) |

### `hybrid.core` — keyword-driven DSL (4.6.0+)

Obtain via `ObVecClient.hybridSearch()` — **throws if cluster &lt; 4.6.0**.

## Public API cheat sheet

| Need | Import |
|------|--------|
| Client | `com.oceanbase.obvector4j.ObVecClient` |
| Schema | `com.oceanbase.obvector4j.schema.*` |
| Row values | `com.oceanbase.obvector4j.model.*` |
| Filters | `com.oceanbase.obvector4j.filter.FilterBuilder` |
| Custom DSL (4.6.0+) | `client.hybridSearch()` → `com.oceanbase.obvector4j.hybrid.core.*` |
| DSL keywords | `com.oceanbase.obvector4j.hybrid.core.dsl.HybridDslKeys` |

## Version notes

| Feature | OceanBase |
|---------|-----------|
| Native `HYBRID_SEARCH` SQL | **≥ 4.6.0** |
| Legacy hybrid paths | &lt; 4.6.0 |

Use `client.supportsHybridSearchSql()` at runtime.

## Tests

```bash
mvn test                  # unit only
mvn test -Pintegration    # + Testcontainers
mvn test -Premote-it      # remote cluster (env vars required)
```

## Related

| Doc | Topic |
|-----|-------|
| [01-getting-started](01-getting-started.md) | Install & quick start |
| [03-hybrid-search](03-hybrid-search.md) | Hybrid search & DSL reference |
| [04-filter](04-filter.md) | Filter API reference |
