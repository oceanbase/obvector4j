# Filter API

> 中文：[Filter过滤](../zh/Filter过滤.md)

## Table of Contents

1. [Overview](#overview)
2. [Quick Start](#quick-start)
3. [Comparison Filters](#comparison-filters)
4. [Logical Filters](#logical-filters)
5. [Using Filters in Hybrid Search](#using-filters-in-hybrid-search)
6. [Complete Examples](#complete-examples)
7. [Best Practices](#best-practices)
8. [API Reference](#api-reference)

## Overview

The Filter API provides a type-safe way to build filters for specifying scalar filtering conditions in hybrid search queries. Compared to traditional string SQL expressions, the Filter API offers the following advantages:

- **Type Safety**: Compile-time checking reduces runtime errors
- **Easy to Use**: Fluent API design makes code more readable
- **Feature Complete**: Supports all common comparison and logical operations
- **Backward Compatible**: Still supports string expressions

## Quick Start

### Basic Usage

```java
import com.oceanbase.obvector4j.ObVecClient;
import com.oceanbase.obvector4j.filter.Filter;
import com.oceanbase.obvector4j.filter.FilterBuilder;

// Create client
ObVecClient ob = new ObVecClient(uri, user, password);

// Create a simple equality filter
Filter filter = FilterBuilder.key("category_id").isEqualTo(1);

// Use in hybrid search
ArrayList<HashMap<String, Sqlizable>> results = ob.scalarVectorSearch()
    .table("products")
    .queryVector(queryVector)
    .filter(filter)
    .topk(10)
    .outputFields("id", "name", "price")
    .search();
```

## Comparison Filters

### 1. Equality (IsEqualTo)

```java
// category_id = 1
Filter filter = FilterBuilder.key("category_id").isEqualTo(1);
```

### 2. Inequality (IsNotEqualTo)

```java
// status != 0
Filter filter = FilterBuilder.key("status").isNotEqualTo(0);
```

### 3. Greater Than (IsGreaterThan)

```java
// price > 100
Filter filter = FilterBuilder.key("price").isGreaterThan(100.0);
```

### 4. Greater Than or Equal (IsGreaterThanOrEqualTo)

```java
// price >= 100
Filter filter = FilterBuilder.key("price").isGreaterThanOrEqualTo(100.0);
```

### 5. Less Than (IsLessThan)

```java
// price < 1000
Filter filter = FilterBuilder.key("price").isLessThan(1000.0);
```

### 6. Less Than or Equal (IsLessThanOrEqualTo)

```java
// price <= 1000
Filter filter = FilterBuilder.key("price").isLessThanOrEqualTo(1000.0);
```

### 7. IN Query (IsIn)

```java
// category_id IN (1, 2, 3)
Filter filter = FilterBuilder.key("category_id").isIn(1, 2, 3);

// Using Collection
List<Integer> categories = Arrays.asList(1, 2, 3);
Filter filter = FilterBuilder.key("category_id").isIn(categories);
```

### 8. NOT IN Query (IsNotIn)

```java
// category_id NOT IN (4, 5, 6)
Filter filter = FilterBuilder.key("category_id").isNotIn(4, 5, 6);

// Using Collection
List<Integer> excludedCategories = Arrays.asList(4, 5, 6);
Filter filter = FilterBuilder.key("category_id").isNotIn(excludedCategories);
```

### 9. String Contains (Contains)

```java
// title LIKE '%OceanBase%'
Filter filter = FilterBuilder.key("title").contains("OceanBase");
```

## Logical Filters

### 1. AND Operation

```java
// category_id = 1 AND price >= 100
Filter filter = FilterBuilder.and(
    FilterBuilder.key("category_id").isEqualTo(1),
    FilterBuilder.key("price").isGreaterThanOrEqualTo(100.0)
);
```

### 2. OR Operation

```java
// category_id = 1 OR category_id = 2
Filter filter = FilterBuilder.or(
    FilterBuilder.key("category_id").isEqualTo(1),
    FilterBuilder.key("category_id").isEqualTo(2)
);
```

### 3. NOT Operation

```java
// NOT (status = 0)
Filter filter = FilterBuilder.not(
    FilterBuilder.key("status").isEqualTo(0)
);
```

### 4. Complex Nested Queries

```java
// (category_id = 1 OR category_id = 2) AND price >= 100 AND price <= 500
Filter filter = FilterBuilder.and(
    FilterBuilder.or(
        FilterBuilder.key("category_id").isEqualTo(1),
        FilterBuilder.key("category_id").isEqualTo(2)
    ),
    FilterBuilder.and(
        FilterBuilder.key("price").isGreaterThanOrEqualTo(100.0),
        FilterBuilder.key("price").isLessThanOrEqualTo(500.0)
    )
);
```

## Using Filters in Hybrid Search

### 1. Scalar Vector Hybrid Search

```java
// Using Filter object
Filter filter = FilterBuilder.and(
    FilterBuilder.key("category_id").isEqualTo(1),
    FilterBuilder.key("price").isGreaterThanOrEqualTo(50.0),
    FilterBuilder.key("price").isLessThanOrEqualTo(250.0)
);

ArrayList<HashMap<String, Sqlizable>> results = ob.scalarVectorSearch()
    .table("products")
    .queryVector(queryVector)
    .filter(filter)  // Using Filter object
    .metric("l2")
    .topk(10)
    .outputFields("id", "name", "price", "category_id")
    .search();
```

### 2. Text Vector Hybrid Search

```java
// Filters can be combined with text vector search
Filter filter = FilterBuilder.and(
    FilterBuilder.key("category_id").isEqualTo(1),
    FilterBuilder.key("price").isLessThanOrEqualTo(200.0)
);

ArrayList<HashMap<String, Sqlizable>> results = ob.textVectorSearch()
    .table("documents")
    .queryVector(queryVector)
    .textFields("title", "content")
    .textQuery("OceanBase database")
    .filter(filter)  // Add Filter
    .topk(10)
    .outputFields("title", "content", "category_id", "price")
    .search();
```

### 3. Backward Compatibility: String Expressions

```java
// Still supports string expressions (backward compatible)
ArrayList<HashMap<String, Sqlizable>> results = ob.scalarVectorSearch()
    .table("products")
    .queryVector(queryVector)
    .filter("category_id = 1 AND price >= 50 AND price <= 250")  // String expression
    .topk(10)
    .outputFields("id", "name", "price")
    .search();
```

## Complete Examples

### Example 1: E-commerce Product Search

```java
import com.oceanbase.obvector4j.ObVecClient;
import com.oceanbase.obvector4j.filter.Filter;
import com.oceanbase.obvector4j.filter.FilterBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Arrays;

// Create client
ObVecClient ob = new ObVecClient(uri, user, password);

// Build complex filter
// Conditions: category is Electronics(1) or Books(2), price between 50-500, status is available(1)
Filter filter = FilterBuilder.and(
    FilterBuilder.or(
        FilterBuilder.key("category_id").isEqualTo(1),
        FilterBuilder.key("category_id").isEqualTo(2)
    ),
    FilterBuilder.and(
        FilterBuilder.key("price").isGreaterThanOrEqualTo(50.0),
        FilterBuilder.key("price").isLessThanOrEqualTo(500.0)
    ),
    FilterBuilder.key("status").isEqualTo(1)
);

// Execute search
float[] queryVector = generateProductVector();
ArrayList<HashMap<String, Sqlizable>> results = ob.scalarVectorSearch()
    .table("products")
    .queryVector(queryVector)
    .filter(filter)
    .metric("cosine")
    .topk(20)
    .outputFields("id", "name", "price", "category_id", "status")
    .search();

// Process results
for (HashMap<String, Sqlizable> row : results) {
    String productId = row.get("id").toString();
    String productName = row.get("name").toString();
    double price = Double.parseDouble(row.get("price").toString());
    // ... process product information
}
```

### Example 2: Document Retrieval System

```java
// Search documents in specific categories, excluding certain statuses
Filter filter = FilterBuilder.and(
    FilterBuilder.key("category").isIn("Technology", "Product", "Marketing"),
    FilterBuilder.not(
        FilterBuilder.key("status").isEqualTo("Deleted")
    ),
    FilterBuilder.key("publish_date").isGreaterThanOrEqualTo("2024-01-01")
);

ArrayList<HashMap<String, Sqlizable>> results = ob.textVectorSearch()
    .table("documents")
    .queryVector(queryVector)
    .textFields("title", "content")
    .textQuery("OceanBase database")
    .filter(filter)
    .topk(10)
    .outputFields("id", "title", "category", "publish_date")
    .search();
```

### Example 3: Range Queries

```java
// Price range query: 100 <= price < 500
Filter filter = FilterBuilder.and(
    FilterBuilder.key("price").isGreaterThanOrEqualTo(100.0),
    FilterBuilder.key("price").isLessThan(500.0)  // Note: using isLessThan instead of isLessThanOrEqualTo
);

ArrayList<HashMap<String, Sqlizable>> results = ob.scalarVectorSearch()
    .table("products")
    .queryVector(queryVector)
    .filter(filter)
    .topk(10)
    .outputFields("id", "name", "price")
    .search();
```

### Example 4: Multi-condition Combination

```java
// Complex business scenario: find products matching conditions
// Conditions:
// 1. Category in specified list
// 2. Price in reasonable range
// 3. Rating >= 4.0
// 4. Stock > 0
// 5. Status is on sale

Filter categoryFilter = FilterBuilder.key("category_id").isIn(1, 2, 3, 4);
Filter priceFilter = FilterBuilder.and(
    FilterBuilder.key("price").isGreaterThanOrEqualTo(10.0),
    FilterBuilder.key("price").isLessThanOrEqualTo(1000.0)
);
Filter ratingFilter = FilterBuilder.key("rating").isGreaterThanOrEqualTo(4.0);
Filter stockFilter = FilterBuilder.key("stock").isGreaterThan(0);
Filter statusFilter = FilterBuilder.key("status").isEqualTo("on_sale");

Filter combinedFilter = FilterBuilder.and(
    categoryFilter,
    priceFilter,
    ratingFilter,
    stockFilter,
    statusFilter
);

ArrayList<HashMap<String, Sqlizable>> results = ob.scalarVectorSearch()
    .table("products")
    .queryVector(queryVector)
    .filter(combinedFilter)
    .topk(50)
    .outputFields("id", "name", "price", "rating", "stock")
    .search();
```

## Best Practices

### 1. Use FilterBuilder to Build Filters

It's recommended to use FilterBuilder's fluent API for clearer code:

```java
// ✅ Recommended: Use FilterBuilder
Filter filter = FilterBuilder.key("category_id").isEqualTo(1);

// ❌ Not recommended: Direct use of Filter factory methods (though possible)
Filter filter = Filter.equal("category_id", 1);
```

### 2. Use AND to Combine Multiple Conditions

```java
// ✅ Recommended: Use AND to combine multiple conditions
Filter filter = FilterBuilder.and(
    FilterBuilder.key("category_id").isEqualTo(1),
    FilterBuilder.key("price").isGreaterThanOrEqualTo(100.0)
);

// ❌ Not recommended: Create multiple independent Filters (cannot combine correctly)
```

### 3. Range Query Merging

Multiple range conditions for the same field are automatically merged by FilterMapper:

```java
// These two conditions will be automatically merged into one range query
Filter filter = FilterBuilder.and(
    FilterBuilder.key("price").isGreaterThanOrEqualTo(50.0),
    FilterBuilder.key("price").isLessThanOrEqualTo(250.0)
);
// Generated JSON: {"range": {"price": {"gte": 50.0, "lte": 250.0}}}
```

### 4. Use IN Instead of Multiple ORs

```java
// ✅ Recommended: Use IN
Filter filter = FilterBuilder.key("category_id").isIn(1, 2, 3);

// ❌ Not recommended: Use multiple ORs (same functionality, but IN is more efficient)
Filter filter = FilterBuilder.or(
    FilterBuilder.key("category_id").isEqualTo(1),
    FilterBuilder.or(
        FilterBuilder.key("category_id").isEqualTo(2),
        FilterBuilder.key("category_id").isEqualTo(3)
    )
);
```

### 5. Avoid Excessive Nesting

While complex nesting is supported, it's recommended to keep reasonable levels:

```java
// ✅ Recommended: Reasonable nesting levels
Filter filter = FilterBuilder.and(
    FilterBuilder.key("category_id").isEqualTo(1),
    FilterBuilder.or(
        FilterBuilder.key("status").isEqualTo("active"),
        FilterBuilder.key("status").isEqualTo("pending")
    )
);

// ❌ Not recommended: Excessive nesting (difficult to understand and maintain)
```

### 6. Type Matching

Ensure the value types in Filters match the database field types:

```java
// ✅ Correct: Numeric type matching
Filter filter1 = FilterBuilder.key("price").isEqualTo(100.0);  // DOUBLE field
Filter filter2 = FilterBuilder.key("category_id").isEqualTo(1);  // INT field

// ✅ Correct: String type matching
Filter filter3 = FilterBuilder.key("status").isEqualTo("active");  // VARCHAR field
```

## API Reference

### FilterBuilder

#### Static Methods

- `key(String key)` - Creates a KeyFilterBuilder to start building a filter
- `and(Filter left, Filter right)` - Creates an AND logical filter
- `or(Filter left, Filter right)` - Creates an OR logical filter
- `not(Filter filter)` - Creates a NOT logical filter

#### KeyFilterBuilder Methods

- `isEqualTo(Object value)` - Equality comparison: `key = value`
- `isNotEqualTo(Object value)` - Inequality comparison: `key != value`
- `isGreaterThan(Object value)` - Greater than comparison: `key > value`
- `isGreaterThanOrEqualTo(Object value)` - Greater than or equal comparison: `key >= value`
- `isLessThan(Object value)` - Less than comparison: `key < value`
- `isLessThanOrEqualTo(Object value)` - Less than or equal comparison: `key <= value`
- `isIn(Object... values)` - IN query: `key IN (value1, value2, ...)`
- `isIn(Collection<?> values)` - IN query (using Collection)
- `isNotIn(Object... values)` - NOT IN query: `key NOT IN (value1, value2, ...)`
- `isNotIn(Collection<?> values)` - NOT IN query (using Collection)
- `contains(String value)` - String contains: `key LIKE '%value%'`

### Filter

#### Factory Methods (Direct use, not recommended)

- `equal(String key, Object value)` - Creates an equality filter
- `notEqual(String key, Object value)` - Creates an inequality filter
- `greaterThan(String key, Object value)` - Creates a greater than filter
- `greaterThanOrEqual(String key, Object value)` - Creates a greater than or equal filter
- `lessThan(String key, Object value)` - Creates a less than filter
- `lessThanOrEqual(String key, Object value)` - Creates a less than or equal filter
- `in(String key, Collection<?> values)` - Creates an IN filter
- `notIn(String key, Collection<?> values)` - Creates a NOT IN filter
- `contains(String key, String value)` - Creates a string contains filter
- `and(Filter left, Filter right)` - Creates an AND logical filter
- `or(Filter left, Filter right)` - Creates an OR logical filter
- `not(Filter expression)` - Creates a NOT logical filter

## Frequently Asked Questions

### Q1: What's the difference between Filter and string expressions?

**A:** Filter API provides type safety with compile-time checking, while string expressions are parsed at runtime. It's recommended to use the Filter API.

### Q2: Is LIKE query supported?

**A:** Yes, use the `contains()` method:
```java
Filter filter = FilterBuilder.key("title").contains("OceanBase");
```

### Q3: How to implement BETWEEN query?

**A:** Use AND to combine two range conditions:
```java
Filter filter = FilterBuilder.and(
    FilterBuilder.key("price").isGreaterThanOrEqualTo(100.0),
    FilterBuilder.key("price").isLessThanOrEqualTo(500.0)
);
```

### Q4: Is NULL value checking supported?

**A:** NULL value checking is not supported in the current version, but may be added in future versions.

### Q5: Does Filter support nested queries?

**A:** Yes, AND, OR, and NOT operations can be nested arbitrarily.

### Q6: What about performance?

**A:** Filter objects are converted to OceanBase's JSON query format, with the same performance as string expressions. When using IN operations, Filters are automatically optimized to should arrays.

## Migration Guide

### Migrating from String Expressions to Filter API

**Before (String Expression):**
```java
.filter("category_id = 1 AND price >= 50 AND price <= 250")
```

**After (Filter API):**
```java
.filter(FilterBuilder.and(
    FilterBuilder.key("category_id").isEqualTo(1),
    FilterBuilder.and(
        FilterBuilder.key("price").isGreaterThanOrEqualTo(50.0),
        FilterBuilder.key("price").isLessThanOrEqualTo(250.0)
    )
))
```

### Migration Steps

1. Identify all places using string filters
2. Convert string expressions to Filter objects
3. Test to verify functionality
4. Migrate gradually, can mix usage (backward compatible)

## Summary

The Filter API provides a powerful and flexible way to build query filters:

- ✅ Type safe, reduces errors
- ✅ Fluent API, readable code
- ✅ Supports all common operations
- ✅ Backward compatible with string expressions
- ✅ Automatically optimizes query performance

Start using the Filter API to make your code more robust and maintainable!
