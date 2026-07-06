/**
 * Hybrid search: legacy compatibility path (&lt; 4.6.0) and shared builder base classes.
 *
 * <p>OceanBase 4.6.0+ {@code HYBRID_SEARCH} DSL syntax lives in
 * {@link com.oceanbase.obvector4j.hybrid.core} — use {@link com.oceanbase.obvector4j.ObVecClient#hybridSearch()}.
 *
 * <p>Version-adaptive builders: {@link com.oceanbase.obvector4j.hybrid.HybridTextVectorSearchBuilder},
 * {@link com.oceanbase.obvector4j.hybrid.HybridScalarVectorSearchBuilder}.
 */
package com.oceanbase.obvector4j.hybrid;
