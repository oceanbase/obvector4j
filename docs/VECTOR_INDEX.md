# Vector Index Configuration Guide

## Table of Contents

1. [Overview](#overview)
2. [Vector Index Types](#vector-index-types)
3. [Index Parameters](#index-parameters)
4. [Usage Examples](#usage-examples)
5. [Best Practices](#best-practices)
6. [API Reference](#api-reference)

## Overview

OceanBase supports multiple vector index algorithms for different use cases. This guide explains how to configure vector indexes using the `IndexParam` class and `VecIndexType` enumeration.

## Vector Index Types

OceanBase supports the following vector index types (defined in `VecIndexType` enum):

### HNSW (Hierarchical Navigable Small World)
- **Best for**: General purpose vector search with good balance of speed and accuracy
- **Default**: Yes, this is the default index type
- **Parameters**: `m`, `ef_construction`, `ef_search`
- **Library**: `vsag` (default)
- **Distance metrics**: `l2`, `inner_product`, `cosine`

### HNSW_SQ (HNSW with Scalar Quantization)
- **Best for**: Memory-constrained environments
- **Parameters**: `m`, `ef_construction`, `ef_search`
- **Library**: `vsag` (default)
- **Distance metrics**: `l2`, `inner_product`, `cosine`

### IVFFLAT (Inverted File Flat)
- **Best for**: Large datasets with many vectors
- **Parameters**: `nlist`, `samples_per_nlist`
- **Library**: `OB` (default)
- **Distance metrics**: `l2`, `inner_product`, `cosine`

### IVFSQ (Inverted File Scalar Quantization)
- **Best for**: Large datasets with memory constraints
- **Parameters**: `nlist`, `samples_per_nlist`
- **Library**: `OB` (default)
- **Distance metrics**: `l2`, `inner_product`, `cosine`

### IVFPQ (Inverted File Product Quantization)
- **Best for**: Very large datasets with strict memory constraints
- **Parameters**: `nlist`, `samples_per_nlist`, `m` (for PQ, **required**)
- **Library**: `OB` (default)
- **Distance metrics**: `l2`, `inner_product`, `cosine`

### DAAT (Document-at-a-time)
- **Best for**: Sparse vector search
- **Parameters**: `sparse_index_type` (optional)
- **Library**: `OB` (default)
- **Distance metrics**: `inner_product` (**required**, cannot use other metrics)

## Index Parameters

### HNSW Parameters

- **m** (default: 16): Number of bi-directional links. Higher values improve recall but increase memory usage.
  - Typical range: 4-64
  - Recommended: 16-32 for most use cases

- **ef_construction** (default: 200): Size of the candidate set during index construction.
  - Typical range: 100-500
  - Higher values improve index quality but slow down construction

- **ef_search** (default: 64): Size of the candidate set during search.
  - Typical range: 16-512
  - Higher values improve recall but slow down search

### IVF Parameters

- **nlist**: Number of clusters (centroids)
  - Typical range: 100-10000
  - Recommended: sqrt(total_vectors) for IVFFLAT

- **samples_per_nlist**: Number of samples per cluster for training
  - Typical range: 100-10000
  - Recommended: 1000-5000

- **m** (for IVFPQ only, **required**): Number of sub-vectors for product quantization
  - Typical range: 8-64
  - Must be a divisor of vector dimension

### Sparse Vector Parameters (DAAT)

- **sparse_index_type**: Optional sparse index type configuration
  - Used for advanced sparse vector indexing

### Common Parameters

- **lib**: Algorithm library (`vsag` or `OB`)
  - `vsag`: Default for HNSW indexes
  - `OB`: Default for IVF and DAAT indexes

- **distance** (metric_type): Distance function
  - `l2`: Euclidean distance (default for dense vectors)
  - `inner_product` or `ip`: Inner product (required for sparse vectors)
  - `cosine`: Cosine similarity

## Usage Examples

### Example 1: Default HNSW Index

```java
import com.oceanbase.obvec_jdbc.IndexParam;
import com.oceanbase.obvec_jdbc.IndexParams;

// Create index with default HNSW parameters
IndexParams index_params = new IndexParams();
IndexParam index_param = new IndexParam("vidx_embedding", "embedding");
// Uses default: m=16, ef_construction=200, ef_search=64, lib=vsag, distance=l2
index_params.addIndex(index_param);
collectionSchema.setIndexParams(index_params);
```

### Example 2: Custom HNSW Index

```java
import com.oceanbase.obvec_jdbc.IndexParam;
import com.oceanbase.obvec_jdbc.VecIndexType;

// Create HNSW index with custom parameters
IndexParam index_param = new IndexParam("vidx_embedding", "embedding", VecIndexType.HNSW)
    .M(32)                    // Increase M for better recall
    .EfConstruction(300)     // Increase ef_construction for better quality
    .EfSearch(128)            // Increase ef_search for better recall
    .MetricType("cosine");    // Use cosine distance
```

### Example 3: HNSW_SQ Index (Memory Efficient)

```java
// Create HNSW_SQ index for memory-constrained environments
IndexParam index_param = new IndexParam("vidx_embedding", "embedding", VecIndexType.HNSW_SQ)
    .M(16)
    .EfConstruction(200)
    .EfSearch(64)
    .MetricType("l2");
```

### Example 4: IVFFLAT Index

```java
// Create IVFFLAT index for large datasets
IndexParam index_param = new IndexParam("vidx_embedding", "embedding", VecIndexType.IVFFLAT)
    .Nlist(100)               // Number of clusters
    .SamplesPerNlist(1000)    // Samples per cluster
    .MetricType("l2");
```

### Example 5: IVFPQ Index (Most Memory Efficient)

```java
// Create IVFPQ index - requires pq_m parameter
IndexParam index_param = new IndexParam("vidx_embedding", "embedding", VecIndexType.IVFPQ)
    .Nlist(100)
    .SamplesPerNlist(1000)
    .PQM(16)                  // Required: number of sub-vectors for PQ
    .MetricType("l2");
```

### Example 6: DAAT Index for Sparse Vectors

```java
// Create DAAT index for sparse vectors (must use inner_product)
IndexParam index_param = new IndexParam("vidx_sparse", "sparse_vector", VecIndexType.DAAT)
    .SparseIndexType("custom_type")  // Optional
    .MetricType("inner_product");    // Required for sparse vectors
```

### Example 7: Using String Type

```java
// Create index using string type (case-insensitive)
IndexParam index_param = new IndexParam("vidx_embedding", "embedding", "hnsw")
    .M(16)
    .EfConstruction(200)
    .EfSearch(64);
```

## Best Practices

### Choosing the Right Index Type

1. **HNSW**: Use for general purpose vector search with moderate dataset sizes (< 10M vectors)
2. **HNSW_SQ**: Use when memory is constrained but you still want HNSW performance
3. **IVFFLAT**: Use for large datasets (> 10M vectors) where memory is not a concern
4. **IVFSQ**: Use for large datasets with moderate memory constraints
5. **IVFPQ**: Use for very large datasets with strict memory constraints
6. **DAAT**: Use only for sparse vectors

### Parameter Tuning

#### HNSW Tuning
- **m**: Start with 16, increase to 32-64 for better recall if memory allows
- **ef_construction**: Start with 200, increase to 300-500 for better quality
- **ef_search**: Start with 64, increase to 128-256 for better recall

#### IVF Tuning
- **nlist**: Use sqrt(total_vectors) as a starting point
- **samples_per_nlist**: Use 1000-5000 depending on dataset size
- **m** (IVFPQ): Must divide vector dimension evenly (e.g., 16 for 128-dim vectors)

### Distance Metric Selection

- **L2**: Best for general purpose similarity search
- **Inner Product**: Best for normalized vectors or sparse vectors
- **Cosine**: Best when you care about direction rather than magnitude

## API Reference

### VecIndexType Enum

```java
public enum VecIndexType {
    HNSW,      // Default
    HNSW_SQ,
    IVFFLAT,
    IVFSQ,
    IVFPQ,
    DAAT
}
```

### IndexParam Class

#### Constructors

```java
// Default HNSW
IndexParam(String vidx_name, String vector_field_name)

// With VecIndexType
IndexParam(String vidx_name, String vector_field_name, VecIndexType index_type)

// With string type
IndexParam(String vidx_name, String vector_field_name, String indexTypeStr)
```

#### HNSW Methods

```java
IndexParam M(int m)
IndexParam EfConstruction(int ef_construction)
IndexParam EfSearch(int ef_search)
```

#### IVF Methods

```java
IndexParam Nlist(int nlist)
IndexParam SamplesPerNlist(int samples_per_nlist)
IndexParam PQM(int pq_m)  // IVFPQ only
```

#### Sparse Vector Methods

```java
IndexParam SparseIndexType(String sparse_index_type)  // DAAT only
```

#### Common Methods

```java
IndexParam Lib(String lib)
IndexParam MetricType(String metric_type)
```

#### Getters

```java
String getVidxName()
String getFieldName()
VecIndexType getIndexType()
Integer getM()
Integer getEfConstruction()
Integer getEfSearch()
Integer getNlist()
Integer getSamplesPerNlist()
Integer getPQM()
String getSparseIndexType()
String getLib()
String getMetricType()
```

## References

- [OceanBase Vector Index Documentation](https://www.oceanbase.com/docs/common-oceanbase-database-standalone-1000000003577789)
- [pyobvector IndexParam Reference](https://github.com/oceanbase/pyobvector)

