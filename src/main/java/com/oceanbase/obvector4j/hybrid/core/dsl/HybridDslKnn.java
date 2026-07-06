package com.oceanbase.obvector4j.hybrid.core.dsl;

import com.oceanbase.obvector4j.filter.Filter;
import com.oceanbase.obvector4j.filter.FilterMapper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * {@code knn} section for HYBRID_SEARCH DSL (single path).
 */
public final class HybridDslKnn {

    private final JSONObject knn = new JSONObject();

    private HybridDslKnn(String field, float[] queryVector, int k) {
        knn.put(HybridDslKeys.FIELD, field);
        knn.put(HybridDslKeys.K, k);
        knn.put(HybridDslKeys.QUERY_VECTOR, HybridDsl.formatVector(queryVector));
    }

    public static HybridDslKnn of(String field, float[] queryVector, int k) {
        HybridDslNode.requireField(field);
        if (queryVector == null || queryVector.length == 0) {
            throw new IllegalArgumentException("queryVector cannot be empty");
        }
        if (k <= 0) {
            throw new IllegalArgumentException("k must be > 0");
        }
        return new HybridDslKnn(field, queryVector, k);
    }

    public HybridDslKnn filter(Filter filterObj) {
        JSONArray conditions = FilterMapper.toFilterConditionList(filterObj);
        if (conditions != null && !conditions.isEmpty()) {
            knn.put(HybridDslKeys.FILTER, conditions);
        }
        return this;
    }

    public HybridDslKnn filter(HybridDslExpr... exprs) {
        JSONArray arr = new JSONArray();
        HybridDsl.addAll(arr, exprs);
        if (!arr.isEmpty()) {
            knn.put(HybridDslKeys.FILTER, arr);
        }
        return this;
    }

    /**
     * Single filter object form, e.g. {@code "filter": {"bool": {...}}} per spec.
     */
    public HybridDslKnn filterObject(HybridDslExpr filterRoot) {
        if (filterRoot != null) {
            knn.put(HybridDslKeys.FILTER, filterRoot.toJson());
        }
        return this;
    }

    /** {@code filter} as a {@code bool} wrapper (common knn pre-filter pattern). */
    public HybridDslKnn filterBool(HybridDslNode boolQuery) {
        return filterObject(boolQuery);
    }

    public HybridDslKnn boost(double boost) {
        knn.put(HybridDslKeys.BOOST, boost);
        return this;
    }

    public HybridDslKnn similarity(double similarity) {
        knn.put(HybridDslKeys.SIMILARITY, similarity);
        return this;
    }

    public HybridDslKnn efSearch(int efSearch) {
        return searchOption(HybridDslKeys.EF_SEARCH, efSearch);
    }

    public HybridDslKnn refineK(double refineK) {
        return searchOption(HybridDslKeys.REFINE_K, refineK);
    }

    /** @see HybridDslKeys.FilterMode */
    public HybridDslKnn filterMode(String filterMode) {
        return searchOption(HybridDslKeys.FILTER_MODE, filterMode);
    }

    public JSONObject toJson() {
        return knn;
    }

    private HybridDslKnn searchOption(String key, Object value) {
        JSONObject options = (JSONObject) knn.get(HybridDslKeys.SEARCH_OPTIONS);
        if (options == null) {
            options = new JSONObject();
            knn.put(HybridDslKeys.SEARCH_OPTIONS, options);
        }
        options.put(key, value);
        return this;
    }
}
