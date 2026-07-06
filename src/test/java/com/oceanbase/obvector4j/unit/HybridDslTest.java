package com.oceanbase.obvector4j.unit;

import com.oceanbase.obvector4j.filter.FilterBuilder;
import com.oceanbase.obvector4j.hybrid.core.HybridSearchDsl;
import com.oceanbase.obvector4j.hybrid.core.dsl.HybridDsl;
import com.oceanbase.obvector4j.hybrid.core.dsl.HybridDslExpr;
import com.oceanbase.obvector4j.hybrid.core.dsl.HybridDslKeys;
import com.oceanbase.obvector4j.hybrid.core.dsl.HybridDslNode;
import junit.framework.TestCase;
import org.json.simple.JSONArray;

public class HybridDslTest extends TestCase {

    public void testMatchAndKnnDsl() {
        float[] vector = {1.0f, 2.0f, 3.0f};
        String dsl = HybridSearchDsl.create()
                .query(HybridDsl.match("content", "oceanbase"))
                .knn(HybridDsl.knn("vector", vector, 5))
                .rank(HybridDsl.rrf(10, 60))
                .size(5)
                .toJsonString();

        assertTrue(dsl.contains("\"" + HybridDslKeys.MATCH + "\""));
        assertTrue(dsl.contains("\"content\":\"oceanbase\""));
        assertTrue(dsl.contains("\"" + HybridDslKeys.KNN + "\""));
        assertTrue(dsl.contains("\"" + HybridDslKeys.QUERY_VECTOR + "\":\"[1.0,2.0,3.0]\""));
        assertTrue(dsl.contains("\"" + HybridDslKeys.RRF + "\""));
        assertTrue(dsl.contains("\"" + HybridDslKeys.SIZE + "\":5"));
    }

    public void testBoolWithFilterAndRange() {
        float[] vector = {0.1f, 0.2f};
        String dsl = HybridSearchDsl.create()
                .query(HybridDsl.bool()
                        .must(HybridDsl.match("content", "mysql"))
                        .filter(
                                HybridDsl.term("status", 1),
                                HybridDsl.range("price").gte(50).lte(250)))
                .knn(HybridDsl.knn("embedding", vector, 10)
                        .filter(FilterBuilder.key("category_id").isEqualTo(1)))
                .size(10)
                .toJsonString();

        assertTrue(dsl.contains("\"" + HybridDslKeys.BOOL + "\""));
        assertTrue(dsl.contains("\"" + HybridDslKeys.MUST + "\""));
        assertTrue(dsl.contains("\"" + HybridDslKeys.FILTER + "\""));
        assertTrue(dsl.contains("\"" + HybridDslKeys.TERM + "\""));
        assertTrue(dsl.contains("\"" + HybridDslKeys.RANGE + "\""));
        assertTrue(dsl.contains("\"category_id\":1"));
    }

    public void testMultiMatchAndMultiKnn() {
        float[] v1 = {1f, 0f};
        float[] v2 = {0f, 1f};
        String dsl = HybridSearchDsl.create()
                .query(HybridDsl.multiMatch(new String[] {"title", "content"}, "keyword"))
                .knn(
                        HybridDsl.knn("vector_a", v1, 3),
                        HybridDsl.knn("vector_b", v2, 3))
                .toJsonString();

        assertTrue(dsl.contains("\"" + HybridDslKeys.MULTI_MATCH + "\""));
        assertTrue(dsl.contains("\"vector_a\""));
        assertTrue(dsl.contains("\"vector_b\""));
    }

    public void testCustomBuilderUsesTypedDsl() {
        String dsl = HybridSearchDsl.create()
                .knn(HybridDsl.knn("vec", new float[] {1f}, 2))
                .size(2)
                .toJsonString();
        assertTrue(dsl.contains("\"" + HybridDslKeys.FIELD + "\":\"vec\""));
        assertTrue(dsl.contains("\"" + HybridDslKeys.K + "\":2"));
        assertTrue(dsl.contains("\"" + HybridDslKeys.QUERY_VECTOR + "\":\"[1.0]\""));
        assertTrue(dsl.contains("\"" + HybridDslKeys.SIZE + "\":2"));
    }

    public void testMatchWithBoostAndWeightedSum() {
        String dsl = HybridSearchDsl.create()
                .query(HybridDsl.match("content")
                        .param(HybridDslKeys.QUERY_PARAM, "python javascript")
                        .param(HybridDslKeys.BOOST, 0.3))
                .knn(HybridDsl.knn("vector_col", new float[] {0.1f, 0.2f, 0.3f, 0.4f}, 5).boost(0.7))
                .rank(HybridDsl.weightedSum(HybridDslKeys.Normalizer.MINMAX, 10))
                .minScore(0.5)
                .size(5)
                .toJsonString();

        assertTrue(dsl.contains("\"" + HybridDslKeys.BOOST + "\":0.3"));
        assertTrue(dsl.contains("\"" + HybridDslKeys.WEIGHTED_SUM + "\""));
        assertTrue(dsl.contains("\"" + HybridDslKeys.NORMALIZER + "\":\"minmax\""));
        assertTrue(dsl.contains("\"" + HybridDslKeys.MIN_SCORE + "\":0.5"));
    }

    public void testJsonAndArrayFilters() {
        String dsl = HybridSearchDsl.create()
                .query(HybridDsl.bool()
                        .filter(
                                HybridDsl.jsonContains("doc_json", "{\"name\":\"doc2\"}", "$"),
                                HybridDsl.arrayContains("tags_array1", "ios"),
                                HybridDsl.arrayOverlaps("tags_array1", "database", "postgres")))
                .size(10)
                .toJsonString();

        assertTrue(dsl.contains("\"" + HybridDslKeys.JSON_CONTAINS + "\""));
        assertTrue(dsl.contains("\"" + HybridDslKeys.ARRAY_CONTAINS + "\""));
        assertTrue(dsl.contains("\"" + HybridDslKeys.ARRAY_OVERLAPS + "\""));
    }

    public void testKnnSearchOptionsAndRrf() {
        String dsl = HybridSearchDsl.create()
                .knn(HybridDsl.knn("vector_col", new float[] {0.1f, 0.2f}, 5)
                        .similarity(0.5)
                        .efSearch(64)
                        .refineK(4.0)
                        .filterMode(HybridDslKeys.FilterMode.PRE))
                .rank(HybridDsl.rrf(10, 60))
                .toJsonString();

        assertTrue(dsl.contains("\"" + HybridDslKeys.SEARCH_OPTIONS + "\""));
        assertTrue(dsl.contains("\"" + HybridDslKeys.EF_SEARCH + "\":64"));
        assertTrue(dsl.contains("\"" + HybridDslKeys.FILTER_MODE + "\":\"pre\""));
        assertTrue(dsl.contains("\"" + HybridDslKeys.RANK_CONSTANT + "\":60"));
    }

    public void testQueryStringAndBoolBoost() {
        String dsl = HybridSearchDsl.create()
                .query(HybridDsl.bool()
                        .should(
                                HybridDsl.match("title", "data"),
                                HybridDsl.multiMatch(new String[] {"title", "content"}, "python java"))
                        .filter(HybridDsl.range("id").gte(5))
                        .minimumShouldMatch(1)
                        .boost(1.2))
                .toJsonString();

        assertTrue(dsl.contains("\"" + HybridDslKeys.MULTI_MATCH + "\""));
        assertTrue(dsl.contains("\"" + HybridDslKeys.MINIMUM_SHOULD_MATCH + "\":1"));
        assertTrue(dsl.contains("\"" + HybridDslKeys.BOOST + "\":1.2"));
    }

    public void testComposeFromKeywordsDirectly() {
        HybridDslExpr expr = HybridDslNode.of(HybridDslKeys.MULTI_MATCH)
                .param(HybridDslKeys.FIELDS, new JSONArray() {{
                    add("title^0.3");
                    add("content^0.2");
                }})
                .param(HybridDslKeys.QUERY_PARAM, "python javascript")
                .param(HybridDslKeys.TYPE, HybridDslKeys.MultiMatchType.BEST_FIELDS)
                .param(HybridDslKeys.OPERATOR, HybridDslKeys.Operator.OR);

        String json = expr.toJson().toJSONString();
        assertTrue(json.contains("\"" + HybridDslKeys.MULTI_MATCH + "\""));
        assertTrue(json.contains("\"" + HybridDslKeys.TYPE + "\":\"best_fields\""));
    }
}
