package com.oceanbase.obvec_jdbc;

import com.oceanbase.obvec_jdbc.filter.Filter;
import com.oceanbase.obvec_jdbc.filter.FilterMapper;
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
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;

public class ObVecClient {
    protected Connection conn = null;

    public ObVecClient(String uri, String user, String password) throws Throwable
    {
        try {
            conn = DriverManager.getConnection(uri, user, password);
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
    }

    public void setHNSWEfSearch(int val) throws Throwable {
        Statement statement = null;

        try {
            statement = conn.createStatement();
            String sql = String.format("SET @@ob_hnsw_ef_search = %d", val);
            statement.executeQuery(sql);
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        } finally {
            try {
                if (statement != null) statement.close();
            } catch (SQLException e) {
                e.printStackTrace();
                throw e;
            }
        }
    }

    public int getHNSWEfSearch() throws Throwable {
        Statement statement = null;
        ResultSet resultSet = null;
        ArrayList<Integer> res = new ArrayList<>();

        try {
            statement = conn.createStatement();
            String sql = String.format("show variables like 'ob_hnsw_ef_search'");
            resultSet = statement.executeQuery(sql);
            
            while (resultSet.next()) {
                res.add(resultSet.getInt("Value"));
            }

            if (res.size() != 1) {
                throw new UnexpectedException("ob_hnsw_ef_search is a single variable");
            }
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        } finally {
            try {
                if (resultSet != null) resultSet.close();
                if (statement != null) statement.close();
            } catch (SQLException e) {
                e.printStackTrace();
                throw e;
            }
        }
        return res.get(0);
    }

    public void dropCollection(String table_name) throws Throwable
    {
        Statement statement = null;

        try {
            statement = conn.createStatement();
            String sql = String.format("DROP TABLE IF EXISTS %s", table_name);
            statement.executeQuery(sql);
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        } finally {
            try {
                if (statement != null) statement.close();
            } catch (SQLException e) {
                e.printStackTrace();
                throw e;
            }
        }
    }

    public boolean hasCollection(String table_name) throws Throwable
    {
        boolean exists = false;
        try {
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet rs = metaData.getTables(null, null, table_name, null);
            if (rs.next()) {
                exists = true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }

        return exists;
    }

    public void createCollection(String table_name, ObCollectionSchema collection) throws Throwable
    {
        Statement statement = null;

        try {
            statement = conn.createStatement();
            String sql = String.format("CREATE TABLE %s (%s)", table_name, collection.visit());
            statement.executeQuery(sql);
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        } finally {
            try {
                if (statement != null) statement.close();
            } catch (SQLException e) {
                e.printStackTrace();
                throw e;
            }
        }
    }

    public void createIndex(String table_name, IndexParam index_param) throws Throwable
    {
        Statement statement = null;

        try {
            statement = conn.createStatement();
            String sql = String.format("CREATE VECTOR INDEX %s on %s(%s) %s", 
                            index_param.getVidxName(),
                            table_name,
                            index_param.getFieldName(),
                            index_param.visit());
            statement.executeQuery(sql);
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        } finally {
            try {
                if (statement != null) statement.close();
            } catch (SQLException e) {
                e.printStackTrace();
                throw e;
            }
        }
    }

    /**
     * Create full-text index
     * @param table_name Table name
     * @param index_name Index name
     * @param column_name Column name
     * @throws Throwable
     */
    public void createFulltextIndex(String table_name, String index_name, String column_name) throws Throwable
    {
        Statement statement = null;

        try {
            statement = conn.createStatement();
            String sql = String.format("ALTER TABLE %s ADD FULLTEXT INDEX %s(%s)", 
                            table_name, index_name, column_name);
            statement.executeUpdate(sql);
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        } finally {
            try {
                if (statement != null) statement.close();
            } catch (SQLException e) {
                e.printStackTrace();
                throw e;
            }
        }
    }

    public void insert(String table_name, String[] column_names, ArrayList<Sqlizable[]> rows) throws Throwable
    {
        if (rows.isEmpty()) {
            return;
        }

        try {
            conn.setAutoCommit(false);
            // set prepared statement
            ArrayList<String> param_str_list = new ArrayList<String>(Collections.nCopies(column_names.length, "?"));
            String sql = String.format("INSERT INTO %s (%s) VALUES (%s)",
                            table_name,
                            String.join(", ", column_names),
                            String.join(", ", param_str_list));
            PreparedStatement preparedStatement = conn.prepareStatement(sql);
            // do insertion
            for (int i = 0; i < rows.size(); i++) {
                Sqlizable[] cols = rows.get(i);
                if (cols.length != column_names.length) {
                    throw new UnsupportedOperationException("column size missmatch");
                }
                for (int col_id = 0; col_id < cols.length; col_id++) {
                    cols[col_id].toDB(col_id + 1, preparedStatement);
                }
                preparedStatement.executeUpdate();
            }
            // commit
            conn.commit();
        } catch (Throwable e) {
            // rollback
            if (conn != null) {
                try {
                    conn.rollback();
                    System.out.println("Transaction rolled back");
                } catch (SQLException rollbackEx) {
                    rollbackEx.printStackTrace();
                    throw rollbackEx;
                }
            }
            e.printStackTrace();
            throw e;
        } finally {
            // reset autocommit
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    e.printStackTrace();
                    throw e;
                }
            }
        }
    }

    public void delete(String table_name, String primary_key_name, ArrayList<Sqlizable> primary_keys) throws Throwable
    {
        Statement statement = null;

        try {
            statement = conn.createStatement();
            ArrayList<String> param_str_list = new ArrayList<String>(Collections.nCopies(primary_keys.size(), "?"));
            String sql = String.format("DELETE FROM %s WHERE %s in (%s)", 
                            table_name,
                            primary_key_name,
                            String.join(", ", param_str_list));
            PreparedStatement preparedStatement = conn.prepareStatement(sql);
            for (int i = 0; i < primary_keys.size(); i++) {
                primary_keys.get(i).toDB(i + 1, preparedStatement);
            }
            preparedStatement.executeUpdate();
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        } finally {
            try {
                if (statement != null) statement.close();
            } catch (SQLException e) {
                e.printStackTrace();
                throw e;
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
                      String where_expr) throws Throwable
    {
        ArrayList<HashMap<String, Sqlizable>> res = new ArrayList<>();

        Statement statement = null;
        ResultSet resultSet = null;

        String metric_type_lower = metric_type.toLowerCase();
        if (!metric_type_lower.equals("l2") &&
            !metric_type_lower.equals("ip") &&
            !metric_type_lower.equals("cosine")) {
            throw new UnsupportedOperationException("Metric Type is not supported.");
        }
        String dist_func = "l2_distance";
        if (metric_type_lower.equals("ip")) {
            dist_func = "negative_inner_product";
        } else if (metric_type_lower.equals("cosine")) {
            dist_func = "cosine_distance";
        }
        
        String[] vec_str = new String[qv.length];
        for (int i = 0; i < qv.length; i++) {
            vec_str[i] = String.valueOf(qv[i]);
        }

        try {
            statement = conn.createStatement();            
            
            String sql = String.format("SELECT %s FROM %s WHERE %s ORDER BY %s(%s, '[%s]') APPROXIMATE LIMIT %d",
                            String.join(", ", output_fields),
                            table_name,
                            (where_expr == null) ? "1" : where_expr,
                            dist_func,
                            vec_col_name,
                            String.join(", ", vec_str),
                            topk);
            resultSet = statement.executeQuery(sql);
            ResultSetMetaData metaData = resultSet.getMetaData();
            int project_count = metaData.getColumnCount();
            if (project_count != output_datatypes.length) {
                throw new IllegalArgumentException(" the length of output_datatypes: " + output_datatypes.length + " mismatched with projected column size: " + project_count);
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
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        } finally {
            try {
                if (resultSet != null) resultSet.close();
                if (statement != null) statement.close();
            } catch (SQLException e) {
                e.printStackTrace();
                throw e;
            }
        }
        return res;
    }

    /**
     * Full-text + vector hybrid search
     * 
     * @param table_name Table name
     * @param vec_col_name Vector column name
     * @param metric_type Distance metric type, supports "l2", "ip", "cosine"
     * @param qv Query vector
     * @param text_fields Full-text search field list
     * @param text_query Full-text search query text
     * @param filter_expr Scalar filter condition (String SQL WHERE expression or Filter object, optional)
     * @param topk Number of results to return
     * @param output_fields Output field list
     * @param output_datatypes Output field data types
     * @param rank_window_size RRF ranking window size (optional, default equals topk)
     * @return Search result list
     * @throws Throwable
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
            Integer rank_window_size) throws Throwable {
        
        // Filter out null and empty strings from text_fields
        if (text_fields != null && text_fields.length > 0) {
            java.util.ArrayList<String> validTextFields = new java.util.ArrayList<>();
            for (String field : text_fields) {
                if (field != null) {
                    String trimmedField = field.trim();
                    if (!trimmedField.isEmpty()) {
                        validTextFields.add(trimmedField);
                    }
                }
            }
            if (validTextFields.isEmpty()) {
                throw new IllegalArgumentException("Full-text search fields cannot be empty after filtering null/empty values");
            }
            text_fields = validTextFields.toArray(new String[0]);
        }

        // Filter out null and empty strings from output_fields and sync output_datatypes
        if (output_fields != null && output_fields.length > 0) {
            java.util.ArrayList<String> validOutputFields = new java.util.ArrayList<>();
            java.util.ArrayList<DataType> validOutputDataTypes = new java.util.ArrayList<>();
            for (int i = 0; i < output_fields.length; i++) {
                String field = output_fields[i];
                if (field != null) {
                    String trimmedField = field.trim();
                    if (!trimmedField.isEmpty()) {
                        validOutputFields.add(trimmedField);
                        if (output_datatypes != null && i < output_datatypes.length) {
                            validOutputDataTypes.add(output_datatypes[i]);
                        }
                    }
                }
            }
            if (validOutputFields.isEmpty()) {
                throw new IllegalArgumentException("Output fields cannot be empty after filtering null/empty values");
            }
            output_fields = validOutputFields.toArray(new String[0]);
            if (output_datatypes != null) {
                output_datatypes = validOutputDataTypes.toArray(new DataType[0]);
            }
        }

        // Convert filter expression to SQL WHERE clause
        String whereExpr = convertFilterToWhereClause(filter_expr);
        
        // Perform vector search (get more results for RRF)
        int vectorTopk = (rank_window_size != null && rank_window_size > 0) ? rank_window_size : topk * 2;
        ArrayList<HashMap<String, Sqlizable>> vectorResults = performVectorSearch(
            table_name, vec_col_name, metric_type, qv, whereExpr, vectorTopk, output_fields, output_datatypes);
        
        // Perform fulltext search (get more results for RRF)
        int fulltextTopk = (rank_window_size != null && rank_window_size > 0) ? rank_window_size : topk * 2;
        ArrayList<HashMap<String, Sqlizable>> fulltextResults = performFulltextSearch(
            table_name, text_fields, text_query, whereExpr, fulltextTopk, output_fields, output_datatypes);
        
        // Combine results using RRF
        ArrayList<HashMap<String, Sqlizable>> combinedResults = combineHybridResults(
            vectorResults, fulltextResults, output_fields, topk, rank_window_size);
        
        return combinedResults;
    }

    /**
     * Scalar + vector hybrid search
     * 
     * @param table_name Table name
     * @param vec_col_name Vector column name
     * @param metric_type Distance metric type, supports "l2", "ip", "cosine"
     * @param qv Query vector
     * @param filter_expr Scalar filter condition (String SQL WHERE expression or Filter object)
     * @param topk Number of results to return
     * @param output_fields Output field list
     * @param output_datatypes Output field data types
     * @return Search result list
     * @throws Throwable
     */
    public ArrayList<HashMap<String, Sqlizable>> hybridScalarVectorSearch(
            String table_name,
            String vec_col_name,
            String metric_type,
            float[] qv,
            Object filter_expr,
            int topk,
            String[] output_fields,
            DataType[] output_datatypes) throws Throwable {
        
        // Filter out null and empty strings from output_fields and sync output_datatypes
        if (output_fields != null && output_fields.length > 0) {
            java.util.ArrayList<String> validOutputFields = new java.util.ArrayList<>();
            java.util.ArrayList<DataType> validOutputDataTypes = new java.util.ArrayList<>();
            for (int i = 0; i < output_fields.length; i++) {
                String field = output_fields[i];
                if (field != null) {
                    String trimmedField = field.trim();
                    if (!trimmedField.isEmpty()) {
                        validOutputFields.add(trimmedField);
                        if (output_datatypes != null && i < output_datatypes.length) {
                            validOutputDataTypes.add(output_datatypes[i]);
                        }
                    }
                }
            }
            if (validOutputFields.isEmpty()) {
                throw new IllegalArgumentException("Output fields cannot be empty after filtering null/empty values");
            }
            output_fields = validOutputFields.toArray(new String[0]);
            if (output_datatypes != null) {
                output_datatypes = validOutputDataTypes.toArray(new DataType[0]);
            }
        }

        // Convert filter expression to SQL WHERE clause
        String whereExpr = convertFilterToWhereClause(filter_expr);
        
        // Perform vector search with scalar filter in WHERE clause
        // For scalar + vector hybrid search, the scalar filter is applied in the WHERE clause
        // during vector search, so we only need to call performVectorSearch
        return performVectorSearch(
            table_name, vec_col_name, metric_type, qv, whereExpr, topk, output_fields, output_datatypes);
    }

    /**
     * Parses a filter expression (either String or Filter object) into OceanBase JSON format.
     * Supports both legacy string expressions and new Filter objects.
     *
     * @deprecated This method is no longer used. We now use SQL WHERE clauses instead of JSON filter format.
     * @param filterExpr Filter expression (String or Filter object)
     * @return JSONArray containing the filter structure, or null if filter is null/empty
     */
    @Deprecated
    private JSONArray parseFilterExpression(Object filterExpr) {
        if (filterExpr == null) {
            return null;
        }

        // If it's a Filter object, use FilterMapper
        if (filterExpr instanceof Filter) {
            return FilterMapper.toOceanBaseJson((Filter) filterExpr);
        }

        if (filterExpr instanceof String) {
            String filterStr = (String) filterExpr;
            if (filterStr.trim().isEmpty()) {
                return null;
            }
            return parseStringFilterExpression(filterStr);
        }

        throw new IllegalArgumentException("Filter expression must be either String or Filter object");
    }

    /**
     * @deprecated This method is only used by deprecated parseFilterExpression method.
     */
    @Deprecated
    private JSONArray parseStringFilterExpression(String filter_expr) {
        if (filter_expr == null || filter_expr.trim().isEmpty()) {
            return null;
        }

        JSONArray filterArray = new JSONArray();
        JSONObject boolObj = new JSONObject();
        JSONArray mustArray = new JSONArray();
        
        // Used to merge range conditions for the same field
        HashMap<String, JSONObject> rangeMap = new HashMap<>();
        
        // Simple parsing: split conditions by AND
        String[] conditions = filter_expr.split("\\s+AND\\s+");
        for (String condition : conditions) {
            condition = condition.trim();
            // Parse single condition
            if (condition.contains(">=")) {
                String[] parts = condition.split("\\s*>=\\s*");
                if (parts.length == 2) {
                    String field = parts[0].trim();
                    String value = parts[1].trim();
                    // Get or create range object for this field
                    JSONObject fieldRangeObj = rangeMap.get(field);
                    if (fieldRangeObj == null) {
                        fieldRangeObj = new JSONObject();
                        rangeMap.put(field, fieldRangeObj);
                    }
                    fieldRangeObj.put("gte", Double.parseDouble(value));
                }
            } else if (condition.contains("<=")) {
                String[] parts = condition.split("\\s*<=\\s*");
                if (parts.length == 2) {
                    String field = parts[0].trim();
                    String value = parts[1].trim();
                    // Get or create range object for this field
                    JSONObject fieldRangeObj = rangeMap.get(field);
                    if (fieldRangeObj == null) {
                        fieldRangeObj = new JSONObject();
                        rangeMap.put(field, fieldRangeObj);
                    }
                    fieldRangeObj.put("lte", Double.parseDouble(value));
                }
            } else if (condition.contains(">") && !condition.contains(">=")) {
                String[] parts = condition.split("\\s*>\\s*");
                if (parts.length == 2) {
                    String field = parts[0].trim();
                    String value = parts[1].trim();
                    // Get or create range object for this field
                    JSONObject fieldRangeObj = rangeMap.get(field);
                    if (fieldRangeObj == null) {
                        fieldRangeObj = new JSONObject();
                        rangeMap.put(field, fieldRangeObj);
                    }
                    fieldRangeObj.put("gt", Double.parseDouble(value));
                }
            } else if (condition.contains("<") && !condition.contains("<=")) {
                String[] parts = condition.split("\\s*<\\s*");
                if (parts.length == 2) {
                    String field = parts[0].trim();
                    String value = parts[1].trim();
                    // Get or create range object for this field
                    JSONObject fieldRangeObj = rangeMap.get(field);
                    if (fieldRangeObj == null) {
                        fieldRangeObj = new JSONObject();
                        rangeMap.put(field, fieldRangeObj);
                    }
                    fieldRangeObj.put("lt", Double.parseDouble(value));
                }
            } else if (condition.contains("=")) {
                String[] parts = condition.split("\\s*=\\s*");
                if (parts.length == 2) {
                    String field = parts[0].trim();
                    String value = parts[1].trim();
                    JSONObject termObj = new JSONObject();
                    JSONObject fieldObj = new JSONObject();
                    // Try to parse as number or string
                    try {
                        if (value.contains(".")) {
                            fieldObj.put(field, Double.parseDouble(value));
                        } else {
                            fieldObj.put(field, Long.parseLong(value));
                        }
                    } catch (NumberFormatException e) {
                        fieldObj.put(field, value);
                    }
                    termObj.put("term", fieldObj);
                    mustArray.add(termObj);
                }
            }
        }
        
        // Add merged range conditions to mustArray
        for (HashMap.Entry<String, JSONObject> entry : rangeMap.entrySet()) {
            String field = entry.getKey();
            JSONObject fieldRangeObj = entry.getValue();
            JSONObject rangeObj = new JSONObject();
            JSONObject rangeFieldObj = new JSONObject();
            rangeFieldObj.put(field, fieldRangeObj);
            rangeObj.put("range", rangeFieldObj);
            mustArray.add(rangeObj);
        }
        
        JSONObject boolMustObj = new JSONObject();
        boolMustObj.put("must", mustArray);
        boolObj.put("bool", boolMustObj);
        filterArray.add(boolObj);
        return filterArray;
    }

    /**
     * Convert filter expression to SQL WHERE clause
     * Supports both String (legacy) and Filter object
     */
    private String convertFilterToWhereClause(Object filterExpr) {
        if (filterExpr == null) {
            return null;
        }
        
        // If it's a String, use it directly as WHERE clause
        if (filterExpr instanceof String) {
            String filterStr = ((String) filterExpr).trim();
            return filterStr.isEmpty() ? null : filterStr;
        }
        
        // If it's a Filter object, convert to SQL WHERE clause
        if (filterExpr instanceof Filter) {
            return convertFilterObjectToWhereClause((Filter) filterExpr);
        }
        
        throw new IllegalArgumentException("Filter expression must be either String or Filter object");
    }
    
    /**
     * Convert Filter object to SQL WHERE clause
     */
    private String convertFilterObjectToWhereClause(Filter filter) {
        if (filter == null) {
            return null;
        }
        
        Filter.Type type = filter.getType();
        String key = filter.getKey();
        Object value = filter.getValue();
        
        switch (type) {
            case EQUAL:
                return formatWhereCondition(key, "=", value);
            case NOT_EQUAL:
                return formatWhereCondition(key, "!=", value);
            case GREATER_THAN:
                return formatWhereCondition(key, ">", value);
            case GREATER_THAN_OR_EQUAL:
                return formatWhereCondition(key, ">=", value);
            case LESS_THAN:
                return formatWhereCondition(key, "<", value);
            case LESS_THAN_OR_EQUAL:
                return formatWhereCondition(key, "<=", value);
            case IN:
                return formatInCondition(key, filter.getValues());
            case NOT_IN:
                return formatNotInCondition(key, filter.getValues());
            case CONTAINS:
                return formatWhereCondition(key, "LIKE", "%" + value + "%");
            case AND:
                String left = convertFilterObjectToWhereClause(filter.getLeft());
                String right = convertFilterObjectToWhereClause(filter.getRight());
                if (left != null && right != null) {
                    return "(" + left + ") AND (" + right + ")";
                } else if (left != null) {
                    return left;
                } else if (right != null) {
                    return right;
                }
                return null;
            case OR:
                left = convertFilterObjectToWhereClause(filter.getLeft());
                right = convertFilterObjectToWhereClause(filter.getRight());
                if (left != null && right != null) {
                    return "(" + left + ") OR (" + right + ")";
                } else if (left != null) {
                    return left;
                } else if (right != null) {
                    return right;
                }
                return null;
            case NOT:
                String expr = convertFilterObjectToWhereClause(filter.getExpression());
                return expr != null ? "NOT (" + expr + ")" : null;
            default:
                throw new UnsupportedOperationException("Filter type " + type + " is not supported");
        }
    }
    
    /**
     * Format WHERE condition
     */
    private String formatWhereCondition(String key, String operator, Object value) {
        if (value instanceof String) {
            return "`" + key + "` " + operator + " '" + escapeSqlString((String) value) + "'";
        } else {
            return "`" + key + "` " + operator + " " + value;
        }
    }
    
    /**
     * Format IN condition
     */
    private String formatInCondition(String key, java.util.Collection<?> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        java.util.ArrayList<String> valueStrs = new java.util.ArrayList<>();
        for (Object val : values) {
            if (val instanceof String) {
                valueStrs.add("'" + escapeSqlString((String) val) + "'");
            } else {
                valueStrs.add(String.valueOf(val));
            }
        }
        return "`" + key + "` IN (" + String.join(", ", valueStrs) + ")";
    }
    
    /**
     * Format NOT IN condition
     */
    private String formatNotInCondition(String key, java.util.Collection<?> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        java.util.ArrayList<String> valueStrs = new java.util.ArrayList<>();
        for (Object val : values) {
            if (val instanceof String) {
                valueStrs.add("'" + escapeSqlString((String) val) + "'");
            } else {
                valueStrs.add(String.valueOf(val));
            }
        }
        return "`" + key + "` NOT IN (" + String.join(", ", valueStrs) + ")";
    }
    
    /**
     * Escape SQL string
     */
    private String escapeSqlString(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("'", "''").replace("\\", "\\\\");
    }

    /**
     * @deprecated This method is no longer used. We now use SQL directly instead of DBMS_HYBRID_SEARCH.SEARCH.
     */
    @Deprecated
    private Object cleanEmptyStringsFromJson(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof JSONArray) {
            JSONArray array = (JSONArray) obj;
            JSONArray cleanedArray = new JSONArray();
            for (Object item : array) {
                if (item instanceof String) {
                    String str = ((String) item).trim();
                    if (!str.isEmpty()) {
                        cleanedArray.add(str);
                    }
                } else {
                    Object cleanedItem = cleanEmptyStringsFromJson(item);
                    if (cleanedItem != null) {
                        // Don't add empty arrays
                        if (cleanedItem instanceof JSONArray) {
                            JSONArray cleanedItemArray = (JSONArray) cleanedItem;
                            if (cleanedItemArray.size() > 0) {
                                cleanedArray.add(cleanedItem);
                            }
                        } else {
                            cleanedArray.add(cleanedItem);
                        }
                    }
                }
            }
            // Return null if array is empty (will be filtered out by caller)
            return cleanedArray.size() > 0 ? cleanedArray : null;
        } else if (obj instanceof JSONObject) {
            JSONObject jsonObj = (JSONObject) obj;
            JSONObject cleanedObj = new JSONObject();
            for (Object keyObj : jsonObj.keySet()) {
                String key = (String) keyObj;
                // Skip empty keys
                if (key == null || key.trim().isEmpty()) {
                    continue;
                }
                String trimmedKey = key.trim();
                Object value = jsonObj.get(key);
                Object cleanedValue = cleanEmptyStringsFromJson(value);
                if (cleanedValue != null) {
                    // Don't add empty arrays or empty objects
                    if (cleanedValue instanceof JSONArray) {
                        JSONArray cleanedValueArray = (JSONArray) cleanedValue;
                        if (cleanedValueArray.size() > 0) {
                            cleanedObj.put(trimmedKey, cleanedValue);
                        }
                    } else if (cleanedValue instanceof JSONObject) {
                        JSONObject cleanedValueObj = (JSONObject) cleanedValue;
                        if (cleanedValueObj.size() > 0) {
                            cleanedObj.put(trimmedKey, cleanedValue);
                        }
                    } else {
                        cleanedObj.put(trimmedKey, cleanedValue);
                    }
                }
            }
            return cleanedObj.size() > 0 ? cleanedObj : null;
        }
        return obj;
    }

    /**
     * Perform vector similarity search using SQL
     */
    private ArrayList<HashMap<String, Sqlizable>> performVectorSearch(
            String table_name,
            String vec_col_name,
            String metric_type,
            float[] qv,
            String whereExpr,
            int topk,
            String[] output_fields,
            DataType[] output_datatypes) throws Throwable {
        ArrayList<HashMap<String, Sqlizable>> res = new ArrayList<>();
        
        String metric_type_lower = metric_type.toLowerCase();
        if (!metric_type_lower.equals("l2") &&
            !metric_type_lower.equals("ip") &&
            !metric_type_lower.equals("cosine")) {
            throw new UnsupportedOperationException("Metric Type is not supported.");
        }
        
        String dist_func = "l2_distance";
        if (metric_type_lower.equals("ip")) {
            dist_func = "negative_inner_product";
        } else if (metric_type_lower.equals("cosine")) {
            dist_func = "cosine_distance";
        }
        
        // Build vector string
        String[] vec_str = new String[qv.length];
        for (int i = 0; i < qv.length; i++) {
            vec_str[i] = String.valueOf(qv[i]);
        }
        String vectorStr = "[" + String.join(", ", vec_str) + "]";
        
        // Build SQL query
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        for (int i = 0; i < output_fields.length; i++) {
            if (i > 0) sql.append(", ");
            sql.append("`").append(output_fields[i]).append("`");
        }
        sql.append(", ").append(dist_func).append("(`").append(vec_col_name).append("`, ?) as distance");
        
        // Calculate score based on metric type
        if ("cosine".equalsIgnoreCase(metric_type)) {
            sql.append(", (2 - ").append(dist_func).append("(`").append(vec_col_name).append("`, ?)) / 2 as score");
        } else if ("l2".equalsIgnoreCase(metric_type)) {
            sql.append(", 1 / (1 + ").append(dist_func).append("(`").append(vec_col_name).append("`, ?)) as score");
        } else if ("ip".equalsIgnoreCase(metric_type) || "inner_product".equalsIgnoreCase(metric_type)) {
            sql.append(", (").append(dist_func).append("(`").append(vec_col_name).append("`, ?) + 1) / 2 as score");
        } else {
            sql.append(", (2 - ").append(dist_func).append("(`").append(vec_col_name).append("`, ?)) / 2 as score");
        }
        
        sql.append(" FROM `").append(table_name).append("`");
        
        if (whereExpr != null && !whereExpr.trim().isEmpty()) {
            sql.append(" WHERE ").append(whereExpr);
        }
        
        sql.append(" ORDER BY ").append(dist_func).append("(`").append(vec_col_name).append("`, ?) ASC");
        sql.append(" APPROXIMATE LIMIT ?");
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            pstmt.setString(1, vectorStr);  // distance
            pstmt.setString(2, vectorStr);  // score
            pstmt.setString(3, vectorStr);  // ORDER BY
            pstmt.setInt(4, topk);          // LIMIT
            
            try (ResultSet rs = pstmt.executeQuery()) {
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                
                while (rs.next()) {
                    HashMap<String, Sqlizable> row = new HashMap<>();
                    // Add output fields
                    for (int i = 1; i <= output_fields.length; i++) {
                        String columnName = output_fields[i - 1];
                        Sqlizable sqlizable = SqlizableFactory.build(output_datatypes[i - 1], rs, columnName);
                        row.put(columnName, sqlizable);
                    }
                    // Add distance for vector search
                    try {
                        double distance = rs.getDouble("distance");
                        row.put("distance", new SqlDouble(distance));
                    } catch (SQLException e) {
                        // Ignore if distance column doesn't exist
                    }
                    res.add(row);
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        }
        
        return res;
    }
    
    /**
     * Perform full-text search using SQL MATCH AGAINST
     */
    private ArrayList<HashMap<String, Sqlizable>> performFulltextSearch(
            String table_name,
            String[] text_fields,
            String text_query,
            String whereExpr,
            int topk,
            String[] output_fields,
            DataType[] output_datatypes) throws Throwable {
        ArrayList<HashMap<String, Sqlizable>> res = new ArrayList<>();
        
        if (text_fields == null || text_fields.length == 0) {
            return res;
        }
        
        // Build MATCH clause - use first field for simplicity
        // For multiple fields, we can use OR or combine them
        String matchField = text_fields[0];
        StringBuilder matchClause = new StringBuilder();
        if (text_fields.length == 1) {
            matchClause.append("MATCH(`").append(matchField).append("`) AGAINST(? IN NATURAL LANGUAGE MODE)");
        } else {
            // Multiple fields: use OR
            matchClause.append("(");
            for (int i = 0; i < text_fields.length; i++) {
                if (i > 0) matchClause.append(" OR ");
                matchClause.append("MATCH(`").append(text_fields[i]).append("`) AGAINST(? IN NATURAL LANGUAGE MODE)");
            }
            matchClause.append(")");
        }
        
        // Build SQL query
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        for (int i = 0; i < output_fields.length; i++) {
            if (i > 0) sql.append(", ");
            sql.append("`").append(output_fields[i]).append("`");
        }
        sql.append(", ").append(matchClause).append(" as score");
        sql.append(" FROM `").append(table_name).append("`");
        sql.append(" WHERE ").append(matchClause);
        
        if (whereExpr != null && !whereExpr.trim().isEmpty()) {
            sql.append(" AND ").append(whereExpr);
        }
        
        sql.append(" ORDER BY score DESC LIMIT ?");
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            // Set query text for each MATCH clause
            for (int i = 0; i < text_fields.length; i++) {
                pstmt.setString(i + 1, text_query);
            }
            for (int i = 0; i < text_fields.length; i++) {
                pstmt.setString(text_fields.length + i + 1, text_query);
            }
            pstmt.setInt(text_fields.length * 2 + 1, topk);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    HashMap<String, Sqlizable> row = new HashMap<>();
                    for (int i = 0; i < output_fields.length; i++) {
                        String columnName = output_fields[i];
                        Sqlizable sqlizable = SqlizableFactory.build(output_datatypes[i], rs, columnName);
                        row.put(columnName, sqlizable);
                    }
                    res.add(row);
                }
            }
        } catch (Throwable e) {
            // If fulltext search fails (e.g., no fulltext index), return empty results
            System.err.println("WARNING: Fulltext search failed, returning empty results: " + e.getMessage());
            return new ArrayList<>();
        }
        
        return res;
    }
    
    /**
     * Combine vector and fulltext search results using RRF (Reciprocal Rank Fusion)
     */
    private ArrayList<HashMap<String, Sqlizable>> combineHybridResults(
            ArrayList<HashMap<String, Sqlizable>> vectorResults,
            ArrayList<HashMap<String, Sqlizable>> fulltextResults,
            String[] output_fields,
            int topk,
            Integer rankWindowSize) {
        if (vectorResults == null) vectorResults = new ArrayList<>();
        if (fulltextResults == null) fulltextResults = new ArrayList<>();
        
        // Use rank window size if provided, otherwise use topk
        final int k = (rankWindowSize != null && rankWindowSize > 0) ? rankWindowSize : topk;
        
        // Create a map to store combined results by a unique key
        // For simplicity, we'll use the first output field as the key
        // In practice, you might want to use a primary key or unique identifier
        HashMap<String, HashMap<String, Sqlizable>> resultMap = new HashMap<>();
        HashMap<String, Double> rrfScores = new HashMap<>();
        
        // Process vector results
        for (int i = 0; i < vectorResults.size(); i++) {
            HashMap<String, Sqlizable> row = vectorResults.get(i);
            String key = generateRowKey(row, output_fields);
            if (key != null) {
                resultMap.put(key, row);
                int rank = i + 1;
                rrfScores.put(key, rrfScores.getOrDefault(key, 0.0) + 1.0 / (k + rank));
            }
        }
        
        // Process fulltext results
        for (int i = 0; i < fulltextResults.size(); i++) {
            HashMap<String, Sqlizable> row = fulltextResults.get(i);
            String key = generateRowKey(row, output_fields);
            if (key != null) {
                if (!resultMap.containsKey(key)) {
                    resultMap.put(key, row);
                }
                int rank = i + 1;
                rrfScores.put(key, rrfScores.getOrDefault(key, 0.0) + 1.0 / (k + rank));
            }
        }
        
        // Sort by RRF score and return topk
        ArrayList<HashMap<String, Sqlizable>> combinedResults = new ArrayList<>(resultMap.values());
        combinedResults.sort((a, b) -> {
            String keyA = generateRowKey(a, output_fields);
            String keyB = generateRowKey(b, output_fields);
            double scoreA = rrfScores.getOrDefault(keyA, 0.0);
            double scoreB = rrfScores.getOrDefault(keyB, 0.0);
            return Double.compare(scoreB, scoreA);
        });
        
        // Return topk results
        ArrayList<HashMap<String, Sqlizable>> finalResults = new ArrayList<>();
        for (int i = 0; i < Math.min(topk, combinedResults.size()); i++) {
            finalResults.add(combinedResults.get(i));
        }
        
        return finalResults;
    }
    
    /**
     * Generate a unique key for a row (for RRF combination)
     * Uses the first output field as the key
     */
    private String generateRowKey(HashMap<String, Sqlizable> row, String[] output_fields) {
        if (output_fields == null || output_fields.length == 0 || row == null) {
            return null;
        }
        Sqlizable value = row.get(output_fields[0]);
        return value != null ? value.toString() : null;
    }

    /**
     * @deprecated This method is no longer used. We now use SQL directly instead of DBMS_HYBRID_SEARCH.SEARCH.
     */
    @Deprecated
    private String escapeJsonForSql(String json) {
        if (json == null) {
            return "''";
        }
        String escaped = json.replace("'", "''").replace("\\", "\\\\");
        return "'" + escaped + "'";
    }

    /**
     * @deprecated This method is no longer used. We now use SQL directly instead of DBMS_HYBRID_SEARCH.SEARCH.
     */
    @Deprecated
    private Sqlizable createSqlizableFromValue(DataType dataType, Object value) throws Exception {
        if (value == null) {
            return null;
        }

        switch (dataType) {
            case INT32:
                if (value instanceof Number) {
                    return new SqlInteger(((Number) value).intValue());
                }
                return new SqlInteger(Integer.parseInt(value.toString()));
            case INT64:
                if (value instanceof Number) {
                    return new SqlInteger(((Number) value).intValue());
                }
                return new SqlInteger(Integer.parseInt(value.toString()));
            case FLOAT:
                if (value instanceof Number) {
                    return new SqlFloat(((Number) value).floatValue());
                }
                return new SqlFloat(Float.parseFloat(value.toString()));
            case DOUBLE:
                if (value instanceof Number) {
                    return new SqlDouble(((Number) value).doubleValue());
                }
                return new SqlDouble(Double.parseDouble(value.toString()));
            case STRING:
            case VARCHAR:
                return new SqlText(value.toString());
            case FLOAT_VECTOR:
                // Vector type needs special handling
                if (value instanceof JSONArray) {
                    JSONArray vecArray = (JSONArray) value;
                    float[] vec = new float[vecArray.size()];
                    for (int i = 0; i < vecArray.size(); i++) {
                        vec[i] = ((Number) vecArray.get(i)).floatValue();
                    }
                    return new SqlVector(vec);
                } else if (value instanceof String) {
                    // Parse string format vector "[1.0, 2.0, 3.0]"
                    String vecStr = value.toString().trim();
                    if (vecStr.startsWith("[") && vecStr.endsWith("]")) {
                        vecStr = vecStr.substring(1, vecStr.length() - 1);
                        String[] parts = vecStr.split(",");
                        float[] vec = new float[parts.length];
                        for (int i = 0; i < parts.length; i++) {
                            vec[i] = Float.parseFloat(parts[i].trim());
                        }
                        return new SqlVector(vec);
                    }
                }
                throw new IllegalArgumentException("Cannot convert value to vector: " + value);
            default:
                return new SqlText(value.toString());
        }
    }

    /**
     * Auto-infer data type based on table name and column name
     * @param tableName Table name
     * @param columnName Column name
     * @return Inferred data type
     */
    protected DataType inferDataType(String tableName, String columnName) throws Throwable {
        if (columnName == null || columnName.trim().isEmpty()) {
            throw new IllegalArgumentException("Column name cannot be null or empty");
        }
        try {
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet rs = metaData.getColumns(null, null, tableName, columnName);
            
            if (rs.next()) {
                String typeName = rs.getString("TYPE_NAME");
                if (typeName == null) {
                    return DataType.STRING;
                }
                typeName = typeName.toUpperCase();
                
                // Map SQL type to DataType
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
                    // Default return STRING
                    return DataType.STRING;
                }
            }
            
            // If field not found, default return STRING
            return DataType.STRING;
        } catch (SQLException e) {
            e.printStackTrace();
            // On error, default return STRING
            return DataType.STRING;
        }
    }

    /**
     * Create full-text + vector hybrid search builder
     * Usage example:
     * <pre>{@code
     * ArrayList<HashMap<String, Sqlizable>> results = ob.textVectorSearch()
     *     .table("HYBRID_TEXT_VECTOR_TEST")
     *     .queryVector(queryVector)
     *     .textFields("title", "content")
     *     .textQuery("OceanBase database")
     *     .topk(3)
     *     .outputFields(new String[]{"title", "content"}, 
     *                   new DataType[]{DataType.STRING, DataType.STRING})
     *     .search();
     * }</pre>
     */
    public HybridTextVectorSearchBuilder textVectorSearch() {
        return new HybridTextVectorSearchBuilder(this);
    }

    /**
     * Create scalar + vector hybrid search builder
     * Usage example:
     * <pre>{@code
     * ArrayList<HashMap<String, Sqlizable>> results = ob.scalarVectorSearch()
     *     .table("HYBRID_SCALAR_VECTOR_TEST")
     *     .queryVector(queryVector)
     *     .filter("category_id = 1 AND price >= 50 AND price <= 250")
     *     .topk(5)
     *     .outputFields(new String[]{"price", "category_id"}, 
     *                   new DataType[]{DataType.DOUBLE, DataType.INT32})
     *     .search();
     * }</pre>
     */
    public HybridScalarVectorSearchBuilder scalarVectorSearch() {
        return new HybridScalarVectorSearchBuilder(this);
    }
}
