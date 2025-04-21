# Index Configuration Guide

## Table of Contents

1. [Overview](#overview)
2. [Vector Indexes](#vector-indexes)
3. [Full-Text Indexes](#full-text-indexes)
4. [Complete Examples](#complete-examples)
5. [Quick Reference](#quick-reference)

## Overview

OceanBase supports two types of indexes for hybrid search:

1. **Vector Indexes**: For vector similarity search
2. **Full-Text Indexes**: For keyword-based text search

Both index types are configurable with different algorithms and parsers to suit various use cases.

## Vector Indexes

### Supported Types

| Index Type | Best For | Parameters | Library | Distance Metrics |
|------------|----------|------------|---------|------------------|
| **HNSW** | General purpose (default) | m, ef_construction, ef_search | vsag | l2, inner_product, cosine |
| **HNSW_SQ** | Memory-constrained | m, ef_construction, ef_search | vsag | l2, inner_product, cosine |
| **IVFFLAT** | Large datasets | nlist, samples_per_nlist | OB | l2, inner_product, cosine |
| **IVFSQ** | Large datasets, memory constraints | nlist, samples_per_nlist | OB | l2, inner_product, cosine |
| **IVFPQ** | Very large datasets | nlist, samples_per_nlist, m (required) | OB | l2, inner_product, cosine |
| **DAAT** | Sparse vectors | sparse_index_type (optional) | OB | inner_product (required) |

### Quick Start

```java
import com.oceanbase.obvec_jdbc.IndexParam;
import com.oceanbase.obvec_jdbc.VecIndexType;

// Default HNSW
IndexParam index1 = new IndexParam("vidx1", "embedding");

// Custom HNSW
IndexParam index2 = new IndexParam("vidx2", "embedding", VecIndexType.HNSW)
    .M(32)
    .EfConstruction(300)
    .EfSearch(128)
    .MetricType("cosine");

// HNSW_SQ (memory efficient)
IndexParam index3 = new IndexParam("vidx3", "embedding", VecIndexType.HNSW_SQ);

// IVFFLAT (large datasets)
IndexParam index4 = new IndexParam("vidx4", "embedding", VecIndexType.IVFFLAT)
    .Nlist(100)
    .SamplesPerNlist(1000);

// IVFPQ (most memory efficient)
IndexParam index5 = new IndexParam("vidx5", "embedding", VecIndexType.IVFPQ)
    .Nlist(100)
    .SamplesPerNlist(1000)
    .PQM(16);  // Required for IVFPQ

// DAAT (sparse vectors)
IndexParam index6 = new IndexParam("vidx6", "sparse_vec", VecIndexType.DAAT)
    .MetricType("inner_product");  // Required
```

For detailed information, see [VECTOR_INDEX.md](./VECTOR_INDEX.md).

## Full-Text Indexes

### Supported Parsers

| Parser | Parser Name | Best For | Use Case |
|--------|-------------|----------|----------|
| **SPACE** | (default) | General text | Default, splits by spaces |
| **IK** | `ik` | Chinese text | Chinese word segmentation |
| **NGRAM** | `ngram` | Asian languages | N-character sequences |
| **NGRAM2** | `ngram2` | Asian languages | Improved n-gram (V4.3.5 BP2+) |
| **BASIC_ENGLISH** | `beng` | English text | English language parser |
| **JIEBA** | `jieba` | Chinese text | Jieba segmentation |

### Quick Start

```java
import com.oceanbase.obvec_jdbc.FtsParser;
import com.oceanbase.obvec_jdbc.FtsIndexParam;
import java.util.Arrays;

ObVecClient ob = new ObVecClient(uri, user, password);

// Default Space parser
ob.createFulltextIndex("articles", "ft_title", "title");

// IK parser for Chinese
ob.createFulltextIndex("articles", "ft_content", "content", FtsParser.IK);

// NGRAM parser
ob.createFulltextIndex("articles", "ft_title", "title", FtsParser.NGRAM);

// Multiple fields with parser
FtsIndexParam ftsParam = new FtsIndexParam(
    "ft_multi",
    Arrays.asList("title", "content"),
    FtsParser.NGRAM2
);
ob.createFulltextIndex("articles", ftsParam);

// Custom parser
ob.createFulltextIndex("articles", "ft_title", "title", "thai_ftparser");
```

For detailed information, see [FULLTEXT_INDEX.md](./FULLTEXT_INDEX.md).

## Complete Examples

### Example 1: Hybrid Search with Custom Indexes

```java
import com.oceanbase.obvec_jdbc.*;
import java.util.Arrays;

ObVecClient ob = new ObVecClient(uri, user, password);
String tableName = "hybrid_search_demo";

// Create schema
ObCollectionSchema schema = new ObCollectionSchema();

// Vector field
ObFieldSchema vecField = new ObFieldSchema("embedding", DataType.FLOAT_VECTOR);
vecField.Dim(128).IsNullable(false);
schema.addField(vecField);

// Text fields
ObFieldSchema titleField = new ObFieldSchema("title", DataType.STRING);
titleField.IsNullable(false);
schema.addField(titleField);

ObFieldSchema contentField = new ObFieldSchema("content", DataType.STRING);
contentField.IsNullable(false);
schema.addField(contentField);

// Vector index with custom parameters
IndexParams indexParams = new IndexParams();
IndexParam vecIndex = new IndexParam("vidx_embedding", "embedding", VecIndexType.HNSW)
    .M(32)
    .EfConstruction(300)
    .EfSearch(128)
    .MetricType("cosine");
indexParams.addIndex(vecIndex);
schema.setIndexParams(indexParams);

ob.createCollection(tableName, schema);

// Full-text indexes with appropriate parsers
ob.createFulltextIndex(tableName, "ft_title", "title", FtsParser.NGRAM);
ob.createFulltextIndex(tableName, "ft_content", "content", FtsParser.NGRAM);
```

### Example 2: Chinese Text Search Setup

```java
// Vector index
IndexParam vecIndex = new IndexParam("vidx_embedding", "embedding");

// Full-text indexes with IK parser for Chinese
ob.createFulltextIndex(tableName, "ft_title", "title", FtsParser.IK);
ob.createFulltextIndex(tableName, "ft_content", "content", FtsParser.IK);
```

### Example 3: Memory-Efficient Setup

```java
// Use HNSW_SQ for memory-efficient vector index
IndexParam vecIndex = new IndexParam("vidx_embedding", "embedding", VecIndexType.HNSW_SQ)
    .M(16)
    .EfConstruction(200)
    .EfSearch(64);

// Use NGRAM parser (more memory efficient than IK/JIEBA)
ob.createFulltextIndex(tableName, "ft_title", "title", FtsParser.NGRAM);
```

## Quick Reference

### VecIndexType Enum

```java
VecIndexType.HNSW       // Default
VecIndexType.HNSW_SQ    // Memory efficient HNSW
VecIndexType.IVFFLAT    // Large datasets
VecIndexType.IVFSQ      // Large datasets, memory constrained
VecIndexType.IVFPQ      // Very large datasets
VecIndexType.DAAT       // Sparse vectors
```

### FtsParser Enum

```java
FtsParser.SPACE         // Default (no parser specified)
FtsParser.IK            // Chinese (IK Analyzer)
FtsParser.NGRAM         // Asian languages
FtsParser.NGRAM2        // Improved n-gram (V4.3.5 BP2+)
FtsParser.BASIC_ENGLISH // English
FtsParser.JIEBA         // Chinese (Jieba)
```

### Common Patterns

#### Pattern 1: Default Setup (Quick Start)
```java
// Vector index: HNSW with defaults
IndexParam vecIndex = new IndexParam("vidx", "embedding");

// Full-text index: Space parser (default)
ob.createFulltextIndex(tableName, "ft_title", "title");
```

#### Pattern 2: High Performance
```java
// Vector index: Custom HNSW
IndexParam vecIndex = new IndexParam("vidx", "embedding")
    .M(32)
    .EfConstruction(300)
    .EfSearch(128)
    .MetricType("cosine");

// Full-text index: NGRAM2 (if supported)
ob.createFulltextIndex(tableName, "ft_title", "title", FtsParser.NGRAM2);
```

#### Pattern 3: Memory Efficient
```java
// Vector index: HNSW_SQ
IndexParam vecIndex = new IndexParam("vidx", "embedding", VecIndexType.HNSW_SQ);

// Full-text index: NGRAM (more efficient than IK/JIEBA)
ob.createFulltextIndex(tableName, "ft_title", "title", FtsParser.NGRAM);
```

#### Pattern 4: Large Dataset
```java
// Vector index: IVFFLAT
IndexParam vecIndex = new IndexParam("vidx", "embedding", VecIndexType.IVFFLAT)
    .Nlist(1000)
    .SamplesPerNlist(5000);
```

## References

- [Vector Index Guide](./VECTOR_INDEX.md) - Detailed vector index documentation
- [Full-Text Index Guide](./FULLTEXT_INDEX.md) - Detailed full-text index documentation
- [OceanBase Documentation](https://www.oceanbase.com/docs/common-oceanbase-database-standalone-1000000003577789)
- [pyobvector Reference](https://github.com/oceanbase/pyobvector)

