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

import com.oceanbase.obvector4j.filter.Filter;
import com.oceanbase.obvector4j.hybrid.core.dsl.HybridDsl;
import com.oceanbase.obvector4j.hybrid.core.dsl.HybridDslExpr;

/**
 * Builds JSON DSL strings for OceanBase HYBRID_SEARCH SQL interface (4.6.0+).
 * Prefer {@link HybridDsl} for typed expression construction.
 */
public final class HybridSearchDslBuilder {

    private static final int DEFAULT_RRF_RANK_CONSTANT = 60;

    private HybridSearchDslBuilder() {
    }

    public static String buildScalarVectorDsl(
            String vecColumnName,
            float[] queryVector,
            int topk,
            Filter filter) {
        return HybridSearchDsl.create()
                .knn(HybridDsl.knn(vecColumnName, queryVector, topk).filter(filter))
                .size(topk)
                .toJsonString();
    }

    public static String buildTextVectorDsl(
            String vecColumnName,
            float[] queryVector,
            String[] textFields,
            String textQuery,
            Filter filter,
            int topk,
            Integer rankWindowSize) {
        int knnK = (rankWindowSize != null && rankWindowSize > 0) ? rankWindowSize : topk * 2;
        int windowSize = (rankWindowSize != null && rankWindowSize > 0) ? rankWindowSize : topk;

        HybridDslExpr textQueryExpr = buildTextQueryExpr(textFields, textQuery, filter);

        return HybridSearchDsl.create()
                .query(textQueryExpr)
                .knn(HybridDsl.knn(vecColumnName, queryVector, knnK).filter(filter))
                .rank(HybridDsl.rrf(windowSize, DEFAULT_RRF_RANK_CONSTANT))
                .size(topk)
                .toJsonString();
    }

    static String formatVector(float[] vector) {
        return HybridDsl.formatVector(vector);
    }

    private static HybridDslExpr buildTextQueryExpr(String[] textFields, String textQuery, Filter filter) {
        HybridDslExpr textMatch;
        if (textFields.length == 1) {
            textMatch = HybridDsl.match(textFields[0], textQuery);
        } else {
            textMatch = HybridDsl.multiMatch(textFields, textQuery);
        }

        if (filter == null) {
            return textMatch;
        }

        return HybridDsl.bool()
                .must(textMatch)
                .filter(filter);
    }
}
