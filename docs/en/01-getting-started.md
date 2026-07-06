# 01 · Getting Started

> Read in order: **01 → 02 → 03 → 04** · 中文：[01-快速入门](../zh/01-快速入门.md)

---

## Table of Contents

1. [Installation](#installation)
2. [Connect](#connect)
3. [Create table & insert](#create-table--insert)
4. [Hybrid search (common)](#hybrid-search-common)
5. [Custom SQL & DSL](#custom-sql--dsl)
6. [Next](#next)

---

## Installation

```xml
<dependency>
  <groupId>com.oceanbase</groupId>
  <artifactId>obvector4j</artifactId>
  <version>1.0.7</version>
</dependency>
```

```bash
git clone https://github.com/oceanbase/obvector4j.git
cd obvector4j
mvn install
```

## Connect

```java
import com.oceanbase.obvector4j.ObVecClient;

String uri = System.getenv("OCEANBASE_URI");       // jdbc:oceanbase://<host>:<port>/<database>
String user = System.getenv("OCEANBASE_USER");
String password = System.getenv("OCEANBASE_PASSWORD");

ObVecClient client = new ObVecClient(uri, user, password);
```

## Create table & insert

See [02-architecture](02-architecture.md) for schema types. Minimal example:

```java
import com.oceanbase.obvector4j.schema.*;
import com.oceanbase.obvector4j.model.*;

// ... build ObCollectionSchema with FLOAT_VECTOR column ...
client.createCollection("my_table", schema);
client.insert("my_table", new String[] {"c2"}, rows);
```

## Hybrid search (common)

```java
import com.oceanbase.obvector4j.filter.*;

// Scalar + vector
Filter filter = FilterBuilder.key("category_id").isEqualTo(1);
client.scalarVectorSearch()
    .table("products")
    .queryVector(queryVector)
    .filter(filter)
    .topk(10)
    .outputFields("id", "name")
    .search();

// Full-text + vector
client.textVectorSearch()
    .table("documents")
    .queryVector(queryVector)
    .textFields("title", "content")
    .textQuery("keyword")
    .topk(10)
    .search();
```

See [03-hybrid-search](03-hybrid-search.md) and [04-filter](04-filter.md) for details.

## Custom SQL & DSL

### Custom SQL

```java
client.executeSql("DELETE FROM my_table WHERE id = 1");
ArrayList<HashMap<String, Sqlizable>> rows = client.querySql("SELECT c1, c2 FROM my_table LIMIT 5");
```

### Custom HYBRID_SEARCH DSL (4.6.0+, throws below 4.6.0)

```java
import com.oceanbase.obvector4j.hybrid.core.dsl.HybridDsl;

client.hybridSearch()
    .customSearch()
    .table("documents")
    .query(HybridDsl.match("content", "keyword"))
    .knn(HybridDsl.knn("embedding", queryVector, 10))
    .rank(HybridDsl.rrf(10, 60))
    .size(10)
    .outputFields("id", "title")
    .search();
```

See [05-hybrid-search-dsl](05-hybrid-search-dsl.md) for full DSL grammar and examples. Overview: [03-hybrid-search](03-hybrid-search.md), filters: [04-filter](04-filter.md).

## Next

| Doc | Topic |
|-----|-------|
| [02-architecture](02-architecture.md) | Packages, API cheat sheet, `hybrid.dsl` |
| [03-hybrid-search](03-hybrid-search.md) | HYBRID_SEARCH overview, compatibility |
| [05-hybrid-search-dsl](05-hybrid-search-dsl.md) | DSL grammar, bool/knn/rank, examples |
| [04-filter](04-filter.md) | Filter DSL full reference |
