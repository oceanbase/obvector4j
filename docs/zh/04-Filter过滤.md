# 04 · Filter 过滤条件

> 阅读顺序：[00-文档目录](00-文档目录.md) → … → [03-混合搜索](03-混合搜索.md) → **04 Filter 过滤**  
> English: [04-filter](../en/04-filter.md)

Filter API 用于在混合搜索中表达**标量过滤**（等于、范围、AND/OR 等），类型安全，推荐在 4.6.0+ 环境配合 `HYBRID_SEARCH` 使用。

---

## 目录

1. [快速开始](#快速开始)
2. [比较运算](#比较运算)
3. [逻辑组合](#逻辑组合)
4. [在混合搜索中使用](#在混合搜索中使用)
5. [与字符串 SQL 过滤对比](#与字符串-sql-过滤对比)
6. [最佳实践](#最佳实践)

---

## 快速开始

```java
import com.oceanbase.obvector4j.filter.Filter;
import com.oceanbase.obvector4j.filter.FilterBuilder;

Filter filter = FilterBuilder.and(
    FilterBuilder.key("category_id").isEqualTo(1),
    FilterBuilder.key("price").isGreaterThanOrEqualTo(100.0)
);

client.scalarVectorSearch()
    .table("products")
    .queryVector(queryVector)
    .filter(filter)
    .topk(10)
    .outputFields("id", "name", "price")
    .search();
```

## 比较运算

| 方法 | 含义 | 示例 |
|------|------|------|
| `isEqualTo(v)` | 等于 | `category_id = 1` |
| `isNotEqualTo(v)` | 不等于 | `status != 0` |
| `isGreaterThan(v)` | 大于 | `price > 100` |
| `isGreaterThanOrEqualTo(v)` | 大于等于 | `price >= 100` |
| `isLessThan(v)` | 小于 | `price < 1000` |
| `isLessThanOrEqualTo(v)` | 小于等于 | `price <= 1000` |
| `isIn(...)` | IN | `id IN (1,2,3)` |
| `isNotIn(...)` | NOT IN | `id NOT IN (4,5)` |
| `contains(s)` | 字符串包含 | `title LIKE '%词%'` |

```java
Filter f1 = FilterBuilder.key("category_id").isIn(1, 2, 3);
Filter f2 = FilterBuilder.key("title").contains("OceanBase");
```

## 逻辑组合

```java
// AND
Filter and = FilterBuilder.and(filterA, filterB);

// OR
Filter or = FilterBuilder.or(filterA, filterB);

// NOT
Filter not = FilterBuilder.not(filterA);

// 嵌套
Filter complex = FilterBuilder.and(
    FilterBuilder.key("category_id").isEqualTo(1),
    FilterBuilder.or(
        FilterBuilder.key("price").isLessThan(100.0),
        FilterBuilder.key("price").isGreaterThan(500.0)
    )
);
```

## 在混合搜索中使用

| 搜索类型 | 用法 |
|----------|------|
| 标量 + 向量 | `.filter(filter)` 必填或推荐 |
| 全文 + 向量 | `.filter(filter)` 可选 |

```java
client.textVectorSearch()
    .table("docs")
    .queryVector(vec)
    .textField("content")
    .textQuery("关键词")
    .filter(FilterBuilder.key("status").isEqualTo(1))
    .topk(10)
    .search();
```

## 与字符串 SQL 过滤对比

| 方式 | 4.6.0+ HYBRID_SEARCH | 兼容模式 |
|------|----------------------|----------|
| `Filter` 对象（**推荐**） | ✅ 转 DSL JSON | ✅ 转 SQL WHERE |
| `.filter("a = 1")` 字符串 | ❌ 回退兼容模式 | ✅ |

**建议**：统一使用 `FilterBuilder`，避免手写 SQL 片段。

## 最佳实践

1. 优先 `Filter` 对象，少用字符串 `.filter("...")`
2. 复杂条件用 `and` / `or` 嵌套，保持可读性
3. 字段名与表列名一致
4. 数值比较注意类型（`100.0` vs `100`）

完整 API 列表见英文版 [04-filter](../en/04-filter.md)。
