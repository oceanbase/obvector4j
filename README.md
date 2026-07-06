# obvector4j

Java SDK for OceanBase vector store: vector table CRUD, ANN search, hybrid search (full-text + vector / scalar + vector), custom SQL and HYBRID_SEARCH DSL, JSON virtual tables.

**Requirements**: OceanBase MySQL mode + JDBC driver `oceanbase-client`. Native hybrid search SQL requires **≥ 4.6.0** (`client.supportsHybridSearchSql()`).

---

## Documentation

Start with **[Getting Started](docs/en/01-getting-started.md)** and read **01 → 04** in order. Chinese docs are in [docs/zh/](docs/zh/00-文档目录.md) (same topics, mirrored numbering).

| Step | English | 中文 |
|:----:|---------|------|
| 0 | — | [文档目录](docs/zh/00-文档目录.md) |
| 1 | [Getting Started](docs/en/01-getting-started.md) | [快速入门](docs/zh/01-快速入门.md) |
| 2 | [Architecture](docs/en/02-architecture.md) | [项目与架构](docs/zh/02-项目与架构.md) |
| 3 | [Hybrid Search](docs/en/03-hybrid-search.md) | [混合搜索](docs/zh/03-混合搜索.md) |
| 5 | [HYBRID_SEARCH DSL](docs/en/05-hybrid-search-dsl.md) | [DSL 语法](docs/zh/05-HYBRID_SEARCH-DSL语法.md) |
| 4 | [Filter API](docs/en/04-filter.md) | [Filter 过滤](docs/zh/04-Filter过滤.md) |

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

Provide credentials via environment variables or a secrets manager — **do not hardcode or commit them**:

```java
// OCEANBASE_URI=jdbc:oceanbase://<host>:<port>/<database>
String uri = System.getenv("OCEANBASE_URI");
String user = System.getenv("OCEANBASE_USER");
String password = System.getenv("OCEANBASE_PASSWORD");

ObVecClient client = new ObVecClient(uri, user, password);
```

## Quick example

```java
import com.oceanbase.obvector4j.ObVecClient;
import com.oceanbase.obvector4j.filter.FilterBuilder;

ObVecClient client = new ObVecClient(uri, user, password);

// Scalar + vector
client.scalarVectorSearch()
    .table("products")
    .queryVector(queryVector)
    .filter(FilterBuilder.key("category_id").isEqualTo(1))
    .topk(10)
    .outputFields("id", "name")
    .search();

// Custom HYBRID_SEARCH DSL (4.6.0+) — throws if cluster < 4.6.0
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

See [Getting Started](docs/en/01-getting-started.md), [Hybrid Search](docs/en/03-hybrid-search.md), and [DSL Syntax](docs/en/05-hybrid-search-dsl.md) for more.

---

## Project layout

```
src/main/java/com/oceanbase/obvector4j/
├── ObVecClient.java              # entry point
├── schema/ model/ hybrid/        # schema, values, hybrid search
├── filter/ version/ json_table/ util/
docs/en/ … docs/zh/               # mirrored EN / CN docs
```

---

## Tests

```bash
mvn test                    # unit tests (default, no database)
mvn test -Pintegration      # Testcontainers integration tests
mvn test -Premote-it        # remote cluster (env vars below)
```

Remote integration tests require your own OceanBase cluster:

```bash
export OCEANBASE_REMOTE_IT=1
export OCEANBASE_HOST=<your-host>
export OCEANBASE_PORT=<your-port>
export OCEANBASE_USER=<your-user>
export OCEANBASE_PASSWORD=<your-password>
export OCEANBASE_DATABASE=<your-database>
# or: export OCEANBASE_URI=jdbc:oceanbase://<host>:<port>/<database>

mvn test -Premote-it
```

---

## License

Mulan Permissive Software License v2
