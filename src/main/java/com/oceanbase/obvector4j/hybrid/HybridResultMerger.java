package com.oceanbase.obvector4j.hybrid;

import com.oceanbase.obvector4j.model.Sqlizable;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Reciprocal Rank Fusion (RRF) for legacy hybrid search result merging.
 */
final class HybridResultMerger {

    private HybridResultMerger() {
    }

    static ArrayList<HashMap<String, Sqlizable>> merge(
            ArrayList<HashMap<String, Sqlizable>> vectorResults,
            ArrayList<HashMap<String, Sqlizable>> fulltextResults,
            String[] outputFields,
            int topk,
            Integer rankWindowSize) {
        if (vectorResults == null) {
            vectorResults = new ArrayList<>();
        }
        if (fulltextResults == null) {
            fulltextResults = new ArrayList<>();
        }

        final int k = (rankWindowSize != null && rankWindowSize > 0) ? rankWindowSize : topk;
        HashMap<String, HashMap<String, Sqlizable>> resultMap = new HashMap<>();
        HashMap<String, Double> rrfScores = new HashMap<>();

        accumulateRrf(vectorResults, outputFields, k, resultMap, rrfScores);
        accumulateRrf(fulltextResults, outputFields, k, resultMap, rrfScores);

        ArrayList<HashMap<String, Sqlizable>> combinedResults = new ArrayList<>(resultMap.values());
        combinedResults.sort((a, b) -> {
            String keyA = rowKey(a, outputFields);
            String keyB = rowKey(b, outputFields);
            double scoreA = rrfScores.getOrDefault(keyA, 0.0);
            double scoreB = rrfScores.getOrDefault(keyB, 0.0);
            return Double.compare(scoreB, scoreA);
        });

        ArrayList<HashMap<String, Sqlizable>> finalResults = new ArrayList<>();
        for (int i = 0; i < Math.min(topk, combinedResults.size()); i++) {
            finalResults.add(combinedResults.get(i));
        }
        return finalResults;
    }

    private static void accumulateRrf(
            ArrayList<HashMap<String, Sqlizable>> rows,
            String[] outputFields,
            int k,
            HashMap<String, HashMap<String, Sqlizable>> resultMap,
            HashMap<String, Double> rrfScores) {
        for (int i = 0; i < rows.size(); i++) {
            HashMap<String, Sqlizable> row = rows.get(i);
            String key = rowKey(row, outputFields);
            if (key != null) {
                resultMap.putIfAbsent(key, row);
                int rank = i + 1;
                rrfScores.put(key, rrfScores.getOrDefault(key, 0.0) + 1.0 / (k + rank));
            }
        }
    }

    private static String rowKey(HashMap<String, Sqlizable> row, String[] outputFields) {
        if (outputFields == null || outputFields.length == 0 || row == null) {
            return null;
        }
        Sqlizable value = row.get(outputFields[0]);
        return value != null ? value.toString() : null;
    }
}
