# 03 · Hybrid Search

> Read in order: ← [02-architecture](02-architecture.md) → [04-filter](04-filter.md)  
> 中文：[03-混合搜索](../zh/03-混合搜索.md)

> Reference: [OceanBase HYBRID_SEARCH](https://www.oceanbase.com/docs/common-oceanbase-database-cn-1000000005682104)

## Table of Contents

1. [Overview](#overview)
2. [Quick start](#quick-start)
3. [Custom SQL & DSL entry](#custom-sql--dsl)
4. [Implementation paths](#implementation-paths)
5. [Constraints](#constraints)
6. [FAQ & remote tests](#faq--remote-tests)
7. [Related docs](#related-docs)

## Overview

Hybrid search combines multiple search techniques to provide more accurate and relevant results. OceanBase supports two types of hybrid search:

1. **Full-Text + Vector Hybrid Search**: Combines full-text search with vector similarity search
2. **Scalar + Vector Hybrid Search**: Combines scalar filtering with vector similarity search

Both types use Reciprocal Rank Fusion (RRF) to merge results from different search methods, providing a unified ranking of results.

### Version Requirements

| Feature | Minimum OceanBase Version | SDK Behavior |
|---------|---------------------------|--------------|
| `HYBRID_SEARCH` SQL interface | **4.6.0** | Auto-enabled when version ≥ 4.6.0 |
| Legacy hybrid search (client-side RRF) | 4.4.x+ | Used when version < 4.6.0 |

When connected to OceanBase 4.6.0+, the SDK automatically uses the native `HYBRID_SEARCH` SQL syntax. See [03-混合搜索 (中文)](../zh/03-混合搜索.md) for the Chinese guide.

```java
if (ob.supportsHybridSearchSql()) {
    // Uses: SELECT ... FROM HYBRID_SEARCH(TABLE ..., dsl_json)
} else {
    // Uses legacy client-side RRF or APPROXIMATE LIMIT vector search
}
```

## Quick start

Builder API is unchanged; the SDK picks the path by version.

### Scalar + vector (with Filter)

```java
import com.oceanbase.obvector4j.filter.FilterBuilder;

Filter filter = FilterBuilder.and(
    FilterBuilder.key("category_id").isEqualTo(1),
    FilterBuilder.key("price").isGreaterThanOrEqualTo(50.0));

client.scalarVectorSearch()
    .table("products")
    .queryVector(queryVector)
    .filter(filter)
    .topk(5)
    .outputFields("price", "category_id")
    .search();
```

### Full-text + vector (RRF)

```java
client.textVectorSearch()
    .table("documents")
    .queryVector(queryVector)
    .textFields("title", "content")
    .textQuery("OceanBase database")
    .topk(10)
    .rankWindowSize(20)
    .outputFields("title", "content")
    .search();
```

See [04-filter](04-filter.md) for Filter details.

## Custom SQL & DSL

> **Full DSL grammar, bool/knn/rank rules, and examples** → [05-hybrid-search-dsl](05-hybrid-search-dsl.md)

### Custom SQL

```java
client.executeSql("UPDATE products SET status = 1 WHERE id = 100");
ArrayList<HashMap<String, Sqlizable>> rows = client.querySql(
    "SELECT id, name, price FROM products WHERE category_id = 1 LIMIT 10");
```

### Custom HYBRID_SEARCH DSL (4.6.0+, throws below 4.6.0)

```java
import com.oceanbase.obvector4j.hybrid.core.dsl.HybridDsl;

client.hybridSearch()
    .customSearch()
    ...
    .search();
```

For `multi_match`, `query_string`, JSON/Array filters, `minimum_should_match` defaults, multi-path knn, WRRF, and more, see **[05 · HYBRID_SEARCH DSL Syntax](05-hybrid-search-dsl.md)**.

---

## Implementation paths

```
textVectorSearch() / scalarVectorSearch()
              │
              ▼
   supportsHybridSearchSql() ?
        /              \
      yes               no
       │                 │
       ▼                 ▼
 HYBRID_SEARCH      Legacy mode
 (4.6.0+ native)   · text+vector: dual query + client RRF
                    · scalar+vector: WHERE + APPROXIMATE LIMIT
```

## Constraints

Same as the [Chinese guide](../zh/03-混合搜索.md#使用限制oceanbase-侧): heap tables only, vector/full-text indexes required, no `WHERE`/`ORDER BY`/`LIMIT` on the same level as `HYBRID_SEARCH`.

## FAQ & remote tests

```java
ob.supportsHybridSearchSql();  // true = native HYBRID_SEARCH SQL
```

Remote IT: `HybridSearchRemoteIT` with `OCEANBASE_REMOTE_IT=1` and connection env vars. See [Chinese guide · 远程集成测试](../zh/03-混合搜索.md#远程集成测试).

## Related docs

- [← 02-architecture](02-architecture.md)
- [04-filter →](04-filter.md)
- [03-混合搜索 (中文)](../zh/03-混合搜索.md)
- [OceanBase HYBRID_SEARCH](https://www.oceanbase.com/docs/common-oceanbase-database-cn-1000000005682104)

---

<!-- Legacy detailed sections below kept for reference -->

## Full-Text + Vector Hybrid Search (detailed)

Full-text + vector hybrid search combines keyword-based full-text search with semantic vector search. This is ideal for scenarios where you need to search both by keywords and semantic similarity.

### Prerequisites

1. **Vector Index**: Create a vector index on the vector column
2. **Full-Text Index**: Create full-text indexes on text columns you want to search

### Setup

```java
import com.oceanbase.obvector4j.ObVecClient;
import com.oceanbase.obvector4j.schema.DataType;
import com.oceanbase.obvector4j.schema.ObCollectionSchema;
import com.oceanbase.obvector4j.schema.ObFieldSchema;
import com.oceanbase.obvector4j.schema.IndexParam;
import com.oceanbase.obvector4j.schema.IndexParams;

// Create client
ObVecClient ob = new ObVecClient(uri, user, password);

// Create table schema
ObCollectionSchema collectionSchema = new ObCollectionSchema();

// Vector field
ObFieldSchema vec_field = new ObFieldSchema("embedding", DataType.FLOAT_VECTOR);
vec_field.Dim(128).IsNullable(false);
collectionSchema.addField(vec_field);

// Text fields for full-text search
ObFieldSchema title_field = new ObFieldSchema("title", DataType.STRING);
title_field.IsNullable(false);
collectionSchema.addField(title_field);

ObFieldSchema content_field = new ObFieldSchema("content", DataType.STRING);
content_field.IsNullable(true);
collectionSchema.addField(content_field);

// Create vector index
IndexParams index_params = new IndexParams();
IndexParam index_param = new IndexParam("vidx_embedding", "embedding");
index_params.addIndex(index_param);
collectionSchema.setIndexParams(index_params);

ob.createCollection("documents", collectionSchema);

// Create full-text indexes
ob.createFulltextIndex("documents", "ft_title", "title");
ob.createFulltextIndex("documents", "ft_content", "content");
```

### Basic Usage

```java
import com.oceanbase.obvector4j.model.SqlVector;
import com.oceanbase.obvector4j.model.SqlText;
import java.util.ArrayList;
import java.util.HashMap;

// Insert data
ArrayList<Sqlizable[]> insert_rows = new ArrayList<>();
Sqlizable[] row1 = {
    new SqlVector(new float[]{...}), // 128-dimensional vector
    new SqlText("OceanBase Database Introduction"),
    new SqlText("OceanBase is a distributed relational database system")
};
insert_rows.add(row1);
ob.insert("documents", new String[]{"embedding", "title", "content"}, insert_rows);

// Perform hybrid search
float[] queryVector = new float[128]; // Your query vector

// Simplest way: outputFields defaults to textFields
ArrayList<HashMap<String, Sqlizable>> results = ob.textVectorSearch()
    .table("documents")
    .queryVector(queryVector)
    .textFields("title", "content")
    .textQuery("OceanBase database")
    .metric("cosine")
    .topk(10)
    .search();

// Process results
for (HashMap<String, Sqlizable> row : results) {
    String title = row.get("title").toString();
    String content = row.get("content").toString();
    // Process result...
}
```

### Advanced Usage with Filters

You can also add scalar filters to full-text + vector search:

```java
import com.oceanbase.obvector4j.filter.Filter;
import com.oceanbase.obvector4j.filter.FilterBuilder;

// Create filter
Filter filter = FilterBuilder.and(
    FilterBuilder.key("category_id").isEqualTo(1),
    FilterBuilder.key("price").isLessThanOrEqualTo(100.0)
);

// Hybrid search with filter
ArrayList<HashMap<String, Sqlizable>> results = ob.textVectorSearch()
    .table("documents")
    .queryVector(queryVector)
    .textFields("title", "content")
    .textQuery("OceanBase database")
    .filter(filter)  // Add scalar filter
    .metric("cosine")
    .topk(10)
    .outputFields("title", "content", "category_id", "price")
    .search();
```

### Specifying Output Fields

```java
// Auto-infer data types (recommended)
ArrayList<HashMap<String, Sqlizable>> results = ob.textVectorSearch()
    .table("documents")
    .queryVector(queryVector)
    .textFields("title", "content")
    .textQuery("database")
    .topk(10)
    .outputFields("title", "content")  // Auto-infer types
    .search();

// Explicitly specify data types
ArrayList<HashMap<String, Sqlizable>> results = ob.textVectorSearch()
    .table("documents")
    .queryVector(queryVector)
    .textFields("title", "content")
    .textQuery("database")
    .topk(10)
    .outputFields(
        new String[]{"title", "content"},
        new DataType[]{DataType.STRING, DataType.STRING}
    )
    .search();
```

## Scalar + Vector Hybrid Search

Scalar + vector hybrid search combines scalar filtering conditions with vector similarity search. This is ideal for scenarios where you need to filter by structured data (e.g., price, category, status) while also finding semantically similar vectors.

### Prerequisites

1. **Vector Index**: Create a vector index on the vector column
2. **Scalar Fields**: Define scalar fields in your table schema

### Setup

```java
import com.oceanbase.obvector4j.ObVecClient;
import com.oceanbase.obvector4j.schema.DataType;
import com.oceanbase.obvector4j.schema.ObCollectionSchema;
import com.oceanbase.obvector4j.schema.ObFieldSchema;
import com.oceanbase.obvector4j.schema.IndexParam;
import com.oceanbase.obvector4j.schema.IndexParams;

// Create client
ObVecClient ob = new ObVecClient(uri, user, password);

// Create table schema
ObCollectionSchema collectionSchema = new ObCollectionSchema();

// Vector field
ObFieldSchema vec_field = new ObFieldSchema("embedding", DataType.FLOAT_VECTOR);
vec_field.Dim(64).IsNullable(false);
collectionSchema.addField(vec_field);

// Scalar fields for filtering
ObFieldSchema price_field = new ObFieldSchema("price", DataType.DOUBLE);
price_field.IsNullable(false);
collectionSchema.addField(price_field);

ObFieldSchema category_field = new ObFieldSchema("category_id", DataType.INT32);
category_field.IsNullable(false);
collectionSchema.addField(category_field);

ObFieldSchema status_field = new ObFieldSchema("status", DataType.INT32);
status_field.IsNullable(false);
collectionSchema.addField(status_field);

// Create vector index
IndexParams index_params = new IndexParams();
IndexParam index_param = new IndexParam("vidx_embedding", "embedding");
index_params.addIndex(index_param);
collectionSchema.setIndexParams(index_params);

ob.createCollection("products", collectionSchema);
```

### Basic Usage with Filter API

Use the Filter API for type-safe filter construction:

```java
import com.oceanbase.obvector4j.model.SqlVector;
import com.oceanbase.obvector4j.model.SqlDouble;
import com.oceanbase.obvector4j.model.SqlInteger;
import com.oceanbase.obvector4j.filter.Filter;
import com.oceanbase.obvector4j.filter.FilterBuilder;
import java.util.ArrayList;
import java.util.HashMap;

// Insert data
ArrayList<Sqlizable[]> insert_rows = new ArrayList<>();
Sqlizable[] row1 = {
    new SqlVector(new float[]{...}), // 64-dimensional vector
    new SqlDouble(100.0),
    new SqlInteger(1),
    new SqlInteger(1)
};
insert_rows.add(row1);
ob.insert("products", new String[]{"embedding", "price", "category_id", "status"}, insert_rows);

// Create filter using Filter API
Filter filter = FilterBuilder.and(
    FilterBuilder.key("category_id").isEqualTo(1),
    FilterBuilder.and(
        FilterBuilder.key("price").isGreaterThanOrEqualTo(50.0),
        FilterBuilder.key("price").isLessThanOrEqualTo(250.0)
    ),
    FilterBuilder.key("status").isEqualTo(1)
);

// Perform hybrid search
float[] queryVector = new float[64]; // Your query vector

ArrayList<HashMap<String, Sqlizable>> results = ob.scalarVectorSearch()
    .table("products")
    .queryVector(queryVector)
    .filter(filter)  // Use Filter object
    .metric("l2")
    .topk(10)
    .outputFields("price", "category_id", "status")
    .search();

// Process results
for (HashMap<String, Sqlizable> row : results) {
    double price = Double.parseDouble(row.get("price").toString());
    int categoryId = Integer.parseInt(row.get("category_id").toString());
    int status = Integer.parseInt(row.get("status").toString());
    // Process result...
}
```

For more details on the Filter API, see [04-filter](04-filter.md).

## Builder API

Both hybrid search types provide fluent builder APIs for easy configuration:

### Text Vector Search Builder

```java
ob.textVectorSearch()
    .table("table_name")                    // Required: table name
    .queryVector(queryVector)                // Required: query vector
    .textFields("field1", "field2")          // Required: text fields to search
    .textQuery("search query")               // Required: text query string
    .vectorColumn("embedding")                // Optional: vector column name (default: "embedding")
    .metric("cosine")                        // Optional: metric type (default: "cosine")
    .filter(filter)                          // Optional: scalar filter (Filter object, recommended)
    .topk(10)                                // Optional: number of results (default: 10)
    .outputFields("field1", "field2")        // Optional: output fields (auto-infer types)
    .outputFields(fields, types)             // Optional: output fields with explicit types
    .rankWindowSize(20)                      // Optional: RRF ranking window size
    .search();
```

### Scalar Vector Search Builder

```java
ob.scalarVectorSearch()
    .table("table_name")                     // Required: table name
    .queryVector(queryVector)                // Required: query vector
    .filter(filter)                          // Required: scalar filter (Filter object, recommended)
    .vectorColumn("embedding")                // Optional: vector column name (default: "embedding")
    .metric("l2")                            // Optional: metric type (default: "l2")
    .topk(10)                                // Optional: number of results (default: 10)
    .outputFields("field1", "field2")        // Optional: output fields (auto-infer types)
    .outputFields(fields, types)             // Optional: output fields with explicit types
    .search();
```

## Complete Examples

### Example 1: E-commerce Product Search

```java
// Search for products similar to a query vector, filtered by category and price range
Filter filter = FilterBuilder.and(
    FilterBuilder.key("category_id").isEqualTo(1),
    FilterBuilder.key("price").isGreaterThanOrEqualTo(50.0),
    FilterBuilder.key("price").isLessThanOrEqualTo(500.0),
    FilterBuilder.key("status").isEqualTo(1)  // Only active products
);

ArrayList<HashMap<String, Sqlizable>> results = ob.scalarVectorSearch()
    .table("products")
    .queryVector(queryVector)
    .filter(filter)
    .metric("cosine")
    .topk(20)
    .outputFields("id", "name", "price", "category_id")
    .search();
```

### Example 2: Document Retrieval with Full-Text Search

```java
// Search documents by keywords and semantic similarity
ArrayList<HashMap<String, Sqlizable>> results = ob.textVectorSearch()
    .table("documents")
    .queryVector(queryVector)
    .textFields("title", "content", "summary")
    .textQuery("machine learning algorithms")
    .metric("cosine")
    .topk(15)
    .outputFields("title", "content", "author", "date")
    .search();
```

### Example 3: Combined Full-Text and Scalar Filtering

```java
// Full-text search with scalar filters
Filter filter = FilterBuilder.and(
    FilterBuilder.key("category_id").isEqualTo(2),
    FilterBuilder.key("published_date").isGreaterThanOrEqualTo("2024-01-01")
);

ArrayList<HashMap<String, Sqlizable>> results = ob.textVectorSearch()
    .table("articles")
    .queryVector(queryVector)
    .textFields("title", "body")
    .textQuery("artificial intelligence")
    .filter(filter)
    .metric("cosine")
    .topk(10)
    .outputFields("title", "body", "author", "published_date")
    .search();
```

## Best Practices

### 1. Index Creation

- **Vector Index**: Always create a vector index on the vector column for efficient similarity search
- **Full-Text Index**: Create full-text indexes on all text columns you plan to search
- **Wait for Index Building**: After creating indexes, wait a few seconds for them to be built before performing searches

```java
ob.createCollection("table_name", collectionSchema);
ob.createFulltextIndex("table_name", "ft_index", "text_column");
Thread.sleep(3000);  // Wait for index building
```

### 2. Metric Selection

Choose the appropriate distance metric based on your use case:

- **Cosine**: Best for normalized vectors, measures angle between vectors
- **L2 (Euclidean)**: Measures straight-line distance, good for general similarity
- **IP (Inner Product)**: Measures dot product, useful for normalized vectors

```java
// For text embeddings (usually normalized)
.metric("cosine")

// For general vector similarity
.metric("l2")

// For normalized vectors with inner product
.metric("ip")
```

### 3. Filter Usage

- **Always use Filter API**: Use `FilterBuilder` to create type-safe filters instead of string expressions
- Use indexed scalar fields in filters for better performance
- Combine multiple conditions using `FilterBuilder.and()` or `FilterBuilder.or()`
- Avoid overly complex filters that might slow down queries

```java
// ✅ Recommended: Use Filter API
Filter filter = FilterBuilder.and(
    FilterBuilder.key("category_id").isEqualTo(1),
    FilterBuilder.key("price").isGreaterThanOrEqualTo(50.0)
);

// ❌ Not recommended: String expressions are deprecated
// .filter("category_id = 1 AND price >= 50")
```

### 4. Output Fields

- Use `outputFields()` to specify only the fields you need
- Auto-infer types when possible (simpler code)
- Explicitly specify types when needed for type safety

```java
// Good: Auto-infer types
.outputFields("field1", "field2")

// Good: Explicit types when needed
.outputFields(
    new String[]{"field1", "field2"},
    new DataType[]{DataType.STRING, DataType.INT32}
)
```

### 5. TopK Selection

- Choose an appropriate `topk` value based on your needs
- Larger `topk` values return more results but may be slower
- For RRF ranking, `rankWindowSize` should be >= `topk`

### 6. Error Handling

Always wrap search operations in try-catch blocks:

```java
try {
    ArrayList<HashMap<String, Sqlizable>> results = ob.textVectorSearch()
        .table("documents")
        .queryVector(queryVector)
        .textFields("title", "content")
        .textQuery("query")
        .topk(10)
        .search();
    
    // Process results
    for (HashMap<String, Sqlizable> row : results) {
        // ...
    }
} catch (Throwable e) {
    e.printStackTrace();
    // Handle error
}
```

## API Reference

### ObVecClient Methods

#### textVectorSearch()

Returns a `HybridTextVectorSearchBuilder` for building full-text + vector hybrid search queries.

```java
HybridTextVectorSearchBuilder textVectorSearch()
```

#### scalarVectorSearch()

Returns a `HybridScalarVectorSearchBuilder` for building scalar + vector hybrid search queries.

```java
HybridScalarVectorSearchBuilder scalarVectorSearch()
```

### HybridTextVectorSearchBuilder Methods

| Method | Required | Description |
|--------|----------|-------------|
| `table(String)` | Yes | Set the table name |
| `queryVector(float[])` | Yes | Set the query vector |
| `textFields(String...)` | Yes | Set text fields to search |
| `textQuery(String)` | Yes | Set the text query string |
| `vectorColumn(String)` | No | Set vector column name (default: "embedding") |
| `metric(String)` | No | Set distance metric (default: "cosine") |
| `filter(Object)` | No | Set scalar filter (Filter object, recommended) |
| `topk(int)` | No | Set number of results (default: 10) |
| `outputFields(String...)` | No | Set output fields (auto-infer types) |
| `outputFields(String[], DataType[])` | No | Set output fields with explicit types |
| `rankWindowSize(int)` | No | Set RRF ranking window size |
| `search()` | Yes | Execute the search |

### HybridScalarVectorSearchBuilder Methods

| Method | Required | Description |
|--------|----------|-------------|
| `table(String)` | Yes | Set the table name |
| `queryVector(float[])` | Yes | Set the query vector |
| `filter(Object)` | Yes | Set scalar filter (Filter object, recommended) |
| `vectorColumn(String)` | No | Set vector column name (default: "embedding") |
| `metric(String)` | No | Set distance metric (default: "l2") |
| `topk(int)` | No | Set number of results (default: 10) |
| `outputFields(String...)` | No | Set output fields (auto-infer types) |
| `outputFields(String[], DataType[])` | No | Set output fields with explicit types |
| `search()` | Yes | Execute the search |

### Supported Metric Types

- `"l2"`: L2 (Euclidean) distance
- `"ip"`: Inner product
- `"cosine"`: Cosine similarity

### Return Type

Both builders return `ArrayList<HashMap<String, Sqlizable>>` where:
- Each `HashMap` represents one search result
- Keys are field names
- Values are `Sqlizable` objects that can be converted to appropriate types

## Implementation Notes

### Legacy paths (< 4.6.0)

The scalar + vector hybrid search uses SQL WHERE clauses to apply scalar filtering conditions, then performs vector similarity search on the filtered results. This approach:

- Uses standard SQL syntax, ensuring better compatibility across OceanBase versions
- Applies scalar filters directly in the WHERE clause before vector search
- Provides efficient filtering combined with vector similarity ranking

### Full-Text + Vector Hybrid Search

The full-text + vector hybrid search uses Reciprocal Rank Fusion (RRF) to combine results from:
- Full-text search (using MATCH AGAINST)
- Vector similarity search

Both searches are performed independently, and results are merged using RRF to provide a unified ranking.

## Related Documentation

- [04-filter](04-filter.md) — Filter API reference
- [03-混合搜索 (中文)](../zh/03-混合搜索.md) — Chinese hybrid search guide

