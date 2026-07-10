# obvector4j

Java SDK for OceanBase vector store: vector table CRUD, ANN search, hybrid search (full-text + vector / scalar + vector), custom SQL and HYBRID_SEARCH DSL, JSON virtual tables.

**Requirements**: OceanBase MySQL mode + JDBC driver `oceanbase-client`. Native hybrid search SQL requires **≥ 4.6.0** (`client.supportsHybridSearchSql()`).

---

## Documentation

- English: [docs/en/](docs/en/)
- 中文: [docs/zh/](docs/zh/)

---

## Installation

```xml
<dependency>
  <groupId>com.oceanbase</groupId>
  <artifactId>obvector4j</artifactId>
  <version>1.0.0</version>
</dependency>
```

```bash
git clone https://github.com/oceanbase/obvector4j.git
cd obvector4j
make build
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

See [docs/en/](docs/en/) for full documentation.

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
make unit-test              # unit tests (default, no database)
make test                   # Testcontainers integration tests
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
