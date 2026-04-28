package com.oceanbase.obvec_jdbc;

import com.oceanbase.obvec_jdbc.filter.Filter;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Builder for full-text + vector hybrid search
 * Used to simplify the call to hybridTextVectorSearch method
 */
public class HybridTextVectorSearchBuilder {
    private final ObVecClient client;
    private String tableName;
    private String vecColumnName = "embedding";
    private String metricType = "cosine";
    private float[] queryVector;
    private String[] textFields;
    private String textQuery;
    private Object filterExpr; // Can be String (legacy) or Filter object
    private int topk = 10;
    private String[] outputFields;
    private DataType[] outputDataTypes;
    private Integer rankWindowSize = null;

    public HybridTextVectorSearchBuilder(ObVecClient client) {
        this.client = client;
    }

    /**
     * Set table name
     */
    public HybridTextVectorSearchBuilder table(String tableName) {
        this.tableName = tableName;
        return this;
    }

    /**
     * Set vector column name (default: "embedding")
     */
    public HybridTextVectorSearchBuilder vectorColumn(String vecColumnName) {
        this.vecColumnName = vecColumnName;
        return this;
    }

    /**
     * Set distance metric type (default: "cosine")
     * Supported values: "l2", "ip", "cosine"
     */
    public HybridTextVectorSearchBuilder metric(String metricType) {
        this.metricType = metricType;
        return this;
    }

    /**
     * Set query vector
     */
    public HybridTextVectorSearchBuilder queryVector(float[] queryVector) {
        this.queryVector = queryVector;
        return this;
    }

    /**
     * Set full-text search field (single field)
     */
    public HybridTextVectorSearchBuilder textField(String textField) {
        this.textFields = new String[]{textField};
        return this;
    }

    /**
     * Set full-text search fields (multiple fields)
     */
    public HybridTextVectorSearchBuilder textFields(String... textFields) {
        this.textFields = textFields;
        return this;
    }

    /**
     * Set full-text search query text
     */
    public HybridTextVectorSearchBuilder textQuery(String textQuery) {
        this.textQuery = textQuery;
        return this;
    }

    /**
     * Set number of results to return (default: 10)
     */
    public HybridTextVectorSearchBuilder topk(int topk) {
        this.topk = topk;
        return this;
    }

    /**
     * Set output field (single field, auto-infer type)
     */
    public HybridTextVectorSearchBuilder outputField(String fieldName) {
        this.outputFields = new String[]{fieldName};
        this.outputDataTypes = null; // Will be auto-inferred in search()
        return this;
    }

    /**
     * Set output field (single field, specify type)
     */
    public HybridTextVectorSearchBuilder outputField(String fieldName, DataType dataType) {
        this.outputFields = new String[]{fieldName};
        this.outputDataTypes = new DataType[]{dataType};
        return this;
    }

    /**
     * Set output fields (multiple fields, auto-infer type)
     */
    public HybridTextVectorSearchBuilder outputFields(String... fieldNames) {
        this.outputFields = fieldNames;
        this.outputDataTypes = null; // Will be auto-inferred in search()
        return this;
    }

    /**
     * Set output fields (multiple fields, specify types)
     */
    public HybridTextVectorSearchBuilder outputFields(String[] fieldNames, DataType[] dataTypes) {
        this.outputFields = fieldNames;
        this.outputDataTypes = dataTypes;
        return this;
    }

    /**
     * Set output fields (convenience method: return all fields)
     * Note: If this method is not called and outputFields is not set, textFields will be used by default
     */
    public HybridTextVectorSearchBuilder outputAll() {
        // If not set, will automatically get all fields during execution
        this.outputFields = null;
        this.outputDataTypes = null;
        return this;
    }

    /**
     * Set filter condition (SQL WHERE expression - legacy method)
     * Example: "category_id = 1 AND price >= 50 AND price <= 250"
     */
    public HybridTextVectorSearchBuilder filter(String filterExpr) {
        this.filterExpr = filterExpr;
        return this;
    }

    /**
     * Set filter condition using Filter object (type-safe method)
     * Example: FilterBuilder.key("category_id").isEqualTo(1)
     */
    public HybridTextVectorSearchBuilder filter(Filter filter) {
        this.filterExpr = filter;
        return this;
    }

    /**
     * Set RRF ranking window size (optional)
     */
    public HybridTextVectorSearchBuilder rankWindowSize(Integer rankWindowSize) {
        this.rankWindowSize = rankWindowSize;
        return this;
    }

    /**
     * Execute search
     */
    public ArrayList<HashMap<String, Sqlizable>> search() throws Throwable {
        // Parameter validation
        if (tableName == null || tableName.isEmpty()) {
            throw new IllegalArgumentException("Table name cannot be empty");
        }
        if (queryVector == null || queryVector.length == 0) {
            throw new IllegalArgumentException("Query vector cannot be empty");
        }
        if (textFields == null || textFields.length == 0) {
            throw new IllegalArgumentException("Full-text search fields cannot be empty");
        }
        if (textQuery == null || textQuery.isEmpty()) {
            throw new IllegalArgumentException("Full-text search query text cannot be empty");
        }
        
        // Filter out null and empty strings from textFields
        java.util.ArrayList<String> validTextFields = new java.util.ArrayList<>();
        for (String field : textFields) {
            if (field != null && !field.trim().isEmpty()) {
                validTextFields.add(field);
            }
        }
        if (validTextFields.isEmpty()) {
            throw new IllegalArgumentException("Full-text search fields cannot be empty after filtering null/empty values");
        }
        textFields = validTextFields.toArray(new String[0]);
        
        // If output fields are not specified, use full-text search fields by default
        if (outputFields == null || outputFields.length == 0) {
            outputFields = textFields;
        }

        // Filter out null and empty strings from outputFields
        java.util.ArrayList<String> validFields = new java.util.ArrayList<>();
        java.util.ArrayList<DataType> validDataTypes = new java.util.ArrayList<>();
        for (int i = 0; i < outputFields.length; i++) {
            String field = outputFields[i];
            if (field != null && !field.trim().isEmpty()) {
                validFields.add(field);
                // If data types are already set, keep the corresponding data type
                if (outputDataTypes != null && i < outputDataTypes.length) {
                    validDataTypes.add(outputDataTypes[i]);
                }
            }
        }
        if (validFields.isEmpty()) {
            throw new IllegalArgumentException("Output fields cannot be empty after filtering null/empty values");
        }
        outputFields = validFields.toArray(new String[0]);

        // If data types are not specified, auto-infer
        if (outputDataTypes == null) {
            outputDataTypes = new DataType[outputFields.length];
            for (int i = 0; i < outputFields.length; i++) {
                outputDataTypes[i] = client.inferDataType(tableName, outputFields[i]);
            }
        } else {
            // Update outputDataTypes to match filtered outputFields
            outputDataTypes = validDataTypes.toArray(new DataType[0]);
        }

        if (outputFields.length != outputDataTypes.length) {
            throw new IllegalArgumentException("Output fields count does not match data types count");
        }

        return client.hybridTextVectorSearch(
            tableName,
            vecColumnName,
            metricType,
            queryVector,
            textFields,
            textQuery,
            filterExpr,
            topk,
            outputFields,
            outputDataTypes,
            rankWindowSize
        );
    }
}

