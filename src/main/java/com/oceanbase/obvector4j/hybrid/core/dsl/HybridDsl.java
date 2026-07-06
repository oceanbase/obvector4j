package com.oceanbase.obvector4j.hybrid.core.dsl;

import com.oceanbase.obvector4j.filter.Filter;
import com.oceanbase.obvector4j.filter.FilterMapper;
import com.oceanbase.obvector4j.hybrid.core.HybridSearchDsl;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Entry point for building OceanBase 4.6.0+ HYBRID_SEARCH DSL from {@link HybridDslKeys}.
 *
 * <pre>{@code
 * HybridSearchDsl.create()
 *     .query(HybridDsl.match("content", "keyword"))
 *     .knn(HybridDsl.knn("embedding", vector, 10))
 *     .rank(HybridDsl.rrf(10))
 *     .size(5)
 *     .toJsonString();
 *
 * // Or compose directly from keywords:
 * HybridDslNode.field(HybridDslKeys.MATCH, "content")
 *     .param(HybridDslKeys.QUERY_PARAM, "python")
 *     .param(HybridDslKeys.BOOST, 0.3);
 * }</pre>
 */
public final class HybridDsl {

    private HybridDsl() {
    }

    // ── full-text ─────────────────────────────────────────────────

    /** {@code {"match":{"field":"query"}}} */
    public static HybridDslExpr match(String field, String query) {
        HybridDslNode.requireText(query, HybridDslKeys.QUERY_PARAM);
        return HybridDslNode.field(HybridDslKeys.MATCH, field, query);
    }

    /** {@code {"match":{"field":{"query":..., "boost":...}}}} */
    public static HybridDslNode match(String field) {
        return HybridDslNode.field(HybridDslKeys.MATCH, field);
    }

    /** {@code {"multi_match":{...}}} */
    public static HybridDslExpr multiMatch(String[] fields, String query) {
        HybridDslNode.requireText(query, HybridDslKeys.QUERY_PARAM);
        return multiMatch(fields).param(HybridDslKeys.QUERY_PARAM, query);
    }

    public static HybridDslNode multiMatch(String[] fields) {
        HybridDslNode.requireNonEmpty(fields, HybridDslKeys.FIELDS);
        JSONArray fieldArray = new JSONArray();
        HybridDslNode.addFields(fieldArray, fields);
        return HybridDslNode.of(HybridDslKeys.MULTI_MATCH)
                .param(HybridDslKeys.FIELDS, fieldArray);
    }

    /** {@code {"query_string":{...}}} builder */
    public static HybridDslNode queryString(String[] fields) {
        HybridDslNode.requireNonEmpty(fields, HybridDslKeys.FIELDS);
        JSONArray fieldArray = new JSONArray();
        HybridDslNode.addFields(fieldArray, fields);
        return HybridDslNode.of(HybridDslKeys.QUERY_STRING)
                .param(HybridDslKeys.FIELDS, fieldArray);
    }

    /** {@code {"query_string":{...}}} with query text */
    public static HybridDslExpr queryString(String[] fields, String query) {
        HybridDslNode.requireText(query, HybridDslKeys.QUERY_PARAM);
        return queryString(fields).param(HybridDslKeys.QUERY_PARAM, query);
    }

    /** {@code {"match_phrase":{"field":"query"}}} or with slop */
    public static HybridDslExpr matchPhrase(String field, String query) {
        return matchPhrase(field, query, null);
    }

    public static HybridDslExpr matchPhrase(String field, String query, Integer slop) {
        HybridDslNode.requireText(query, HybridDslKeys.QUERY_PARAM);
        HybridDslNode node = HybridDslNode.field(HybridDslKeys.MATCH_PHRASE, field);
        node.param(HybridDslKeys.QUERY_PARAM, query);
        if (slop != null) {
            node.param(HybridDslKeys.SLOP, slop);
        }
        return node;
    }

    // ── scalar ────────────────────────────────────────────────────

    /** {@code {"term":{"field":value}}} */
    public static HybridDslExpr term(String field, Object value) {
        return HybridDslNode.field(HybridDslKeys.TERM, field, value);
    }

    /** {@code {"range":{"field":{"gte":...}}}} */
    public static HybridDslNode range(String field) {
        return HybridDslNode.range(field);
    }

    /** {@code {"terms":{"field":[v1,v2]}}} */
    public static HybridDslExpr terms(String field, Object... values) {
        HybridDslNode.requireNonEmpty(values, "terms values");
        return HybridDslNode.field(HybridDslKeys.TERMS, field, HybridDslNode.toArray(values));
    }

    // ── json / array ────────────────────────────────────────────────

    public static HybridDslExpr jsonContains(String field, Object candidate) {
        return jsonContains(field, candidate, null);
    }

    public static HybridDslExpr jsonContains(String field, Object candidate, String path) {
        return HybridDslNode.jsonOp(HybridDslKeys.JSON_CONTAINS, field, candidate, path);
    }

    public static HybridDslExpr jsonOverlaps(String field, Object candidate) {
        return jsonOverlaps(field, candidate, null);
    }

    public static HybridDslExpr jsonOverlaps(String field, Object candidate, String path) {
        return HybridDslNode.jsonOp(HybridDslKeys.JSON_OVERLAPS, field, candidate, path);
    }

    public static HybridDslExpr jsonMemberOf(String field, Object candidate) {
        return jsonMemberOf(field, candidate, null);
    }

    public static HybridDslExpr jsonMemberOf(String field, Object candidate, String path) {
        return HybridDslNode.jsonOp(HybridDslKeys.JSON_MEMBER_OF, field, candidate, path);
    }

    public static HybridDslExpr arrayContains(String field, Object value) {
        return HybridDslNode.field(HybridDslKeys.ARRAY_CONTAINS, field, value);
    }

    public static HybridDslExpr arrayContainsAll(String field, Object... values) {
        HybridDslNode.requireNonEmpty(values, "array_contains_all values");
        return HybridDslNode.field(HybridDslKeys.ARRAY_CONTAINS_ALL, field, HybridDslNode.toArray(values));
    }

    public static HybridDslExpr arrayOverlaps(String field, Object... values) {
        HybridDslNode.requireNonEmpty(values, "array_overlaps values");
        return HybridDslNode.field(HybridDslKeys.ARRAY_OVERLAPS, field, HybridDslNode.toArray(values));
    }

    // ── bool ──────────────────────────────────────────────────────

    /** {@code {"bool":{...}}} */
    public static HybridDslNode bool() {
        return HybridDslNode.bool();
    }

    // ── knn / rank ──────────────────────────────────────────────────

    public static HybridDslKnn knn(String field, float[] queryVector, int k) {
        return HybridDslKnn.of(field, queryVector, k);
    }

    public static HybridDslRank rrf(int rankWindowSize) {
        return rrf(rankWindowSize, 60);
    }

    public static HybridDslRank rrf(int rankWindowSize, int rankConstant) {
        return HybridDslRank.rrf(rankWindowSize, rankConstant);
    }

    public static HybridDslRank weightedSum(String normalizer) {
        return weightedSum(normalizer, 0);
    }

    public static HybridDslRank weightedSum(String normalizer, int rankWindowSize) {
        return HybridDslRank.weightedSum(normalizer, rankWindowSize);
    }

    /** Text + vector + RRF (document hybrid search pattern). */
    public static HybridSearchDsl textVectorRrf(
            HybridDslExpr textQuery,
            HybridDslKnn vectorQuery,
            int size,
            int rankWindowSize,
            int rankConstant) {
        return HybridSearchDsl.create()
                .query(textQuery)
                .knn(vectorQuery)
                .rank(rrf(rankWindowSize, rankConstant))
                .size(size);
    }

    /** Text + vector + weighted_sum minmax normalization (document §归一化). */
    public static HybridSearchDsl textVectorWeightedSum(
            HybridDslExpr textQuery,
            HybridDslKnn vectorQuery,
            int size,
            int rankWindowSize) {
        return HybridSearchDsl.create()
                .query(textQuery)
                .knn(vectorQuery)
                .rank(weightedSum(HybridDslKeys.Normalizer.MINMAX, rankWindowSize))
                .size(size);
    }

    /** Low-level: wrap any query keyword into a DSL expression node. */
    public static HybridDslNode node(String keyword) {
        return HybridDslNode.of(keyword);
    }

    public static String formatVector(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(vector[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    static void addAll(JSONArray target, HybridDslExpr[] exprs) {
        if (exprs == null) {
            return;
        }
        for (HybridDslExpr expr : exprs) {
            if (expr != null) {
                target.add(expr.toJson());
            }
        }
    }

    static void addFilterConditions(JSONArray target, Filter filter) {
        JSONArray conditions = FilterMapper.toFilterConditionList(filter);
        if (conditions != null) {
            target.addAll(conditions);
        }
    }
}
