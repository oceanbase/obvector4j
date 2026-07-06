package com.oceanbase.obvector4j.hybrid.core;

import com.oceanbase.obvector4j.hybrid.AbstractHybridSearchBuilder;
import com.oceanbase.obvector4j.hybrid.core.dsl.HybridDslExpr;
import com.oceanbase.obvector4j.hybrid.core.dsl.HybridDslKnn;
import com.oceanbase.obvector4j.hybrid.core.dsl.HybridDslRank;
import com.oceanbase.obvector4j.schema.DataType;
import com.oceanbase.obvector4j.ObVecClient;
import com.oceanbase.obvector4j.model.Sqlizable;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Builder for HYBRID_SEARCH with custom DSL sections (OceanBase 4.6.0+ only).
 */
public class HybridSearchCustomBuilder extends AbstractHybridSearchBuilder<HybridSearchCustomBuilder> {

    private final HybridSearchDsl dsl = HybridSearchDsl.create();

    public HybridSearchCustomBuilder(ObVecClient client) {
        super(client);
    }

    public HybridSearchCustomBuilder table(String tableName) {
        this.tableName = tableName;
        return this;
    }

    /**
     * Complete DSL JSON passed directly to HYBRID_SEARCH.
     */
    public HybridSearchCustomBuilder dsl(String dslJson) {
        dsl.rawDsl(dslJson);
        return this;
    }

    public HybridSearchCustomBuilder dsl(HybridSearchDsl dslDocument) {
        if (dslDocument == null) {
            throw new IllegalArgumentException("DSL document cannot be null");
        }
        return dsl(dslDocument.toJsonString());
    }

    /**
     * Typed {@code query} section (no raw JSON string).
     */
    public HybridSearchCustomBuilder query(HybridDslExpr queryExpression) {
        dsl.query(queryExpression);
        return this;
    }

    /**
     * Custom {@code query} section as JSON string (escape hatch).
     */
    public HybridSearchCustomBuilder queryDsl(String queryExpressionJson) {
        dsl.query(queryExpressionJson);
        return this;
    }

    /**
     * Typed single-path {@code knn} section.
     */
    public HybridSearchCustomBuilder knn(HybridDslKnn knnExpression) {
        dsl.knn(knnExpression);
        return this;
    }

    /**
     * Typed multi-path {@code knn} section.
     */
    public HybridSearchCustomBuilder knn(HybridDslKnn... knnExpressions) {
        dsl.knn(knnExpressions);
        return this;
    }

    /**
     * Custom {@code knn} section as JSON string (escape hatch).
     */
    public HybridSearchCustomBuilder knnDsl(String knnExpressionJson) {
        dsl.knn(knnExpressionJson);
        return this;
    }

    /**
     * Typed {@code rank} section (RRF).
     */
    public HybridSearchCustomBuilder rank(HybridDslRank rankExpression) {
        dsl.rank(rankExpression);
        return this;
    }

    /**
     * Custom {@code rank} section as JSON string (escape hatch).
     */
    public HybridSearchCustomBuilder rankDsl(String rankExpressionJson) {
        dsl.rank(rankExpressionJson);
        return this;
    }

    public HybridSearchCustomBuilder mergeDsl(String topLevelDslJson) {
        dsl.merge(topLevelDslJson);
        return this;
    }

    public HybridSearchCustomBuilder from(int from) {
        dsl.from(from);
        return this;
    }

    public HybridSearchCustomBuilder size(int size) {
        dsl.size(size);
        return this;
    }

    public HybridSearchCustomBuilder minScore(double minScore) {
        dsl.minScore(minScore);
        return this;
    }

    public HybridSearchCustomBuilder outputField(String fieldName) {
        this.outputFields = new String[]{fieldName};
        this.outputDataTypes = null;
        return this;
    }

    public HybridSearchCustomBuilder outputField(String fieldName, DataType dataType) {
        this.outputFields = new String[]{fieldName};
        this.outputDataTypes = new DataType[]{dataType};
        return this;
    }

    public HybridSearchCustomBuilder outputFields(String... fieldNames) {
        this.outputFields = fieldNames;
        this.outputDataTypes = null;
        return this;
    }

    public HybridSearchCustomBuilder outputFields(String[] fieldNames, DataType[] dataTypes) {
        this.outputFields = fieldNames;
        this.outputDataTypes = dataTypes;
        return this;
    }

    /**
     * Returns the final DSL JSON without executing the search (for debugging or GET_SQL-style workflows).
     */
    public String buildDsl() {
        return dsl.toJsonString();
    }

    public ArrayList<HashMap<String, Sqlizable>> search() throws Throwable {
        if (tableName == null || tableName.isEmpty()) {
            throw new IllegalArgumentException("Table name cannot be empty");
        }
        if (outputFields == null || outputFields.length == 0) {
            throw new IllegalArgumentException("Output fields must be set");
        }
        HybridSearchSupport.require(client.getOceanBaseVersion());

        resolveOutputFields(null);
        return client.hybridSearchWithDsl(tableName, dsl.toJsonString(), outputFields, outputDataTypes);
    }
}
