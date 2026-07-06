# 05 · HYBRID_SEARCH DSL Syntax & Examples

> Read in order: [03-hybrid-search](03-hybrid-search.md) → **05 DSL syntax** → [04-filter](04-filter.md)  
> 中文：[05-HYBRID_SEARCH-DSL语法](../zh/05-HYBRID_SEARCH-DSL语法.md)

This guide covers the **JSON DSL** for OceanBase **4.6.0+** `HYBRID_SEARCH`: grammar, semantics, and obvector4j **`hybrid.core`** builders. For overview and version notes see [03-hybrid-search](03-hybrid-search.md).

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [SQL wrapper & document shape](#sql-wrapper--document-shape)
3. [Three ways to use DSL](#three-ways-to-use-dsl)
4. [Top-level fields](#top-level-fields)
5. [query expressions](#query-expressions)
6. [bool queries](#bool-queries)
7. [knn vector search](#knn-vector-search)
8. [rank fusion](#rank-fusion)
9. [Pagination & min_score](#pagination--min_score)
10. [HybridDslKeys reference](#hybriddslkeys-reference)
11. [SDK classes & patterns](#sdk-classes--patterns)
12. [Constraints & best practices](#constraints--best-practices)
13. [End-to-end examples](#end-to-end-examples)
14. [Debugging & tests](#debugging--tests)
15. [Related docs](#related-docs)

---

## Prerequisites

| Item | Requirement |
|------|-------------|
| OceanBase | **≥ 4.6.0** (`client.supportsHybridSearchSql()`) |
| Table | Heap table `ORGANIZATION HEAP WITH COLUMN GROUP(all columns)` |
| Vector column | HNSW vector index required |
| Full-text columns | Full-text index; `match_phrase` needs `FTS_INDEX_TYPE=PHRASE_MATCH` |
| SDK entry | `client.hybridSearch()` (throws if cluster &lt; 4.6.0) |

Package: `com.oceanbase.obvector4j.hybrid.core` (keywords in `.core.dsl`)

---

## SQL wrapper & document shape

```sql
SELECT col1, col2, ...
FROM HYBRID_SEARCH(TABLE table_name, 'dsl_json_string') [alias];
```

Do **not** attach `WHERE` / `ORDER BY` / `LIMIT` on the same level as `HYBRID_SEARCH`; wrap in a subquery instead.

```json
{
  "query": { },
  "knn":   { },
  "rank":  { },
  "from":  0,
  "size":  10,
  "min_score": 0.5
}
```

At least one of `query` or `knn` is required. Multi-path vector search uses a **JSON array** for `knn`.

---

## Three ways to use DSL

### 1. Raw SQL

```sql
SELECT id FROM HYBRID_SEARCH(TABLE documents, '{
  "query": {"match": {"content": "OceanBase"}},
  "knn": {"field": "embedding", "k": 10, "query_vector": "[0.1,0.2,0.3]"},
  "rank": {"rrf": {"rank_window_size": 10, "rank_constant": 60}},
  "size": 5
}');
```

### 2. `searchWithDsl(table, dslJson, fields, types)`

```java
String dsl = HybridSearchDsl.create()
    .query(HybridDsl.match("content", "OceanBase"))
    .knn(HybridDsl.knn("embedding", queryVector, 10))
    .rank(HybridDsl.rrf(10, 60))
    .size(5)
    .toJsonString();

client.hybridSearch().searchWithDsl("documents", dsl,
    new String[] {"id", "title"}, null);
```

### 3. `customSearch()` builder (recommended)

```java
client.hybridSearch()
    .customSearch()
    .table("documents")
    .query(HybridDsl.match("content", "OceanBase"))
    .knn(HybridDsl.knn("embedding", queryVector, 10))
    .rank(HybridDsl.rrf(10, 60))
    .size(5)
    .outputFields("id", "title")
    .search();
```

Use `.buildDsl()` to inspect JSON without executing. Escape hatch: `queryDsl()` / `knnDsl()` / `rankDsl()` for raw JSON fragments.

---

## Top-level fields

| Key | Constant | Required | Description |
|-----|----------|:--------:|-------------|
| `query` | `QUERY` | one of | Full-text / scalar / bool |
| `knn` | `KNN` | one of | Vector search (object or array) |
| `rank` | `RANK` | no | RRF or weighted_sum |
| `from` | `FROM` | no | Offset (0-based), use with `size` |
| `size` | `SIZE` | no | Result count, default 10 |
| `min_score` | `MIN_SCORE` | no | Minimum `__score` threshold |

---

## query expressions

```
query = bool
      | match | multi_match | match_phrase | query_string
      | term | range | terms
      | json_contains | json_overlaps | json_member_of
      | array_contains | array_contains_all | array_overlaps
```

### Full-text

**match** — term-level match on one field:

```json
{"match": {"content": "Python JavaScript"}}
```

With parameters:

```json
{"match": {"content": {"query": "Python JavaScript", "operator": "OR", "boost": 1.2}}}
```

```java
HybridDsl.match("content", "Python JavaScript");
HybridDsl.match("content").query("kw").param(HybridDslKeys.BOOST, 0.3);
```

**multi_match** — multiple fields:

```java
HybridDsl.multiMatch(new String[] {"title^0.3", "content"}, "keyword");
```

**match_phrase** — phrase with optional slop:

```java
HybridDsl.matchPhrase("content", "machine learning", 2);
```

**query_string** — query-string syntax:

```java
HybridDsl.queryString(new String[] {"title", "content"}, "OceanBase AND vector");
```

### Scalar (non-scoring)

> Put `term` / `range` / `terms` in **`bool.filter`** or **`knn.filter`**, not in `must` / `should`.

```java
HybridDsl.term("status", 1);
HybridDsl.range("id").gte(3).lte(10);
HybridDsl.terms("category_id", 1, 2, 3);
HybridDsl.term("doc_json.name", "doc2");  // dotted JSON path
```

### JSON & Array

```java
HybridDsl.jsonContains("doc_json", "{\"name\":\"doc2\"}", "$");
HybridDsl.jsonOverlaps("doc_json", "[\"database\"]", "$.tags");
HybridDsl.arrayContains("tags_array1", "ios");
HybridDsl.arrayContainsAll("tags_array1", "a", "b");
HybridDsl.arrayOverlaps("tags_array1", "web", "mobile");
```

---

## bool queries

```json
{
  "bool": {
    "must":     [ ],
    "should":   [ ],
    "filter":   [ ],
    "must_not": [ ],
    "minimum_should_match": 1,
    "boost": 1.6
  }
}
```

| Clause | Logic | Scoring |
|--------|-------|:-------:|
| `must` | AND | yes |
| `should` | OR (controlled by `minimum_should_match`) | yes |
| `filter` | AND | no |
| `must_not` | NOT | no |

Rules:

1. At least one positive clause (`must` / `should` / `filter`); not `must_not` alone
2. Scalar/json/array expressions belong in **`filter`**
3. Clauses must not be empty arrays

### `minimum_should_match` defaults

| Case | Default |
|------|---------|
| `should` only (no `must`, no `filter`) | **1** |
| `should` with `must` or `filter` | **0** (should is optional; filter/must still apply) |

Doc example (should + filter, no explicit `minimum_should_match`):

```java
HybridDsl.bool()
    .should(
        HybridDsl.match("title", "data"),
        HybridDsl.match("content", "python java"))
    .filter(HybridDsl.range("id").gte(5));
```

Require at least one should hit:

```java
.minimumShouldMatch(1)
```

---

## knn vector search

### Single path

```json
{
  "field": "vector_col",
  "k": 5,
  "query_vector": "[0.1, 0.2, 0.3, 0.4]",
  "filter": [{"range": {"id": {"gte": 5}}}],
  "boost": 0.7,
  "similarity": 0.5,
  "search_options": {
    "ef_search": 64,
    "refine_k": 4.0,
    "filter_mode": "pre"
  }
}
```

```java
HybridDsl.knn("vector_col", queryVector, 5)
    .filter(HybridDsl.range("id").gte(5))
    .boost(0.7)
    .efSearch(64)
    .filterMode(HybridDslKeys.FilterMode.PRE);

// filter as single bool object
HybridDsl.knn("vector_col", queryVector, 5)
    .filterBool(HybridDsl.bool().filter(HybridDsl.term("category_id", 1)));
```

**filter_mode**: `pre` | `pre-knn` | `pre-brute` | `post` | `post-index-merge`

### Multi-path

```java
.knn(
    HybridDsl.knn("vector_col", vectorA, 5),
    HybridDsl.knn("vector_col", vectorB, 5))
```

Filters on `query` and each `knn` path are **independent**.

---

## rank fusion

When both `query` and `knn` are present, specify fusion; if omitted, default is **`WEIGHT_SUM`**.

### RRF

```json
{"rank": {"rrf": {"rank_window_size": 10, "rank_constant": 60}}}
```

```java
.rank(HybridDsl.rrf(10, 60))
// shortcut
HybridDsl.textVectorRrf(textQuery, knnQuery, size, window, constant);
```

Set `boost` on `query` / `knn` for **WRRF** (weighted RRF).

### weighted_sum

```json
{"rank": {"weighted_sum": {"normalizer": "minmax", "rank_window_size": 10}}}
```

```java
.rank(HybridDsl.weightedSum(HybridDslKeys.Normalizer.MINMAX, 10))
.minScore(0.0)
```

| `normalizer` | Meaning |
|--------------|---------|
| `none` | No normalization |
| `minmax` | Scale scores to [0,1]; pairs with `min_score` |

---

## Pagination & min_score

```java
.from(0).size(10).minScore(0.5)
```

`min_score` may return fewer rows than `size`.

---

## HybridDslKeys reference

| Category | Constants | JSON keys |
|----------|-----------|-----------|
| Top-level | `QUERY` `KNN` `RANK` `FROM` `SIZE` `MIN_SCORE` | same |
| Full-text | `MATCH` `MULTI_MATCH` `MATCH_PHRASE` `QUERY_STRING` | same |
| Scalar | `TERM` `RANGE` `TERMS` | same |
| JSON | `JSON_CONTAINS` `JSON_OVERLAPS` `JSON_MEMBER_OF` | same |
| Array | `ARRAY_CONTAINS` `ARRAY_CONTAINS_ALL` `ARRAY_OVERLAPS` | same |
| bool | `BOOL` `MUST` `SHOULD` `FILTER` `MUST_NOT` | same |
| Params | `QUERY_PARAM`→`query` `FIELDS` `OPERATOR` `BOOST` … | snake_case |
| knn | `FIELD` `K` `QUERY_VECTOR` `SIMILARITY` `SEARCH_OPTIONS` | same |
| rank | `RRF` `WEIGHTED_SUM` `RANK_CONSTANT` `RANK_WINDOW_SIZE` `NORMALIZER` | same |
| Enums | `Operator.OR/AND` `Normalizer.MINMAX` `FilterMode.PRE` … | string literals |

---

## SDK classes & patterns

| Class | Role |
|-------|------|
| `HybridSearch` | Entry (`client.hybridSearch()`) |
| `HybridSearchCustomBuilder` | Chain builder + `buildDsl()` |
| `HybridSearchDsl` | Mutable document → `toJsonString()` |
| `HybridDsl` | Factory (`match`, `bool`, `knn`, `rrf`, …) |
| `HybridDslNode` | Generic keyword node |
| `HybridDslKnn` / `HybridDslRank` | `knn` / `rank` sections |

`Filter` objects convert via `FilterMapper` — see [04-filter](04-filter.md).

---

## Constraints & best practices

1. Top-level sub-queries: `query` (1) + `knn` paths ≤ **3** total
2. Scalar conditions in `filter`, not `must`/`should`
3. `query.filter` ≠ `knn.filter` — declare both when needed
4. `query` + `knn` without `rank` → `WEIGHT_SUM`; fused results may include knn-only hits
5. Prefer string form for `query_vector`: `"[0.1,0.2]"`
6. Not supported: `_source`, `rank_feature`, `es_mode`

---

## End-to-end examples

Aligned with `HybridSearchDocIT`:

```java
// knn only
.knn(HybridDsl.knn("vector_col", queryVector, 5)).size(5)

// knn + range filter
.knn(HybridDsl.knn("vector_col", queryVector, 5)
    .filter(HybridDsl.range("id").gte(5)))

// full-text + bool filter
.query(HybridDsl.bool()
    .must(HybridDsl.match("content", "Python JavaScript"))
    .filter(HybridDsl.range("id").gte(5)))

// text + filter + knn (default WEIGHT_SUM)
.query(HybridDsl.bool()
    .must(HybridDsl.match("content", "Python JavaScript"))
    .filter(HybridDsl.range("id").gte(3), HybridDsl.range("id").lte(10)))
.knn(HybridDsl.knn("vector_col", queryVector, 10))

// text + vector + RRF
HybridDsl.textVectorRrf(
    HybridDsl.match("content", "python javascript"),
    HybridDsl.knn("vector_col", queryVector, 5),
    6, 10, 60)

// JSON term on dotted field
.query(HybridDsl.bool().filter(HybridDsl.term("doc_json.name", "doc2")))

// array_contains
.query(HybridDsl.bool().filter(HybridDsl.arrayContains("tags_array1", "ios")))
```

---

## Debugging & tests

```java
String json = client.hybridSearch().customSearch()
    .table("t")
    .query(HybridDsl.match("content", "test"))
    .buildDsl();
```

Remote IT (spec-aligned):

```bash
OCEANBASE_REMOTE_IT=1 OCEANBASE_HOST=... OCEANBASE_PORT=... \
OCEANBASE_USER=... OCEANBASE_PASSWORD=... OCEANBASE_DATABASE=... \
mvn test -Dtest=HybridSearchDocIT
```

---

## Related docs

- [← 03-hybrid-search](03-hybrid-search.md)
- [04-filter](04-filter.md)
- [05-HYBRID_SEARCH-DSL语法 (中文)](../zh/05-HYBRID_SEARCH-DSL语法.md)
- [OceanBase HYBRID_SEARCH](https://www.oceanbase.com/docs/common-oceanbase-database-cn-1000000005682104)
