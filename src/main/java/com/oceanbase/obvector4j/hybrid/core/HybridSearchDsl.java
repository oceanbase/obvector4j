/*
 * Copyright (c) 2024 OceanBase. All rights reserved.
 *
 * obvector4j is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 *
 *     http://license.coscl.org.cn/MulanPSL2
 *
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
 * EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
 * MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 *
 * See the Mulan PSL v2 for more details.
 */

package com.oceanbase.obvector4j.hybrid.core;

import com.oceanbase.obvector4j.hybrid.core.dsl.HybridDslExpr;
import com.oceanbase.obvector4j.hybrid.core.dsl.HybridDslKeys;
import com.oceanbase.obvector4j.hybrid.core.dsl.HybridDslKnn;
import com.oceanbase.obvector4j.hybrid.core.dsl.HybridDslRank;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Mutable HYBRID_SEARCH DSL document for OceanBase 4.6.0+.
 * Supports full JSON override or per-section customization of query / knn / rank / from / size / min_score.
 */
public final class HybridSearchDsl {

    private String rawDsl;
    private Object query;
    private Object knn;
    private JSONObject rank;
    private Integer from;
    private Integer size;
    private Double minScore;

    private HybridSearchDsl() {
    }

    public static HybridSearchDsl create() {
        return new HybridSearchDsl();
    }

    /**
     * Use a complete DSL JSON string as-is (ignores other fields set on this builder).
     */
    public HybridSearchDsl rawDsl(String dslJson) {
        if (dslJson == null || dslJson.trim().isEmpty()) {
            throw new IllegalArgumentException("DSL JSON cannot be empty");
        }
        this.rawDsl = dslJson.trim();
        return this;
    }

    /**
     * Set the top-level {@code query} expression, e.g. {@code {"match":{"content":"keyword"}}}.
     */
    public HybridSearchDsl query(String queryExpressionJson) {
        this.query = parseJsonValue(queryExpressionJson, "query");
        return this;
    }

    public HybridSearchDsl query(HybridDslExpr queryExpression) {
        if (queryExpression == null) {
            throw new IllegalArgumentException("query expression cannot be null");
        }
        this.query = queryExpression.toJson();
        return this;
    }

    public HybridSearchDsl query(JSONObject queryExpression) {
        if (queryExpression == null || queryExpression.isEmpty()) {
            throw new IllegalArgumentException("query expression cannot be empty");
        }
        this.query = queryExpression;
        return this;
    }

    /**
     * Set the top-level {@code knn} expression (object or array for multi-path vector search).
     */
    public HybridSearchDsl knn(String knnExpressionJson) {
        this.knn = parseJsonValue(knnExpressionJson, "knn");
        return this;
    }

    public HybridSearchDsl knn(HybridDslKnn knnExpression) {
        if (knnExpression == null) {
            throw new IllegalArgumentException("knn expression cannot be null");
        }
        this.knn = knnExpression.toJson();
        return this;
    }

    public HybridSearchDsl knn(HybridDslKnn... knnExpressions) {
        if (knnExpressions == null || knnExpressions.length == 0) {
            throw new IllegalArgumentException("knn expressions cannot be empty");
        }
        if (knnExpressions.length == 1) {
            this.knn = knnExpressions[0].toJson();
        } else {
            JSONArray multi = new JSONArray();
            for (HybridDslKnn knnExpression : knnExpressions) {
                multi.add(knnExpression.toJson());
            }
            this.knn = multi;
        }
        return this;
    }

    public HybridSearchDsl knn(JSONObject knnExpression) {
        if (knnExpression == null || knnExpression.isEmpty()) {
            throw new IllegalArgumentException("knn expression cannot be empty");
        }
        this.knn = knnExpression;
        return this;
    }

    public HybridSearchDsl knn(JSONArray multiKnnExpressions) {
        if (multiKnnExpressions == null || multiKnnExpressions.isEmpty()) {
            throw new IllegalArgumentException("knn array cannot be empty");
        }
        this.knn = multiKnnExpressions;
        return this;
    }

    /**
     * Set the top-level {@code rank} expression, e.g. {@code {"rrf":{"rank_constant":60,"rank_window_size":10}}}.
     */
    public HybridSearchDsl rank(String rankExpressionJson) {
        Object parsed = parseJsonValue(rankExpressionJson, "rank");
        if (!(parsed instanceof JSONObject)) {
            throw new IllegalArgumentException("rank expression must be a JSON object");
        }
        this.rank = (JSONObject) parsed;
        return this;
    }

    public HybridSearchDsl rank(HybridDslRank rankExpression) {
        if (rankExpression == null) {
            throw new IllegalArgumentException("rank expression cannot be null");
        }
        this.rank = rankExpression.toJson();
        return this;
    }

    public HybridSearchDsl rank(JSONObject rankExpression) {
        if (rankExpression == null || rankExpression.isEmpty()) {
            throw new IllegalArgumentException("rank expression cannot be empty");
        }
        this.rank = rankExpression;
        return this;
    }

    public HybridSearchDsl from(int from) {
        if (from < 0) {
            throw new IllegalArgumentException("from must be >= 0");
        }
        this.from = from;
        return this;
    }

    public HybridSearchDsl size(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("size must be > 0");
        }
        this.size = size;
        return this;
    }

    public HybridSearchDsl minScore(double minScore) {
        this.minScore = minScore;
        return this;
    }

    /**
     * Merge additional top-level DSL keys from JSON (later keys override existing ones).
     */
    public HybridSearchDsl merge(String topLevelDslJson) {
        Object parsed = parseJsonValue(topLevelDslJson, "merge");
        if (!(parsed instanceof JSONObject)) {
            throw new IllegalArgumentException("merge JSON must be a top-level object");
        }
        JSONObject extra = (JSONObject) parsed;
        if (extra.containsKey("query")) {
            this.query = extra.get("query");
        }
        if (extra.containsKey("knn")) {
            this.knn = extra.get("knn");
        }
        if (extra.containsKey("rank")) {
            Object rankVal = extra.get("rank");
            if (!(rankVal instanceof JSONObject)) {
                throw new IllegalArgumentException("rank must be a JSON object");
            }
            this.rank = (JSONObject) rankVal;
        }
        if (extra.containsKey("from")) {
            this.from = toInt(extra.get("from"), "from");
        }
        if (extra.containsKey("size")) {
            this.size = toInt(extra.get("size"), "size");
        }
        if (extra.containsKey("min_score")) {
            this.minScore = toDouble(extra.get("min_score"), "min_score");
        }
        return this;
    }

    public String toJsonString() {
        if (rawDsl != null) {
            return rawDsl;
        }
        if (query == null && knn == null) {
            throw new IllegalStateException("HYBRID_SEARCH DSL requires at least query or knn");
        }

        JSONObject dsl = new JSONObject();
        if (query != null) {
            dsl.put(HybridDslKeys.QUERY, query);
        }
        if (knn != null) {
            dsl.put(HybridDslKeys.KNN, knn);
        }
        if (rank != null) {
            dsl.put(HybridDslKeys.RANK, rank);
        }
        if (from != null) {
            dsl.put(HybridDslKeys.FROM, from);
        }
        if (size != null) {
            dsl.put(HybridDslKeys.SIZE, size);
        }
        if (minScore != null) {
            dsl.put(HybridDslKeys.MIN_SCORE, minScore);
        }
        return dsl.toJSONString();
    }

    private static Object parseJsonValue(String json, String fieldName) {
        if (json == null || json.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " JSON cannot be empty");
        }
        try {
            return new JSONParser().parse(json.trim());
        } catch (ParseException e) {
            throw new IllegalArgumentException("Invalid " + fieldName + " JSON: " + e.getMessage(), e);
        }
    }

    private static int toInt(Object value, String fieldName) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        throw new IllegalArgumentException(fieldName + " must be a number");
    }

    private static double toDouble(Object value, String fieldName) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        throw new IllegalArgumentException(fieldName + " must be a number");
    }
}
