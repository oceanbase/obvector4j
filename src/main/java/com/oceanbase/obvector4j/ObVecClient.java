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

package com.oceanbase.obvector4j;

import com.oceanbase.obvector4j.hybrid.HybridSearchEngine;
import com.oceanbase.obvector4j.hybrid.core.HybridSearch;
import com.oceanbase.obvector4j.hybrid.core.HybridSearchSupport;
import com.oceanbase.obvector4j.hybrid.core.HybridSearchCustomBuilder;
import com.oceanbase.obvector4j.hybrid.HybridScalarVectorSearchBuilder;
import com.oceanbase.obvector4j.hybrid.HybridTextVectorSearchBuilder;
import com.oceanbase.obvector4j.util.JdbcTypeMapper;
import com.oceanbase.obvector4j.model.Sqlizable;
import com.oceanbase.obvector4j.model.SqlizableFactory;
import com.oceanbase.obvector4j.schema.DataType;
import com.oceanbase.obvector4j.schema.IndexParam;
import com.oceanbase.obvector4j.schema.ObCollectionSchema;
import com.oceanbase.obvector4j.util.VectorMetric;
import com.oceanbase.obvector4j.version.OceanBaseVersion;
import java.rmi.UnexpectedException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class ObVecClient implements AutoCloseable {
    protected Connection conn = null;
    private OceanBaseVersion cachedVersion = null;
    private HybridSearchEngine hybridSearchEngine = null;

    public ObVecClient(String uri, String user, String password) throws Exception
    {
        try {
            conn = DriverManager.getConnection(uri, user, password);
        } catch (SQLException e) {
            throw e;
        }
    }

    /**
     * Close the underlying JDBC connection.
     */
    @Override
    public void close() throws SQLException {
        if (conn != null && !conn.isClosed()) {
            conn.close();
        }
    }

    private HybridSearchEngine hybridSearchEngine() {
        if (hybridSearchEngine == null) {
            hybridSearchEngine = new HybridSearchEngine(conn, new HybridSearchEngine.VersionSupport() {
                @Override
                public boolean supportsHybridSearchSql() throws Exception {
                    return ObVecClient.this.supportsHybridSearchSql();
                }

                @Override
                public OceanBaseVersion getOceanBaseVersion() throws Exception {
                    return ObVecClient.this.getOceanBaseVersion();
                }
            });
        }
        return hybridSearchEngine;
    }

    public void setHNSWEfSearch(int val) throws Exception {
        try (Statement statement = conn.createStatement()) {
            String sql = String.format("SET @@ob_hnsw_ef_search = %d", val);
            statement.execute(sql);
        } catch (SQLException e) {
            throw e;
        }
    }

    public int getHNSWEfSearch() throws Exception {
        ArrayList<Integer> res = new ArrayList<>();
        try (Statement statement = conn.createStatement()) {
            String sql = String.format("show variables like 'ob_hnsw_ef_search'");
            try (ResultSet resultSet = statement.executeQuery(sql)) {
                while (resultSet.next()) {
                    res.add(resultSet.getInt("Value"));
                }
            }
            if (res.size() != 1) {
                throw new UnexpectedException("ob_hnsw_ef_search is a single variable");
            }
        } catch (SQLException e) {
            throw e;
        }
        return res.get(0);
    }

    public void dropCollection(String table_name) throws Exception
    {
        try (Statement statement = conn.createStatement()) {
            String sql = String.format("DROP TABLE IF EXISTS %s", table_name);
            statement.execute(sql);
        } catch (SQLException e) {
            throw e;
        }
    }

    public boolean hasCollection(String table_name) throws Exception
    {
        try {
            DatabaseMetaData metaData = conn.getMetaData();
            try (ResultSet rs = metaData.getTables(null, null, table_name, null)) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw e;
        }
    }

    public void createCollection(String table_name, ObCollectionSchema collection) throws Exception
    {
        try (Statement statement = conn.createStatement()) {
            String sql = String.format("CREATE TABLE %s (%s)%s",
                    table_name, collection.visit(), collection.visitTableOptions());
            statement.execute(sql);
        } catch (SQLException e) {
            throw e;
        }
    }

    public void createIndex(String table_name, IndexParam index_param) throws Exception
    {
        try (Statement statement = conn.createStatement()) {
            String sql = String.format("CREATE VECTOR INDEX %s on %s(%s) %s",
                            index_param.getVidxName(),
                            table_name,
                            index_param.getFieldName(),
                            index_param.visit());
            statement.execute(sql);
        } catch (SQLException e) {
            throw e;
        }
    }

    /**
     * Create full-text index
     * @param table_name Table name
     * @param index_name Index name
     * @param column_name Column name
     * @throws Exception
     */
    public void createFulltextIndex(String table_name, String index_name, String column_name) throws Exception
    {
        try (Statement statement = conn.createStatement()) {
            String sql = String.format("ALTER TABLE %s ADD FULLTEXT INDEX %s(%s)",
                            table_name, index_name, column_name);
            statement.execute(sql);
        } catch (SQLException e) {
            throw e;
        }
    }

    public void insert(String table_name, String[] column_names, ArrayList<Sqlizable[]> rows) throws Exception
    {
        if (rows.isEmpty()) {
            return;
        }

        conn.setAutoCommit(false);
        ArrayList<String> param_str_list = new ArrayList<String>(Collections.nCopies(column_names.length, "?"));
        String sql = String.format("INSERT INTO %s (%s) VALUES (%s)",
                        table_name,
                        String.join(", ", column_names),
                        String.join(", ", param_str_list));
        try (PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
            for (int i = 0; i < rows.size(); i++) {
                Sqlizable[] cols = rows.get(i);
                if (cols.length != column_names.length) {
                    throw new UnsupportedOperationException("column size mismatch");
                }
                for (int col_id = 0; col_id < cols.length; col_id++) {
                    cols[col_id].toDB(col_id + 1, preparedStatement);
                }
                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();
            conn.commit();
        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    throw rollbackEx;
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
            }
        }
    }

    public void delete(String table_name, String primary_key_name, ArrayList<Sqlizable> primary_keys) throws Exception
    {
        ArrayList<String> param_str_list = new ArrayList<String>(Collections.nCopies(primary_keys.size(), "?"));
        String sql = String.format("DELETE FROM %s WHERE %s in (%s)",
                        table_name,
                        primary_key_name,
                        String.join(", ", param_str_list));
        try (PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
            for (int i = 0; i < primary_keys.size(); i++) {
                primary_keys.get(i).toDB(i + 1, preparedStatement);
            }
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw e;
        }
    }

    /**
     * Update rows by primary key.
     * @param table_name Table name
     * @param column_names Column names to update (excluding the primary key)
     * @param values New values for each row (must align with column_names)
     * @param primary_key_name Primary key column name
     * @param primary_keys Primary key values to identify rows (must align with values)
     * @throws Exception on error
     */
    public void update(String table_name, String[] column_names, ArrayList<Sqlizable[]> values,
                       String primary_key_name, ArrayList<Sqlizable> primary_keys) throws Exception
    {
        if (values.isEmpty()) {
            return;
        }
        if (values.size() != primary_keys.size()) {
            throw new IllegalArgumentException("values size mismatched with primary_keys size");
        }
        String[] setParams = new String[column_names.length];
        for (int i = 0; i < column_names.length; i++) {
            setParams[i] = column_names[i] + " = ?";
        }
        String sql = String.format("UPDATE %s SET %s WHERE %s = ?",
                        table_name,
                        String.join(", ", setParams),
                        primary_key_name);
        conn.setAutoCommit(false);
        try (PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
            for (int i = 0; i < values.size(); i++) {
                Sqlizable[] cols = values.get(i);
                if (cols.length != column_names.length) {
                    throw new UnsupportedOperationException("column size mismatch");
                }
                for (int col_id = 0; col_id < cols.length; col_id++) {
                    cols[col_id].toDB(col_id + 1, preparedStatement);
                }
                primary_keys.get(i).toDB(cols.length + 1, preparedStatement);
                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();
            conn.commit();
        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    throw rollbackEx;
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
            }
        }
    }

    public ArrayList<HashMap<String, Sqlizable>> query(String table_name,
                      String vec_col_name,
                      String metric_type,
                      float[] qv,
                      int topk,
                      String[] output_fields,
                      DataType[] output_datatypes,
                      String where_expr) throws Exception
    {
        ArrayList<HashMap<String, Sqlizable>> res = new ArrayList<>();

        VectorMetric.validateMetricType(metric_type);
        String dist_func = VectorMetric.resolveDistanceFunction(metric_type);
        String vectorLiteral = VectorMetric.formatVectorLiteral(qv);

        try (Statement statement = conn.createStatement()) {
            String sql = String.format("SELECT %s FROM %s WHERE %s ORDER BY %s(%s, '%s') APPROXIMATE LIMIT %d",
                            String.join(", ", output_fields),
                            table_name,
                            (where_expr == null) ? "1" : where_expr,
                            dist_func,
                            vec_col_name,
                            vectorLiteral,
                            topk);
            try (ResultSet resultSet = statement.executeQuery(sql)) {
                ResultSetMetaData metaData = resultSet.getMetaData();
                int project_count = metaData.getColumnCount();
                if (project_count != output_datatypes.length) {
                    throw new IllegalArgumentException(
                            " the length of output_datatypes: " + output_datatypes.length
                                    + " mismatched with projected column size: " + project_count);
                }

                while (resultSet.next()) {
                    HashMap<String, Sqlizable> row = new HashMap<>();

                    for (int i = 1; i <= project_count; i++) {
                        String columnName = metaData.getColumnName(i);
                        Sqlizable sqlizable = SqlizableFactory.build(output_datatypes[i - 1], resultSet, columnName);
                        row.put(columnName, sqlizable);
                    }

                    res.add(row);
                }
            }
        } catch (SQLException e) {
            throw e;
        }
        return res;
    }

    /**
     * Returns the connected OceanBase version (cached after first call).
     */
    public OceanBaseVersion getOceanBaseVersion() throws Exception {
        if (cachedVersion != null) {
            return cachedVersion;
        }
        try (Statement statement = conn.createStatement()) {
            String versionString = null;
            try (ResultSet obVer = statement.executeQuery("SELECT OB_VERSION()")) {
                if (obVer.next()) {
                    versionString = obVer.getString(1);
                }
            } catch (SQLException ignored) {
                // fall back to VERSION()
            }
            if (versionString == null || versionString.trim().isEmpty()) {
                try (ResultSet ver = statement.executeQuery("SELECT VERSION()")) {
                    if (ver.next()) {
                        versionString = ver.getString(1);
                    }
                }
            }
            if (versionString != null && !versionString.trim().isEmpty()) {
                cachedVersion = OceanBaseVersion.parse(versionString);
                return cachedVersion;
            }
        } catch (SQLException e) {
            throw e;
        }
        throw new SQLException("Unable to detect OceanBase version");
    }

    /**
     * Whether the connected OceanBase supports HYBRID_SEARCH SQL interface (4.6.0+).
     */
    public boolean supportsHybridSearchSql() throws Exception {
        return getOceanBaseVersion().isAtLeast(OceanBaseVersion.HYBRID_SEARCH_SQL_MIN);
    }

    /**
     * Full-text + vector hybrid search
     */
    public ArrayList<HashMap<String, Sqlizable>> hybridTextVectorSearch(
            String table_name,
            String vec_col_name,
            String metric_type,
            float[] qv,
            String[] text_fields,
            String text_query,
            Object filter_expr,
            int topk,
            String[] output_fields,
            DataType[] output_datatypes,
            Integer rank_window_size) throws Exception {
        return hybridSearchEngine().textVectorSearch(
                table_name, vec_col_name, metric_type, qv, text_fields, text_query,
                filter_expr, topk, output_fields, output_datatypes, rank_window_size);
    }

    /**
     * Scalar + vector hybrid search
     */
    public ArrayList<HashMap<String, Sqlizable>> hybridScalarVectorSearch(
            String table_name,
            String vec_col_name,
            String metric_type,
            float[] qv,
            Object filter_expr,
            int topk,
            String[] output_fields,
            DataType[] output_datatypes) throws Exception {
        return hybridSearchEngine().scalarVectorSearch(
                table_name, vec_col_name, metric_type, qv, filter_expr,
                topk, output_fields, output_datatypes);
    }

    /**
     * Auto-infer data type based on table name and column name
     */
    /**
     * Infers column data type from table metadata. Used by hybrid search builders.
     */
    public DataType inferColumnDataType(String tableName, String columnName) throws Exception {
        return inferDataType(tableName, columnName);
    }

    protected DataType inferDataType(String tableName, String columnName) throws Exception {
        if (columnName == null || columnName.trim().isEmpty()) {
            throw new IllegalArgumentException("Column name cannot be null or empty");
        }
        try {
            DatabaseMetaData metaData = conn.getMetaData();
            try (ResultSet rs = metaData.getColumns(null, null, tableName, columnName)) {
                if (rs.next()) {
                    String typeName = rs.getString("TYPE_NAME");
                    if (typeName == null) {
                        return DataType.STRING;
                    }
                    typeName = typeName.toUpperCase();

                    if (typeName.contains("VECTOR") || typeName.contains("FLOAT_VECTOR")) {
                        return DataType.FLOAT_VECTOR;
                    } else if (typeName.contains("TINYINT")) {
                        return DataType.BOOL;
                    } else if (typeName.contains("SMALLINT")) {
                        return DataType.INT16;
                    } else if (typeName.contains("INT") && !typeName.contains("BIGINT")) {
                        return DataType.INT32;
                    } else if (typeName.contains("BIGINT")) {
                        return DataType.INT64;
                    } else if (typeName.contains("FLOAT")) {
                        return DataType.FLOAT;
                    } else if (typeName.contains("DOUBLE") || typeName.contains("DECIMAL")) {
                        return DataType.DOUBLE;
                    } else if (typeName.contains("VARCHAR") || typeName.contains("CHAR")) {
                        return DataType.VARCHAR;
                    } else if (typeName.contains("TEXT") || typeName.contains("LONGTEXT")) {
                        return DataType.STRING;
                    } else if (typeName.contains("JSON")) {
                        return DataType.JSON;
                    } else {
                        return DataType.STRING;
                    }
                }
            }
            return DataType.STRING;
        } catch (SQLException e) {
            return DataType.STRING;
        }
    }

    /**
     * Execute arbitrary SQL (DDL/DML). Use {@link #querySql(String)} for SELECT.
     */
    public void executeSql(String sql) throws Exception {
        if (sql == null || sql.trim().isEmpty()) {
            throw new IllegalArgumentException("SQL cannot be empty");
        }
        try (Statement statement = conn.createStatement()) {
            statement.execute(sql.trim());
        } catch (SQLException e) {
            throw e;
        }
    }

    /**
     * Execute a SELECT and map rows to {@link Sqlizable} values.
     * Column types are inferred from JDBC metadata when not specified.
     */
    public ArrayList<HashMap<String, Sqlizable>> querySql(String sql) throws Exception {
        if (sql == null || sql.trim().isEmpty()) {
            throw new IllegalArgumentException("SQL cannot be empty");
        }

        ArrayList<HashMap<String, Sqlizable>> results = new ArrayList<>();
        try (Statement statement = conn.createStatement();
             ResultSet rs = statement.executeQuery(sql.trim())) {
            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();
            String[] columnNames = new String[columnCount];
            DataType[] columnTypes = new DataType[columnCount];
            for (int i = 0; i < columnCount; i++) {
                columnNames[i] = meta.getColumnLabel(i + 1);
                columnTypes[i] = JdbcTypeMapper.fromJdbc(meta.getColumnType(i + 1), meta.getColumnTypeName(i + 1));
            }

            while (rs.next()) {
                HashMap<String, Sqlizable> row = new HashMap<>();
                for (int i = 0; i < columnCount; i++) {
                    row.put(columnNames[i], SqlizableFactory.build(columnTypes[i], rs, columnNames[i]));
                }
                results.add(row);
            }
        } catch (SQLException e) {
            throw e;
        }
        return results;
    }

    /**
     * Execute HYBRID_SEARCH with custom DSL JSON (OceanBase 4.6.0+).
     * Prefer {@link #customHybridSearch()} for fluent DSL construction.
     */
    public ArrayList<HashMap<String, Sqlizable>> hybridSearchWithDsl(
            String tableName,
            String dslJson,
            String[] outputFields,
            DataType[] outputDataTypes) throws Exception {
        HybridSearchSupport.require(getOceanBaseVersion());
        if (outputFields == null || outputFields.length == 0) {
            throw new IllegalArgumentException("Output fields must be set");
        }
        if (outputDataTypes == null) {
            outputDataTypes = new DataType[outputFields.length];
            for (int i = 0; i < outputFields.length; i++) {
                outputDataTypes[i] = inferColumnDataType(tableName, outputFields[i]);
            }
        }
        return hybridSearchEngine().searchWithDsl(tableName, dslJson, outputFields, outputDataTypes);
    }

    public HybridTextVectorSearchBuilder textVectorSearch() {
        return new HybridTextVectorSearchBuilder(this);
    }

    public HybridScalarVectorSearchBuilder scalarVectorSearch() {
        return new HybridScalarVectorSearchBuilder(this);
    }

    /**
     * OceanBase 4.6.0+ {@code HYBRID_SEARCH} DSL entry point.
     *
     * @throws UnsupportedOperationException if the connected cluster is below 4.6.0
     */
    public HybridSearch hybridSearch() throws Exception {
        HybridSearchSupport.require(getOceanBaseVersion());
        return new HybridSearch(this);
    }

    public HybridSearchCustomBuilder customHybridSearch() throws Exception {
        return hybridSearch().customSearch();
    }
}
