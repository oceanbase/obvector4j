package com.oceanbase.obvector4j.hybrid;

import com.oceanbase.obvector4j.schema.DataType;
import com.oceanbase.obvector4j.model.SqlDouble;
import com.oceanbase.obvector4j.model.Sqlizable;
import com.oceanbase.obvector4j.model.SqlizableFactory;
import com.oceanbase.obvector4j.filter.Filter;
import com.oceanbase.obvector4j.filter.FilterSqlConverter;
import com.oceanbase.obvector4j.hybrid.core.HybridSearchSupport;
import com.oceanbase.obvector4j.hybrid.core.HybridSearchDslBuilder;
import com.oceanbase.obvector4j.util.VectorMetric;
import com.oceanbase.obvector4j.version.OceanBaseVersion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Executes hybrid search via HYBRID_SEARCH SQL (4.6.0+) or legacy SQL paths.
 */
public final class HybridSearchEngine {

    public interface VersionSupport {
        boolean supportsHybridSearchSql() throws Throwable;

        OceanBaseVersion getOceanBaseVersion() throws Throwable;
    }

    private final Connection connection;
    private final VersionSupport versionSupport;

    public HybridSearchEngine(Connection connection, VersionSupport versionSupport) {
        this.connection = connection;
        this.versionSupport = versionSupport;
    }

    public ArrayList<HashMap<String, Sqlizable>> textVectorSearch(
            String tableName,
            String vecColumnName,
            String metricType,
            float[] queryVector,
            String[] textFields,
            String textQuery,
            Object filterExpr,
            int topk,
            String[] outputFields,
            DataType[] outputDataTypes,
            Integer rankWindowSize) throws Throwable {

        textFields = OutputFieldValidator.sanitizeStringFields(textFields, "Full-text search fields");
        OutputFieldValidator.ValidatedFields validated =
                OutputFieldValidator.validateOutputFields(outputFields, outputDataTypes);
        outputFields = validated.getFields();
        outputDataTypes = validated.getDataTypes();

        if (versionSupport.supportsHybridSearchSql() && !(filterExpr instanceof String)) {
            Filter filter = filterExpr instanceof Filter ? (Filter) filterExpr : null;
            String dsl = HybridSearchDslBuilder.buildTextVectorDsl(
                    vecColumnName, queryVector, textFields, textQuery, filter, topk, rankWindowSize);
            return executeHybridSearchSql(tableName, dsl, outputFields, outputDataTypes);
        }

        String whereExpr = FilterSqlConverter.toWhereClause(filterExpr);
        int window = (rankWindowSize != null && rankWindowSize > 0) ? rankWindowSize : topk * 2;
        ArrayList<HashMap<String, Sqlizable>> vectorResults = vectorSearch(
                tableName, vecColumnName, metricType, queryVector, whereExpr, window, outputFields, outputDataTypes);
        ArrayList<HashMap<String, Sqlizable>> fulltextResults = fulltextSearch(
                tableName, textFields, textQuery, whereExpr, window, outputFields, outputDataTypes);
        return HybridResultMerger.merge(vectorResults, fulltextResults, outputFields, topk, rankWindowSize);
    }

    public ArrayList<HashMap<String, Sqlizable>> scalarVectorSearch(
            String tableName,
            String vecColumnName,
            String metricType,
            float[] queryVector,
            Object filterExpr,
            int topk,
            String[] outputFields,
            DataType[] outputDataTypes) throws Throwable {

        OutputFieldValidator.ValidatedFields validated =
                OutputFieldValidator.validateOutputFields(outputFields, outputDataTypes);
        outputFields = validated.getFields();
        outputDataTypes = validated.getDataTypes();

        if (versionSupport.supportsHybridSearchSql() && !(filterExpr instanceof String)) {
            Filter filter = filterExpr instanceof Filter ? (Filter) filterExpr : null;
            String dsl = HybridSearchDslBuilder.buildScalarVectorDsl(vecColumnName, queryVector, topk, filter);
            return executeHybridSearchSql(tableName, dsl, outputFields, outputDataTypes);
        }

        String whereExpr = FilterSqlConverter.toWhereClause(filterExpr);
        return vectorSearch(tableName, vecColumnName, metricType, queryVector, whereExpr, topk, outputFields, outputDataTypes);
    }

    /**
     * Execute HYBRID_SEARCH with a caller-supplied DSL JSON (4.6.0+).
     */
    public ArrayList<HashMap<String, Sqlizable>> searchWithDsl(
            String tableName,
            String dslJson,
            String[] outputFields,
            DataType[] outputDataTypes) throws Throwable {
        HybridSearchSupport.require(versionSupport.getOceanBaseVersion());
        OutputFieldValidator.ValidatedFields validated =
                OutputFieldValidator.validateOutputFields(outputFields, outputDataTypes);
        return executeHybridSearchSql(
                tableName, dslJson, validated.getFields(), validated.getDataTypes());
    }

    private ArrayList<HashMap<String, Sqlizable>> executeHybridSearchSql(
            String tableName,
            String dslJson,
            String[] outputFields,
            DataType[] outputDataTypes) throws Throwable {
        StringBuilder sql = new StringBuilder("SELECT ");
        for (int i = 0; i < outputFields.length; i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append("`").append(outputFields[i]).append("`");
        }
        sql.append(" FROM HYBRID_SEARCH(TABLE `").append(tableName).append("`, ?)");

        ArrayList<HashMap<String, Sqlizable>> results = new ArrayList<>();
        try (PreparedStatement pstmt = connection.prepareStatement(sql.toString())) {
            pstmt.setString(1, dslJson);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    HashMap<String, Sqlizable> row = new HashMap<>();
                    for (int i = 0; i < outputFields.length; i++) {
                        String columnName = outputFields[i];
                        row.put(columnName, SqlizableFactory.build(outputDataTypes[i], rs, columnName));
                    }
                    results.add(row);
                }
            }
        }
        return results;
    }

    private ArrayList<HashMap<String, Sqlizable>> vectorSearch(
            String tableName,
            String vecColumnName,
            String metricType,
            float[] queryVector,
            String whereExpr,
            int topk,
            String[] outputFields,
            DataType[] outputDataTypes) throws Throwable {
        VectorMetric.validateMetricType(metricType);
        String distFunc = VectorMetric.resolveDistanceFunction(metricType);
        String vectorStr = VectorMetric.formatVectorLiteral(queryVector);

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        for (int i = 0; i < outputFields.length; i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append("`").append(outputFields[i]).append("`");
        }
        sql.append(", ").append(distFunc).append("(`").append(vecColumnName).append("`, ?) as distance");
        appendScoreExpression(sql, metricType, distFunc, vecColumnName);
        sql.append(" FROM `").append(tableName).append("`");
        if (whereExpr != null && !whereExpr.trim().isEmpty()) {
            sql.append(" WHERE ").append(whereExpr);
        }
        sql.append(" ORDER BY ").append(distFunc).append("(`").append(vecColumnName).append("`, ?) ASC");
        sql.append(" APPROXIMATE LIMIT ?");

        ArrayList<HashMap<String, Sqlizable>> results = new ArrayList<>();
        try (PreparedStatement pstmt = connection.prepareStatement(sql.toString())) {
            pstmt.setString(1, vectorStr);
            pstmt.setString(2, vectorStr);
            pstmt.setString(3, vectorStr);
            pstmt.setInt(4, topk);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    HashMap<String, Sqlizable> row = new HashMap<>();
                    for (int i = 0; i < outputFields.length; i++) {
                        String columnName = outputFields[i];
                        row.put(columnName, SqlizableFactory.build(outputDataTypes[i], rs, columnName));
                    }
                    try {
                        row.put("distance", new SqlDouble(rs.getDouble("distance")));
                    } catch (SQLException ignored) {
                        // distance column optional
                    }
                    results.add(row);
                }
            }
        }
        return results;
    }

    private static void appendScoreExpression(
            StringBuilder sql, String metricType, String distFunc, String vecColumnName) {
        if ("cosine".equalsIgnoreCase(metricType)) {
            sql.append(", (2 - ").append(distFunc).append("(`").append(vecColumnName).append("`, ?)) / 2 as score");
        } else if ("l2".equalsIgnoreCase(metricType)) {
            sql.append(", 1 / (1 + ").append(distFunc).append("(`").append(vecColumnName).append("`, ?)) as score");
        } else if ("ip".equalsIgnoreCase(metricType) || "inner_product".equalsIgnoreCase(metricType)) {
            sql.append(", (").append(distFunc).append("(`").append(vecColumnName).append("`, ?) + 1) / 2 as score");
        } else {
            sql.append(", (2 - ").append(distFunc).append("(`").append(vecColumnName).append("`, ?)) / 2 as score");
        }
    }

    private ArrayList<HashMap<String, Sqlizable>> fulltextSearch(
            String tableName,
            String[] textFields,
            String textQuery,
            String whereExpr,
            int topk,
            String[] outputFields,
            DataType[] outputDataTypes) throws Throwable {
        ArrayList<HashMap<String, Sqlizable>> results = new ArrayList<>();
        if (textFields == null || textFields.length == 0) {
            return results;
        }

        StringBuilder matchClause = new StringBuilder();
        if (textFields.length == 1) {
            matchClause.append("MATCH(`").append(textFields[0]).append("`) AGAINST(? IN NATURAL LANGUAGE MODE)");
        } else {
            matchClause.append("(");
            for (int i = 0; i < textFields.length; i++) {
                if (i > 0) {
                    matchClause.append(" OR ");
                }
                matchClause.append("MATCH(`").append(textFields[i]).append("`) AGAINST(? IN NATURAL LANGUAGE MODE)");
            }
            matchClause.append(")");
        }

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        for (int i = 0; i < outputFields.length; i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append("`").append(outputFields[i]).append("`");
        }
        sql.append(", ").append(matchClause).append(" as score");
        sql.append(" FROM `").append(tableName).append("`");
        sql.append(" WHERE ").append(matchClause);
        if (whereExpr != null && !whereExpr.trim().isEmpty()) {
            sql.append(" AND ").append(whereExpr);
        }
        sql.append(" ORDER BY score DESC LIMIT ?");

        try (PreparedStatement pstmt = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < textFields.length; i++) {
                pstmt.setString(i + 1, textQuery);
            }
            for (int i = 0; i < textFields.length; i++) {
                pstmt.setString(textFields.length + i + 1, textQuery);
            }
            pstmt.setInt(textFields.length * 2 + 1, topk);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    HashMap<String, Sqlizable> row = new HashMap<>();
                    for (int i = 0; i < outputFields.length; i++) {
                        String columnName = outputFields[i];
                        row.put(columnName, SqlizableFactory.build(outputDataTypes[i], rs, columnName));
                    }
                    results.add(row);
                }
            }
        }
        return results;
    }
}
