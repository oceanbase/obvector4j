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

package com.oceanbase.obvector4j.util;

/**
 * Vector distance metric helpers for SQL generation.
 */
public final class VectorMetric {

    private VectorMetric() {
    }

    public static String resolveDistanceFunction(String metricType) {
        String metric = metricType.toLowerCase();
        if ("ip".equals(metric)) {
            return "negative_inner_product";
        }
        if ("cosine".equals(metric)) {
            return "cosine_distance";
        }
        if ("l2".equals(metric)) {
            return "l2_distance";
        }
        throw new UnsupportedOperationException("Metric Type is not supported: " + metricType);
    }

    public static void validateMetricType(String metricType) {
        resolveDistanceFunction(metricType);
    }

    public static String formatVectorLiteral(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(vector[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}
