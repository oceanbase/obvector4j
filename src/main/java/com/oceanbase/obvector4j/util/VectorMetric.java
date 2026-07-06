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
