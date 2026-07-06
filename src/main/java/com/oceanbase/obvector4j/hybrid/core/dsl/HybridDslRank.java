package com.oceanbase.obvector4j.hybrid.core.dsl;

import org.json.simple.JSONObject;

/**
 * {@code rank} section for HYBRID_SEARCH DSL.
 */
public final class HybridDslRank {

    private final JSONObject rank = new JSONObject();

    private HybridDslRank() {
    }

    public static HybridDslRank rrf(int rankWindowSize, int rankConstant) {
        if (rankWindowSize <= 0) {
            throw new IllegalArgumentException("rank_window_size must be > 0");
        }
        HybridDslRank result = new HybridDslRank();
        JSONObject rrf = new JSONObject();
        rrf.put(HybridDslKeys.RANK_WINDOW_SIZE, rankWindowSize);
        rrf.put(HybridDslKeys.RANK_CONSTANT, rankConstant);
        result.rank.put(HybridDslKeys.RRF, rrf);
        return result;
    }

    /**
     * @param normalizer {@link HybridDslKeys.Normalizer#NONE} or {@link HybridDslKeys.Normalizer#MINMAX}
     */
    public static HybridDslRank weightedSum(String normalizer, int rankWindowSize) {
        if (normalizer == null || normalizer.trim().isEmpty()) {
            throw new IllegalArgumentException("normalizer cannot be empty");
        }
        HybridDslRank result = new HybridDslRank();
        JSONObject weightedSum = new JSONObject();
        weightedSum.put(HybridDslKeys.NORMALIZER, normalizer.trim());
        if (rankWindowSize > 0) {
            weightedSum.put(HybridDslKeys.RANK_WINDOW_SIZE, rankWindowSize);
        }
        result.rank.put(HybridDslKeys.WEIGHTED_SUM, weightedSum);
        return result;
    }

    public JSONObject toJson() {
        return rank;
    }
}
