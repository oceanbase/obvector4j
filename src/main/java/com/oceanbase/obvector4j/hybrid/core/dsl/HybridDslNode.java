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

package com.oceanbase.obvector4j.hybrid.core.dsl;

import com.oceanbase.obvector4j.filter.Filter;
import com.oceanbase.obvector4j.filter.FilterMapper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Universal HYBRID_SEARCH DSL node built from {@link HybridDslKeys} keywords.
 * <p>
 * Typical patterns:
 * <pre>{@code
 * // {"match":{"content":"keyword"}}
 * HybridDslNode.field(HybridDslKeys.MATCH, "content", "keyword")
 *
 * // {"match":{"content":{"query":"kw","boost":0.3}}}
 * HybridDslNode.field(HybridDslKeys.MATCH, "content")
 *     .param(HybridDslKeys.QUERY_PARAM, "kw").param(HybridDslKeys.BOOST, 0.3)
 *
 * // {"bool":{"must":[...],"filter":[...]}}
 * HybridDslNode.bool().must(...).filter(...)
 * }</pre>
 */
public final class HybridDslNode implements HybridDslExpr {

    private final String keyword;
    private final JSONObject body;
    private JSONObject fieldParams;

    private HybridDslNode(String keyword, JSONObject body) {
        this.keyword = keyword;
        this.body = body;
    }

    // ── factories ─────────────────────────────────────────────────

    /** {@code {"keyword": {field: value}}} */
    public static HybridDslNode field(String keyword, String field, Object value) {
        requireKeyword(keyword);
        requireField(field);
        JSONObject inner = new JSONObject();
        inner.put(field.trim(), value);
        return new HybridDslNode(keyword, inner);
    }

    /** {@code {"keyword": {field: {params...}}}} — fluent param builder */
    public static HybridDslNode field(String keyword, String field) {
        requireKeyword(keyword);
        requireField(field);
        HybridDslNode node = new HybridDslNode(keyword, new JSONObject());
        node.fieldParams = new JSONObject();
        node.body.put(field.trim(), node.fieldParams);
        return node;
    }

    /** {@code {"keyword": {bodyKey: bodyValue}}} e.g. multi_match, query_string */
    public static HybridDslNode of(String keyword) {
        requireKeyword(keyword);
        return new HybridDslNode(keyword, new JSONObject());
    }

    /** {@code {"bool": {...}}} */
    public static HybridDslNode bool() {
        return new HybridDslNode(HybridDslKeys.BOOL, new JSONObject());
    }

    /** {@code {"range": {field: {gte:..., lte:...}}}} */
    public static HybridDslNode range(String field) {
        requireField(field);
        HybridDslNode node = new HybridDslNode(HybridDslKeys.RANGE, new JSONObject());
        node.fieldParams = new JSONObject();
        node.body.put(field.trim(), node.fieldParams);
        return node;
    }

    /** json_contains / json_overlaps / json_member_of */
    public static HybridDslNode jsonOp(String keyword, String field, Object candidate, String path) {
        JSONObject params = new JSONObject();
        params.put(HybridDslKeys.CANDIDATE, candidate);
        if (path != null && !path.isEmpty()) {
            params.put(HybridDslKeys.PATH, path);
        }
        return field(keyword, field, params);
    }

    // ── fluent setters ────────────────────────────────────────────

    public HybridDslNode param(String key, Object value) {
        if (fieldParams != null) {
            fieldParams.put(key, value);
        } else {
            body.put(key, value);
        }
        return this;
    }

    /** Shorthand for {@link HybridDslKeys#QUERY_PARAM}. */
    public HybridDslNode query(String queryText) {
        return param(HybridDslKeys.QUERY_PARAM, queryText);
    }

    /** Shorthand for {@link HybridDslKeys#OPERATOR}. */
    public HybridDslNode operator(String operator) {
        return param(HybridDslKeys.OPERATOR, operator);
    }

    /** Shorthand for {@link HybridDslKeys#TYPE}. */
    public HybridDslNode type(String type) {
        return param(HybridDslKeys.TYPE, type);
    }

    /** Shorthand for {@link HybridDslKeys#DEFAULT_OPERATOR}. */
    public HybridDslNode defaultOperator(String defaultOperator) {
        return param(HybridDslKeys.DEFAULT_OPERATOR, defaultOperator);
    }

    public HybridDslNode gte(Number value) {
        return bound(HybridDslKeys.GTE, value);
    }

    public HybridDslNode gt(Number value) {
        return bound(HybridDslKeys.GT, value);
    }

    public HybridDslNode lte(Number value) {
        return bound(HybridDslKeys.LTE, value);
    }

    public HybridDslNode lt(Number value) {
        return bound(HybridDslKeys.LT, value);
    }

    public HybridDslNode must(HybridDslExpr... exprs) {
        return clause(HybridDslKeys.MUST, exprs);
    }

    public HybridDslNode should(HybridDslExpr... exprs) {
        return clause(HybridDslKeys.SHOULD, exprs);
    }

    public HybridDslNode mustNot(HybridDslExpr... exprs) {
        return clause(HybridDslKeys.MUST_NOT, exprs);
    }

    public HybridDslNode filter(HybridDslExpr... exprs) {
        return clause(HybridDslKeys.FILTER, exprs);
    }

    public HybridDslNode filter(Filter filterObj) {
        JSONArray conditions = FilterMapper.toFilterConditionList(filterObj);
        if (conditions != null && !conditions.isEmpty()) {
            appendClause(HybridDslKeys.FILTER, conditions);
        }
        return this;
    }

    /**
     * Sets {@code minimum_should_match} on a {@code bool} query.
     * When {@code should} coexists with {@code must} or {@code filter}, the server default is {@code 0}
     * (should clauses are optional); otherwise the default is {@code 1}.
     */
    public HybridDslNode minimumShouldMatch(int n) {
        return param(HybridDslKeys.MINIMUM_SHOULD_MATCH, n);
    }

    public HybridDslNode boost(double boost) {
        return param(HybridDslKeys.BOOST, boost);
    }

    @Override
    public JSONObject toJson() {
        if (HybridDslKeys.RANGE.equals(keyword) && fieldParams != null && fieldParams.isEmpty()) {
            throw new IllegalStateException("range requires at least one bound");
        }
        if (HybridDslKeys.BOOL.equals(keyword) && body.isEmpty()) {
            throw new IllegalStateException("bool query must have at least one clause");
        }
        JSONObject root = new JSONObject();
        root.put(keyword, body);
        return root;
    }

    JSONObject innerBody() {
        return body;
    }

    String keyword() {
        return keyword;
    }

    // ── internal ────────────────────────────────────────────────────

    private HybridDslNode bound(String key, Number value) {
        if (fieldParams == null) {
            throw new IllegalStateException("range bounds require range() builder");
        }
        fieldParams.put(key, value);
        return this;
    }

    private HybridDslNode clause(String clauseKey, HybridDslExpr[] exprs) {
        if (exprs == null || exprs.length == 0) {
            return this;
        }
        JSONArray arr = new JSONArray();
        for (HybridDslExpr expr : exprs) {
            if (expr != null) {
                arr.add(expr.toJson());
            }
        }
        if (!arr.isEmpty()) {
            appendClause(clauseKey, arr);
        }
        return this;
    }

    private void appendClause(String clauseKey, JSONArray items) {
        Object existing = body.get(clauseKey);
        if (existing instanceof JSONArray) {
            ((JSONArray) existing).addAll(items);
        } else {
            body.put(clauseKey, items);
        }
    }

    static void addFields(JSONArray target, String[] fields) {
        for (String field : fields) {
            target.add(requireField(field));
        }
    }

    static JSONArray toArray(Object... values) {
        JSONArray arr = new JSONArray();
        if (values != null) {
            for (Object value : values) {
                arr.add(value);
            }
        }
        return arr;
    }

    private static void requireKeyword(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            throw new IllegalArgumentException("keyword cannot be empty");
        }
    }

    static String requireField(String field) {
        if (field == null || field.trim().isEmpty()) {
            throw new IllegalArgumentException("field cannot be empty");
        }
        return field.trim();
    }

    static void requireNonEmpty(Object[] values, String name) {
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException(name + " cannot be empty");
        }
    }

    static void requireText(String text, String name) {
        if (text == null || text.isEmpty()) {
            throw new IllegalArgumentException(name + " cannot be empty");
        }
    }
}
