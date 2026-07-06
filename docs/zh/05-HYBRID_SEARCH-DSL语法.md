# 05 · HYBRID_SEARCH DSL 语法与示例

> 阅读顺序：[00-文档目录](00-文档目录.md) → … → [03-混合搜索](03-混合搜索.md) → **05 DSL 语法** → [04-Filter过滤](04-Filter过滤.md)  
> English: [05-hybrid-search-dsl](../en/05-hybrid-search-dsl.md)

本文专门介绍 OceanBase **4.6.0+** `HYBRID_SEARCH` 的 **JSON DSL 语法规则**、语义约束，以及 obvector4j **`hybrid.core`** 包中的 Java 构建方式。混合搜索概览与版本说明见 [03-混合搜索](03-混合搜索.md)。

---

## 目录

1. [前置条件](#前置条件)
2. [SQL 外壳与 DSL 文档结构](#sql-外壳与-dsl-文档结构)
3. [三种使用方式](#三种使用方式)
4. [顶层字段](#顶层字段)
5. [query 表达式](#query-表达式)
6. [bool 组合查询](#bool-组合查询)
7. [knn 向量检索](#knn-向量检索)
8. [rank 融合排序](#rank-融合排序)
9. [分页与分数过滤](#分页与分数过滤)
10. [HybridDslKeys 关键字速查](#hybriddslkeys-关键字速查)
11. [SDK 类与封装模式](#sdk-类与封装模式)
12. [语法约束与最佳实践](#语法约束与最佳实践)
13. [完整场景示例](#完整场景示例)
14. [调试与测试](#调试与测试)
15. [相关文档](#相关文档)

---

## 前置条件

| 项目 | 要求 |
|------|------|
| OceanBase 版本 | **≥ 4.6.0**（`client.supportsHybridSearchSql()` 为 `true`） |
| 表类型 | 堆表 `ORGANIZATION HEAP WITH COLUMN GROUP(all columns)` |
| 向量列 | 须建 HNSW 向量索引 |
| 全文列 | 须建全文索引；`match_phrase` 列需 `FTS_INDEX_TYPE=PHRASE_MATCH` |
| JSON/Array 过滤 | 可选 SEARCH INDEX，建议创建以提升性能 |
| SDK 入口 | `client.hybridSearch()`（低于 4.6.0 抛 `UnsupportedOperationException`） |

**包路径**：`com.oceanbase.obvector4j.hybrid.core`（DSL 关键字在 `.core.dsl` 子包）

---

## SQL 外壳与 DSL 文档结构

### SQL 形式

```sql
SELECT col1, col2, ...
FROM HYBRID_SEARCH(TABLE table_name, 'dsl_json_string') [alias];
```

- `table_name`：目标堆表，支持分区表
- `dsl_json_string`：描述检索与排序的 JSON 字符串
- **不能在** `HYBRID_SEARCH` 同层写 `WHERE` / `ORDER BY` / `LIMIT`，须包一层子查询：

```sql
-- ❌ 不支持
SELECT id FROM HYBRID_SEARCH(TABLE t, '{...}') WHERE id > 3;

-- ✅ 支持
SELECT id FROM (
  SELECT id FROM HYBRID_SEARCH(TABLE t, '{...}')
) sub WHERE id > 3;
```

### DSL 文档骨架

```json
{
  "query": { /* 可选：全文/标量/组合 */ },
  "knn":   { /* 可选：单路向量 */ },
  "rank":  { /* 可选：融合策略 */ },
  "from":  0,
  "size":  10,
  "min_score": 0.5
}
```

`query` 与 `knn` **至少填一个**；多路向量时 `knn` 为**数组**。

---

## 三种使用方式

### 1. 原生 SQL（直接写 JSON）

```sql
SELECT id, title
FROM HYBRID_SEARCH(TABLE documents, '{
  "query": {"match": {"content": "OceanBase"}},
  "knn": {"field": "embedding", "k": 10, "query_vector": "[0.1,0.2,0.3]"},
  "rank": {"rrf": {"rank_window_size": 10, "rank_constant": 60}},
  "size": 5
}');
```

### 2. SDK：`hybridSearchWithDsl`（传入完整 JSON）

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

### 3. SDK：`customSearch()` 链式 Builder（推荐）

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

调试时可调用 `.buildDsl()` 查看最终 JSON，无需执行查询。

**兜底**：特殊语法用 `queryDsl()` / `knnDsl()` / `rankDsl()` 传入原始 JSON 片段。

---

## 顶层字段

| JSON 键 | Java 常量 | 必填 | 说明 |
|---------|-----------|:----:|------|
| `query` | `HybridDslKeys.QUERY` | 二选一 | 全文/标量/组合查询 |
| `knn` | `HybridDslKeys.KNN` | 二选一 | 向量检索；对象=单路，数组=多路 |
| `rank` | `HybridDslKeys.RANK` | 否 | RRF / 加权求和融合 |
| `from` | `HybridDslKeys.FROM` | 否 | 分页起始（从 0 起），须与 `size` 配合 |
| `size` | `HybridDslKeys.SIZE` | 否 | 返回条数，默认 10 |
| `min_score` | `HybridDslKeys.MIN_SCORE` | 否 | 最低相关性分（`__score` 阈值） |

---

## query 表达式

### 表达式类型一览

```
query = bool
      | match | multi_match | match_phrase | query_string   // 全文
      | term | range | terms                                 // 标量
      | json_contains | json_overlaps | json_member_of       // JSON
      | array_contains | array_contains_all | array_overlaps // Array
```

### 全文检索

#### match — 单字段词项匹配

**简单形式**（推荐）：

```json
{"match": {"content": "Python JavaScript"}}
```

**带参数形式**：

```json
{
  "match": {
    "content": {
      "query": "Python JavaScript",
      "operator": "OR",
      "minimum_should_match": 1,
      "boost": 1.2
    }
  }
}
```

| 参数 | 说明 |
|------|------|
| `query` | 查询文本（必填） |
| `operator` | `OR`（默认）/ `AND` |
| `minimum_should_match` | 多词最小匹配数，非负整数 |
| `boost` | 权重，≥ 0 |

```java
// 简单
HybridDsl.match("content", "Python JavaScript")

// 带 boost
HybridDsl.match("content")
    .query("python javascript")
    .param(HybridDslKeys.BOOST, 0.3)
    .operator(HybridDslKeys.Operator.OR)
```

#### multi_match — 多字段全文

```json
{
  "multi_match": {
    "fields": ["title^0.3", "content^0.2"],
    "query": "OceanBase database",
    "type": "best_fields",
    "operator": "OR",
    "minimum_should_match": 1,
    "boost": 0.6
  }
}
```

```java
HybridDsl.multiMatch(new String[] {"title", "content"}, "OceanBase database");

// 或分步设置
HybridDsl.multiMatch(new String[] {"title^0.3", "content"})
    .param(HybridDslKeys.QUERY_PARAM, "keyword")
    .type(HybridDslKeys.MultiMatchType.BEST_FIELDS);
```

#### match_phrase — 短语匹配

```json
{
  "match_phrase": {
    "content": {
      "query": "machine learning",
      "slop": 2
    }
  }
}
```

```java
HybridDsl.matchPhrase("content", "machine learning", 2);
```

#### query_string — 查询串语法

```json
{
  "query_string": {
    "fields": ["title^0.3", "author^0.2"],
    "query": "Gatsby^0.2 Dream",
    "type": "best_fields",
    "default_operator": "OR",
    "minimum_should_match": 2,
    "boost": 0.6
  }
}
```

```java
HybridDsl.queryString(new String[] {"title^0.3", "content"}, "OceanBase AND vector");
```

### 标量检索

> **重要**：`term` / `range` / `terms` **不参与算分**，应放在 `bool.filter` 或 `knn.filter` 中，**不要**放在 `bool.must` / `bool.should` 里。

#### term — 等值

```json
{"term": {"status": 1}}
// 或
{"term": {"status": {"value": 1}}}
```

```java
HybridDsl.term("status", 1);
HybridDsl.term("doc_json.name", "doc2");  // JSON 点路径
```

#### range — 范围

```json
{"range": {"id": {"gte": 3, "lte": 10}}}
```

边界：`gte` / `gt` / `lte` / `lt`，至少填一个，同边界不可重复。

```java
HybridDsl.range("id").gte(3).lte(10);
HybridDsl.range("price").gte(50).lte(250);
```

#### terms — 多值等值

```json
{"terms": {"category_id": [1, 2, 3]}}
```

```java
HybridDsl.terms("category_id", 1, 2, 3);
```

### JSON 检索

#### json_contains

```json
{
  "json_contains": {
    "doc_json": {
      "candidate": "{\"name\": \"doc2\"}",
      "path": "$"
    }
  }
}
```

```java
HybridDsl.jsonContains("doc_json", "{\"name\":\"doc2\"}", "$");
```

#### json_overlaps

```json
{
  "json_overlaps": {
    "doc_json": {
      "candidate": "[\"database\", \"mysql\"]",
      "path": "$.tags"
    }
  }
}
```

```java
HybridDsl.jsonOverlaps("doc_json", "[\"database\",\"mysql\"]", "$.tags");
```

#### json_member_of

```java
HybridDsl.jsonMemberOf("doc_json", "\"database\"", "$.tags");
```

### Array 检索

```json
{"array_contains": {"tags_array1": "ios"}}
{"array_contains_all": {"tags_array1": ["database", "oceanbase"]}}
{"array_overlaps": {"tags_array1": ["web", "mobile"]}}
```

```java
HybridDsl.arrayContains("tags_array1", "ios");
HybridDsl.arrayContainsAll("tags_array1", "database", "oceanbase");
HybridDsl.arrayOverlaps("tags_array1", "web", "mobile");
```

---

## bool 组合查询

### 结构

```json
{
  "query": {
    "bool": {
      "must":     [ /* 必须匹配，参与算分 */ ],
      "should":   [ /* 可选匹配，参与算分 */ ],
      "filter":   [ /* 必须满足，不算分 */ ],
      "must_not": [ /* 必须不匹配，不算分 */ ],
      "minimum_should_match": 1,
      "boost": 1.6
    }
  }
}
```

| 子句 | 逻辑 | 算分 | 说明 |
|------|------|:----:|------|
| `must` | AND | ✅ | 全部子句须匹配 |
| `should` | OR（受 `minimum_should_match` 约束） | ✅ | 至少匹配 N 条 |
| `filter` | AND | ❌ | 过滤，不影响相关性分 |
| `must_not` | NOT | ❌ | 排除匹配项 |

**规则摘要**：

1. `must` / `should` / `filter` 之间是 **AND** 关系
2. bool 中至少包含 **1 个正向条件**（`must` / `should` / `filter`），不能只有 `must_not`
3. 标量条件（`term` / `range` / `terms` / json / array）应放 **`filter`**，不要放 `must` / `should`
4. 子句值不可为空数组

### minimum_should_match 默认值

| 场景 | 默认值 |
|------|--------|
| 仅有 `should`（无 `must`、无 `filter`） | **1** |
| `should` 与 `must` 或 `filter` 共存 | **0**（should 可选，filter/must 仍生效） |

示例（文档标准用法 — should + filter，不设 `minimum_should_match`）：

```json
{
  "query": {
    "bool": {
      "should": [
        {"match": {"title": "data"}},
        {"match": {"content": "python java"}}
      ],
      "filter": [
        {"range": {"id": {"gte": 5}}}
      ]
    }
  }
}
```

此时 `minimum_should_match` 默认为 **0**：只要满足 `filter`，即使 should 全不匹配也会返回（相当于 filter 驱动召回）。

显式要求 should 至少命中 1 条：

```java
HybridDsl.bool()
    .should(
        HybridDsl.match("title", "data"),
        HybridDsl.match("content", "python java"))
    .filter(HybridDsl.range("id").gte(5))
    .minimumShouldMatch(1);
```

### 典型组合

**全文 + 标量过滤**：

```java
HybridDsl.bool()
    .must(HybridDsl.match("content", "Python JavaScript"))
    .filter(HybridDsl.range("id").gte(5));
```

**仅过滤（不算分）**：

```java
HybridDsl.bool()
    .filter(
        HybridDsl.term("doc_json.name", "doc2"),
        HybridDsl.arrayContains("tags_array1", "ios"));
```

**嵌套 bool**（filter 内嵌套 must_not 等）：

```java
HybridDsl.bool()
    .filter(
        HybridDsl.range("id").gte(3),
        HybridDsl.bool()
            .mustNot(HybridDsl.term("status", 0)));
```

---

## knn 向量检索

### 单路 knn

```json
{
  "knn": {
    "field": "vector_col",
    "k": 5,
    "query_vector": "[0.1, 0.2, 0.3, 0.4]",
    "similarity": 0.5,
    "boost": 0.7,
    "filter": [
      {"range": {"id": {"gte": 5}}}
    ],
    "search_options": {
      "ef_search": 64,
      "refine_k": 4.0,
      "filter_mode": "pre"
    }
  }
}
```

| 字段 | Java 常量 | 说明 |
|------|-----------|------|
| `field` | `FIELD` | 向量列名（必填） |
| `k` | `K` | 该路 TopK（必填） |
| `query_vector` | `QUERY_VECTOR` | 查询向量，**推荐字符串** `"[1.0,2.0]"` |
| `filter` | `FILTER` | 该路标量过滤；数组或单个 `bool` 对象 |
| `similarity` | `SIMILARITY` | 相似度阈值 [0,1]，可能导致返回数 < k |
| `boost` | `BOOST` | 路权重（WRRF / weighted_sum 时使用） |
| `search_options` | `SEARCH_OPTIONS` | 见下表 |

**search_options.filter_mode**：

| 值 | 含义 |
|----|------|
| `pre` | 前过滤自适应（默认） |
| `pre-knn` | 前过滤 + knn |
| `pre-brute` | 前过滤 + 暴搜 |
| `post` | 表达式迭代式后过滤 |
| `post-index-merge` | index-merge 框架迭代过滤 |

```java
HybridDsl.knn("vector_col", queryVector, 5)
    .filter(HybridDsl.range("id").gte(5))
    .boost(0.7)
    .similarity(0.5)
    .efSearch(64)
    .filterMode(HybridDslKeys.FilterMode.PRE);

// filter 为单个 bool 对象
HybridDsl.knn("vector_col", queryVector, 5)
    .filterBool(HybridDsl.bool()
        .filter(HybridDsl.term("category_id", 1)));
```

### 多路 knn

```json
{
  "knn": [
    {"field": "vector_col", "k": 5, "query_vector": "[0.1,0.2,0.3,0.4]"},
    {"field": "vector_col", "k": 5, "query_vector": "[0.4,0.3,0.2,0.1]"}
  ],
  "size": 7
}
```

```java
client.hybridSearch().customSearch()
    .table("documents")
    .knn(
        HybridDsl.knn("vector_col", vectorA, 5),
        HybridDsl.knn("vector_col", vectorB, 5))
    .size(7)
    .outputFields("id", "title")
    .search();
```

### 纯向量 + 过滤（仅 knn，无 query）

```java
client.hybridSearch().customSearch()
    .table("products")
    .knn(HybridDsl.knn("embedding", queryVector, 5)
        .filter(
            HybridDsl.term("category_id", 1),
            HybridDsl.range("price").gte(50).lte(250)))
    .size(5)
    .outputFields("id", "price")
    .search();
```

> **注意**：`query` 与各 `knn` 路的 `filter` **相互独立**。全文侧的 filter 不会自动作用于 knn 路，反之亦然。

---

## rank 融合排序

当同时存在 `query`（全文）与 `knn`（向量）时，需要指定融合策略；若省略 `rank`，全文+向量默认走 **`WEIGHT_SUM`**（加权求和）。

### RRF（互惠排名融合）

```json
{
  "rank": {
    "rrf": {
      "rank_window_size": 10,
      "rank_constant": 60
    }
  }
}
```

| 字段 | 说明 |
|------|------|
| `rank_window_size` | 每路参与融合的窗口；未指定 `size` 时可作返回条数参考 |
| `rank_constant` | RRF 平滑常数 K，默认 60 |

在 `query` / `knn` 上设置 `boost` 即为 **WRRF**（带权 RRF）：

```java
client.hybridSearch().customSearch()
    .table("documents")
    .query(HybridDsl.match("content")
        .query("python javascript")
        .param(HybridDslKeys.BOOST, 0.3))
    .knn(HybridDsl.knn("vector_col", queryVector, 5).boost(0.7))
    .rank(HybridDsl.rrf(10, 60))
    .size(6)
    .outputFields("id", "content")
    .search();
```

快捷工厂：

```java
HybridDsl.textVectorRrf(
    HybridDsl.match("content", "python javascript"),
    HybridDsl.knn("vector_col", queryVector, 5),
    6,   // size
    10,  // rank_window_size
    60); // rank_constant
```

### weighted_sum（加权求和 + 归一化）

```json
{
  "rank": {
    "weighted_sum": {
      "normalizer": "minmax",
      "rank_window_size": 10
    }
  }
}
```

| `normalizer` | 说明 |
|--------------|------|
| `none` | 不归一化，直接按 boost 加权 |
| `minmax` | min-max 归一化到 [0,1]，可与 `min_score` 配合 |

```java
.rank(HybridDsl.weightedSum(HybridDslKeys.Normalizer.MINMAX, 10))
.minScore(0.0)

// 快捷
HybridDsl.textVectorWeightedSum(
    HybridDsl.match("content", "keyword"),
    HybridDsl.knn("vector_col", queryVector, 5),
    5, 10);
```

---

## 分页与分数过滤

```java
client.hybridSearch().customSearch()
    .table("documents")
    .query(HybridDsl.match("content", "keyword"))
    .from(0)
    .size(10)
    .minScore(0.5)
    .outputFields("id")
    .search();
```

对应 JSON：

```json
{
  "query": {"match": {"content": "keyword"}},
  "from": 0,
  "size": 10,
  "min_score": 0.5
}
```

`min_score` 过滤 `__score`，可能导致实际返回条数少于 `size`。

---

## HybridDslKeys 关键字速查

| 分类 | Java 常量 | JSON 键 |
|------|-----------|---------|
| **顶层** | `QUERY` `KNN` `RANK` `FROM` `SIZE` `MIN_SCORE` | 同左 |
| **全文** | `MATCH` `MULTI_MATCH` `MATCH_PHRASE` `QUERY_STRING` | 同左 |
| **标量** | `TERM` `RANGE` `TERMS` | 同左 |
| **JSON** | `JSON_CONTAINS` `JSON_OVERLAPS` `JSON_MEMBER_OF` | 同左 |
| **Array** | `ARRAY_CONTAINS` `ARRAY_CONTAINS_ALL` `ARRAY_OVERLAPS` | 同左 |
| **bool** | `BOOL` `MUST` `SHOULD` `FILTER` `MUST_NOT` | 同左 |
| **通用参数** | `QUERY_PARAM`→`query` `FIELDS` `OPERATOR` `BOOST` `TYPE` `SLOP` `MINIMUM_SHOULD_MATCH` | snake_case |
| **range** | `GTE` `GT` `LTE` `LT` | 同左 |
| **JSON 参数** | `CANDIDATE` `PATH` | 同左 |
| **knn** | `FIELD` `K` `QUERY_VECTOR` `SIMILARITY` `SEARCH_OPTIONS` | 同左 |
| **search_options** | `EF_SEARCH` `REFINE_K` `FILTER_MODE` | 同左 |
| **rank** | `RRF` `WEIGHTED_SUM` `RANK_CONSTANT` `RANK_WINDOW_SIZE` `NORMALIZER` | 同左 |
| **枚举** | `Operator.OR/AND` `Normalizer.MINMAX/NONE` `FilterMode.PRE/...` | 字符串字面量 |

---

## SDK 类与封装模式

| 类 | 作用 |
|----|------|
| `HybridSearch` | 4.6.0+ 入口（`client.hybridSearch()`） |
| `HybridSearchSupport` | 版本校验 `require(version)` |
| `HybridSearchCustomBuilder` | 链式构建 + 执行；`buildDsl()` 预览 JSON |
| `HybridSearchDsl` | 可变 DSL 文档，`toJsonString()` |
| `HybridDsl` | 常用表达式工厂（`match` / `bool` / `knn` / `rrf` 等） |
| `HybridDslNode` | 通用节点，支持任意关键词 + 链式 `param()` |
| `HybridDslKnn` | `knn` 段 |
| `HybridDslRank` | `rank` 段 |
| `HybridDslExpr` | 表达式接口（`toJson()`） |

### JSON 封装模式

| 模式 | Java API | 生成形状 |
|------|----------|----------|
| 字段表达式 | `HybridDslNode.field(KEY, field, value)` | `{KEY: {field: value}}` |
| 字段 + 参数 | `HybridDslNode.field(KEY, field).param(...)` | `{KEY: {field: {param:…}}}` |
| 对象体 | `HybridDslNode.of(KEY).param(...)` | `{KEY: {param:…}}` |
| bool | `HybridDslNode.bool().must(...).filter(...)` | `{bool: {must:[…], filter:[…]}}` |
| range | `HybridDslNode.range(field).gte(n)` | `{range: {field: {gte:n}}}` |
| JSON 操作 | `HybridDslNode.jsonOp(KEY, field, candidate, path)` | `{KEY: {field: {candidate, path}}}` |

### Filter 对象转 DSL

`HybridDslKnn.filter(Filter)` 与 `HybridDslNode.filter(Filter)` 通过 `FilterMapper` 将 [FilterBuilder](04-Filter过滤.md) 转为 `term` / `range` / `bool` 条件列表。

---

## 语法约束与最佳实践

1. **子查询数量**：顶层 `query`（算 1 个）+ `knn`（对象算 1 个，数组每个元素各算 1 个）合计 **≤ 3**
2. **过滤位置**：标量/json/array 放 `filter`；全文放 `must` / `should`
3. **filter 独立**：`query` 与各 `knn` 的 filter 互不影响；混合场景需在两侧分别声明
4. **融合结果**：`query` + `knn` 无 `rank` 时默认 `WEIGHT_SUM`，融合结果可能包含仅命中 knn 路的行
5. **query_vector**：SDK 自动格式化为字符串 `"[0.1,0.2,...]"`
6. **不支持**：`_source`、`rank_feature`、`es_mode`
7. **索引**：向量路须向量索引；全文路须全文索引

---

## 完整场景示例

以下示例与集成测试 `HybridSearchDocIT` 对齐，可在 **4.6.0+** 集群上直接验证。

### 单路 knn

```java
client.hybridSearch().customSearch()
    .table("doc_table")
    .knn(HybridDsl.knn("vector_col", queryVector, 5))
    .size(5)
    .outputFields("id", "title")
    .search();
```

### knn + range 过滤

```java
.knn(HybridDsl.knn("vector_col", queryVector, 5)
    .filter(HybridDsl.range("id").gte(5)))
```

### 全文 match

```java
.query(HybridDsl.match("content", "Python JavaScript"))
```

### 全文 + bool filter

```java
.query(HybridDsl.bool()
    .must(HybridDsl.match("content", "Python JavaScript"))
    .filter(HybridDsl.range("id").gte(5)))
```

### 全文 + filter + knn 混合（默认 WEIGHT_SUM）

```java
.query(HybridDsl.bool()
    .must(HybridDsl.match("content", "Python JavaScript"))
    .filter(
        HybridDsl.range("id").gte(3),
        HybridDsl.range("id").lte(10)))
.knn(HybridDsl.knn("vector_col", queryVector, 10))
.size(10)
```

### 全文 + 向量 + RRF

```java
String dsl = HybridDsl.textVectorRrf(
    HybridDsl.match("content", "python javascript"),
    HybridDsl.knn("vector_col", queryVector, 5),
    6, 10, 60).toJsonString();

client.hybridSearch().searchWithDsl("doc_table", dsl,
    new String[] {"id", "title"}, null);
```

### JSON 点路径 term

```java
.query(HybridDsl.bool()
    .filter(HybridDsl.term("doc_json.name", "doc2")))
```

### array_contains

```java
.query(HybridDsl.bool()
    .filter(HybridDsl.arrayContains("tags_array1", "ios")))
```

---

## 调试与测试

**预览 DSL JSON**：

```java
String json = client.hybridSearch()
    .customSearch()
    .table("t")
    .query(HybridDsl.match("content", "test"))
    .knn(HybridDsl.knn("vec", queryVector, 5))
    .buildDsl();
System.out.println(json);
```

**远程集成测试**（提测文档对齐）：

```bash
OCEANBASE_REMOTE_IT=1 \
OCEANBASE_HOST=<host> OCEANBASE_PORT=<port> \
OCEANBASE_USER=<user> OCEANBASE_PASSWORD=<pwd> OCEANBASE_DATABASE=<db> \
mvn test -Dtest=HybridSearchDocIT
```

测试类：`com.oceanbase.obvector4j.integration.remote.HybridSearchDocIT`

---

## 相关文档

- [← 03-混合搜索](03-混合搜索.md) — 概览、版本、兼容模式
- [04-Filter过滤](04-Filter过滤.md) — FilterBuilder 与 DSL 转换
- [02-项目与架构](02-项目与架构.md) — 包路径与 API 速查
- [05-hybrid-search-dsl (EN)](../en/05-hybrid-search-dsl.md)
- [OceanBase 官方 HYBRID_SEARCH](https://www.oceanbase.com/docs/common-oceanbase-database-cn-1000000005682104)
