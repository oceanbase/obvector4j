# Full-Text Index Configuration Guide

## Table of Contents

1. [Overview](#overview)
2. [Full-Text Search Parsers](#full-text-search-parsers)
3. [Usage Examples](#usage-examples)
4. [Best Practices](#best-practices)
5. [API Reference](#api-reference)

## Overview

OceanBase supports full-text search indexes with multiple parser types for different languages and use cases. This guide explains how to configure full-text indexes using the `FtsParser` enumeration and `FtsIndexParam` class.

## Full-Text Search Parsers

OceanBase supports the following full-text search parsers (defined in `FtsParser` enum):

### SPACE (Default)
- **Parser name**: None (default, no need to specify)
- **Best for**: General text search, splits by spaces
- **Use case**: Default parser for most scenarios

### IK (IK Analyzer)
- **Parser name**: `ik`
- **Best for**: Chinese text search
- **Use case**: Chinese word segmentation using IK Analyzer

### NGRAM
- **Parser name**: `ngram`
- **Best for**: Asian languages, prefix/suffix matching
- **Use case**: Splits text into n-character sequences

### NGRAM2
- **Parser name**: `ngram2`
- **Best for**: Asian languages with better performance
- **Use case**: Improved n-gram parser (supported from OceanBase V4.3.5 BP2+)

### BASIC_ENGLISH
- **Parser name**: `beng`
- **Best for**: English text search
- **Use case**: Basic English language parser

### JIEBA
- **Parser name**: `jieba`
- **Best for**: Chinese text search with Jieba segmentation
- **Use case**: Chinese word segmentation using Jieba

### Custom Parsers
- **Parser name**: Custom string (e.g., `"thai_ftparser"`)
- **Best for**: Custom language parsers
- **Use case**: Use string directly instead of enum for custom parsers

## Usage Examples

### Example 1: Default Space Parser

```java
import com.oceanbase.obvec_jdbc.ObVecClient;

ObVecClient ob = new ObVecClient(uri, user, password);

// Create full-text index with default Space parser
ob.createFulltextIndex("articles", "ft_title", "title");
// Equivalent to: ALTER TABLE articles ADD FULLTEXT INDEX ft_title(`title`)
```

### Example 2: IK Parser for Chinese

```java
import com.oceanbase.obvec_jdbc.ObVecClient;
import com.oceanbase.obvec_jdbc.FtsParser;

// Create full-text index with IK parser for Chinese text
ob.createFulltextIndex("articles", "ft_content", "content", FtsParser.IK);
// SQL: ALTER TABLE articles ADD FULLTEXT INDEX ft_content(`content`) WITH PARSER ik
```

### Example 3: NGRAM Parser

```java
// Create full-text index with NGRAM parser
ob.createFulltextIndex("articles", "ft_title", "title", FtsParser.NGRAM);
// SQL: ALTER TABLE articles ADD FULLTEXT INDEX ft_title(`title`) WITH PARSER ngram
```

### Example 4: Multiple Fields with Parser

```java
import com.oceanbase.obvec_jdbc.FtsIndexParam;
import java.util.Arrays;

// Create full-text index on multiple fields with parser
FtsIndexParam ftsParam = new FtsIndexParam(
    "ft_title_content", 
    Arrays.asList("title", "content"), 
    FtsParser.NGRAM
);
ob.createFulltextIndex("articles", ftsParam);
// SQL: ALTER TABLE articles ADD FULLTEXT INDEX ft_title_content(`title`, `content`) WITH PARSER ngram
```

### Example 5: Custom Parser

```java
// Create full-text index with custom parser
ob.createFulltextIndex("articles", "ft_title", "title", "thai_ftparser");
// SQL: ALTER TABLE articles ADD FULLTEXT INDEX ft_title(`title`) WITH PARSER thai_ftparser
```

### Example 6: Using FtsIndexParam Directly

```java
import com.oceanbase.obvec_jdbc.FtsIndexParam;
import java.util.Arrays;

// Single field with enum parser
FtsIndexParam ftsParam1 = new FtsIndexParam("ft_title", "title", FtsParser.IK);
ob.createFulltextIndex("articles", ftsParam1);

// Multiple fields with enum parser
FtsIndexParam ftsParam2 = new FtsIndexParam(
    "ft_multi", 
    Arrays.asList("title", "content", "summary"), 
    FtsParser.NGRAM2
);
ob.createFulltextIndex("articles", ftsParam2);

// Custom parser
FtsIndexParam ftsParam3 = new FtsIndexParam(
    "ft_custom", 
    "title", 
    "custom_parser_name"
);
ob.createFulltextIndex("articles", ftsParam3);
```

### Example 7: Complete Hybrid Search Setup

```java
import com.oceanbase.obvec_jdbc.*;
import java.util.Arrays;

ObVecClient ob = new ObVecClient(uri, user, password);
String tableName = "hybrid_search_table";

// Create table schema
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

// Vector index
IndexParams indexParams = new IndexParams();
IndexParam vecIndex = new IndexParam("vidx_embedding", "embedding");
indexParams.addIndex(vecIndex);
schema.setIndexParams(indexParams);

ob.createCollection(tableName, schema);

// Create full-text indexes with appropriate parsers
ob.createFulltextIndex(tableName, "ft_title", "title", FtsParser.NGRAM);
ob.createFulltextIndex(tableName, "ft_content", "content", FtsParser.NGRAM);
```

## Best Practices

### Parser Selection

1. **SPACE (default)**: Use for general English text or when parser doesn't matter
2. **IK**: Use for Chinese text when you need word-level segmentation
3. **NGRAM**: Use for Asian languages or when you need prefix/suffix matching
4. **NGRAM2**: Use for Asian languages if your OceanBase version supports it (V4.3.5 BP2+)
5. **BASIC_ENGLISH**: Use for English text when you need language-specific features
6. **JIEBA**: Use for Chinese text when you prefer Jieba segmentation over IK

### Multi-Field Indexes

- Create separate indexes for different fields if they need different parsers
- Use multi-field indexes when fields share the same parser and search together

### Performance Considerations

- NGRAM2 generally performs better than NGRAM
- IK and JIEBA parsers require more memory than NGRAM
- Consider your data characteristics when choosing parsers

## API Reference

### FtsParser Enum

```java
public enum FtsParser {
    SPACE,          // Default (empty string)
    IK,             // "ik"
    NGRAM,          // "ngram"
    NGRAM2,         // "ngram2"
    BASIC_ENGLISH,  // "beng"
    JIEBA           // "jieba"
}
```

### FtsIndexParam Class

#### Constructors

```java
// Default Space parser, single field
FtsIndexParam(String indexName, String fieldName)

// Default Space parser, multiple fields
FtsIndexParam(String indexName, List<String> fieldNames)

// With FtsParser enum, single field
FtsIndexParam(String indexName, String fieldName, FtsParser parserType)

// With FtsParser enum, multiple fields
FtsIndexParam(String indexName, List<String> fieldNames, FtsParser parserType)

// With custom parser name, single field
FtsIndexParam(String indexName, String fieldName, String customParserName)

// With custom parser name, multiple fields
FtsIndexParam(String indexName, List<String> fieldNames, String customParserName)
```

#### Methods

```java
String getIndexName()
List<String> getFieldNames()
FtsParser getParserType()
String getCustomParserName()
String getParserNameString()  // Returns parser name for SQL, null if default
boolean isDefaultParser()
```

### ObVecClient Methods

```java
// Default Space parser
void createFulltextIndex(String table_name, String index_name, String column_name)

// With FtsParser enum
void createFulltextIndex(String table_name, String index_name, String column_name, FtsParser parserType)

// With custom parser name
void createFulltextIndex(String table_name, String index_name, String column_name, String customParserName)

// With FtsIndexParam (supports multiple fields)
void createFulltextIndex(String table_name, FtsIndexParam ftsParam)
```

## SQL Examples

The following SQL statements are generated by the API:

```sql
-- Default Space parser (single field)
ALTER TABLE articles ADD FULLTEXT INDEX ft_title(`title`);

-- IK parser (single field)
ALTER TABLE articles ADD FULLTEXT INDEX ft_content(`content`) WITH PARSER ik;

-- NGRAM parser (multiple fields)
ALTER TABLE articles ADD FULLTEXT INDEX ft_multi(`title`, `content`) WITH PARSER ngram;

-- Custom parser
ALTER TABLE articles ADD FULLTEXT INDEX ft_custom(`title`) WITH PARSER thai_ftparser;
```

## References

- [OceanBase Full-Text Search Documentation](https://www.oceanbase.com/docs/common-oceanbase-database-standalone-1000000003577789)
- [pyobvector FtsIndexParam Reference](https://github.com/oceanbase/pyobvector)

