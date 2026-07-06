package com.oceanbase.obvector4j.hybrid.core;

import com.oceanbase.obvector4j.ObVecClient;
import com.oceanbase.obvector4j.hybrid.core.dsl.HybridDsl;
import com.oceanbase.obvector4j.schema.DataType;
import com.oceanbase.obvector4j.model.Sqlizable;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Entry point for OceanBase 4.6.0+ {@code HYBRID_SEARCH} DSL syntax.
 * Obtain via {@link ObVecClient#hybridSearch()} — throws if the cluster is below 4.6.0.
 */
public final class HybridSearch {

    private final ObVecClient client;

    public HybridSearch(ObVecClient client) {
        this.client = client;
    }

    /** Start building a custom DSL document ({@link HybridSearchDsl}). */
    public HybridSearchDsl dsl() {
        return HybridSearchDsl.create();
    }

    /** Fluent builder for custom {@code query} / {@code knn} / {@code rank} sections. */
    public HybridSearchCustomBuilder customSearch() {
        return new HybridSearchCustomBuilder(client);
    }

    /**
     * Execute HYBRID_SEARCH with a complete DSL JSON string.
     *
     * @see HybridDsl
     * @see HybridSearchDsl#toJsonString()
     */
    public ArrayList<HashMap<String, Sqlizable>> searchWithDsl(
            String tableName,
            String dslJson,
            String[] outputFields,
            DataType[] outputDataTypes) throws Throwable {
        return client.hybridSearchWithDsl(tableName, dslJson, outputFields, outputDataTypes);
    }
}
