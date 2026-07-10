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

/**
 * Fixed JSON keywords for OceanBase 4.6.0+ {@code HYBRID_SEARCH} DSL.
 * Extracted from the official hybrid-search SQL interface specification.
 */
public final class HybridDslKeys {

    private HybridDslKeys() {
    }

    // ── top-level ─────────────────────────────────────────────────

    public static final String QUERY = "query";
    public static final String KNN = "knn";
    public static final String RANK = "rank";
    public static final String FROM = "from";
    public static final String SIZE = "size";
    public static final String MIN_SCORE = "min_score";

    // ── full-text query types ─────────────────────────────────────

    public static final String MATCH = "match";
    public static final String MULTI_MATCH = "multi_match";
    public static final String MATCH_PHRASE = "match_phrase";
    public static final String QUERY_STRING = "query_string";

    // ── scalar query types ────────────────────────────────────────

    public static final String TERM = "term";
    public static final String RANGE = "range";
    public static final String TERMS = "terms";

    // ── json query types ──────────────────────────────────────────

    public static final String JSON_CONTAINS = "json_contains";
    public static final String JSON_OVERLAPS = "json_overlaps";
    public static final String JSON_MEMBER_OF = "json_member_of";

    // ── array query types ─────────────────────────────────────────

    public static final String ARRAY_CONTAINS = "array_contains";
    public static final String ARRAY_CONTAINS_ALL = "array_contains_all";
    public static final String ARRAY_OVERLAPS = "array_overlaps";

    // ── bool ──────────────────────────────────────────────────────

    public static final String BOOL = "bool";
    public static final String MUST = "must";
    public static final String SHOULD = "should";
    public static final String FILTER = "filter";
    public static final String MUST_NOT = "must_not";

    // ── common params ─────────────────────────────────────────────

    public static final String QUERY_PARAM = "query";
    public static final String FIELDS = "fields";
    public static final String OPERATOR = "operator";
    public static final String DEFAULT_OPERATOR = "default_operator";
    public static final String MINIMUM_SHOULD_MATCH = "minimum_should_match";
    public static final String BOOST = "boost";
    public static final String TYPE = "type";
    public static final String SLOP = "slop";
    public static final String VALUE = "value";

    // ── range bounds ────────────────────────────────────────────────

    public static final String GTE = "gte";
    public static final String GT = "gt";
    public static final String LTE = "lte";
    public static final String LT = "lt";

    // ── json params ─────────────────────────────────────────────────

    public static final String CANDIDATE = "candidate";
    public static final String PATH = "path";

    // ── knn ─────────────────────────────────────────────────────────

    public static final String FIELD = "field";
    public static final String K = "k";
    public static final String QUERY_VECTOR = "query_vector";
    public static final String SIMILARITY = "similarity";
    public static final String SEARCH_OPTIONS = "search_options";
    public static final String EF_SEARCH = "ef_search";
    public static final String REFINE_K = "refine_k";
    public static final String FILTER_MODE = "filter_mode";

    // ── rank ────────────────────────────────────────────────────────

    public static final String RRF = "rrf";
    public static final String WEIGHTED_SUM = "weighted_sum";
    public static final String RANK_CONSTANT = "rank_constant";
    public static final String RANK_WINDOW_SIZE = "rank_window_size";
    public static final String NORMALIZER = "normalizer";

    // ── enum-like values ────────────────────────────────────────────

    public static final class Operator {
        public static final String OR = "OR";
        public static final String AND = "AND";

        private Operator() {
        }
    }

    public static final class MultiMatchType {
        public static final String BEST_FIELDS = "best_fields";
        public static final String MOST_FIELDS = "most_fields";

        private MultiMatchType() {
        }
    }

    public static final class FilterMode {
        public static final String PRE = "pre";
        public static final String PRE_KNN = "pre-knn";
        public static final String PRE_BRUTE = "pre-brute";
        public static final String POST = "post";
        public static final String POST_INDEX_MERGE = "post-index-merge";

        private FilterMode() {
        }
    }

    public static final class Normalizer {
        public static final String NONE = "none";
        public static final String MINMAX = "minmax";

        private Normalizer() {
        }
    }
}
