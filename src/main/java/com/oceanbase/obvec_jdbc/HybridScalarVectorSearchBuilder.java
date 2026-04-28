package com.oceanbase.obvec_jdbc;

import com.oceanbase.obvec_jdbc.filter.Filter;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Builder for scalar + vector hybrid search
 * Used to simplify the call to hybridScalarVectorSearch method
 */
public class HybridScalarVectorSearchBuilder {
    private final ObVecClient client;
    private String tableName;
    private String vecColumnName = "embedding";
    private String metricType = "l2";
    private float[] queryVector;
    private Object filterExpr; // Can be String (legacy) or Filter object
    private int topk = 10;
    private String[] outputFields;
    private DataType[] outputDataTypes;

    public HybridScalarVectorSearchBuilder(ObVecClient client) {
        this.client = client;
    }

    /**
     * Set table name
     */
    public HybridScalarVectorSearchBuilder table(String tableName) {
        this.tableName = tableName;
        return this;
    }

    /**
     * Set vector column name (default: "embedding")
     */
    public HybridScalarVectorSearchBuilder vectorColumn(String vecColumnName) {
        this.vecColumnName = vecColumnName;
        return this;
    }

    /**
     * Set distance metric type (default: "l2")
     * Supported values: "l2", "ip", "cosine"
     */
    public HybridScalarVectorSearchBuilder metric(String metricType) {
        this.metricType = metricType;
        return this;
    }

    /**
     * Set query vector
     */
    public HybridScalarVectorSearchBuilder queryVector(float[] queryVector) {
        this.queryVector = queryVector;
        return this;
    }

    /**
     * Set filter condition (SQL WHERE expression - legacy method)
     * Example: "category_id = 1 AND price >= 50 AND price <= 250"
     */
    public HybridScalarVectorSearchBuilder filter(String filterExpr) {
        this.filterExpr = filterExpr;
        return this;
    }

    /**
     * Set filter condition using Filter object (type-safe method)
     * Example: FilterBuilder.key("category_id").isEqualTo(1)
     */
    public HybridScalarVectorSearchBuilder filter(Filter filter) {
        this.filterExpr = filter;
        return this;
    }

    /**
     * Set number of results to return (default: 10)
     */
    public HybridScalarVectorSearchBuilder topk(int topk) {
        this.topk = topk;
        return this;
    }

    /**
     * Set output field (single field, auto-infer type)
     */
    public HybridScalarVectorSearchBuilder outputField(String fieldName) {
        this.outputFields = new String[]{fieldName};
        this.outputDataTypes = null; // Will be auto-inferred in search()
        return this;
    }

    /**
     * Set output field (single field, specify type)
     */
    public HybridScalarVectorSearchBuilder outputField(String fieldName, DataType dataType) {
        this.outputFields = new String[]{fieldName};
        this.outputDataTypes = new DataType[]{dataType};
        return this;
    }

    /**
     * Set output fields (multiple fields, auto-infer type)
     */
    public HybridScalarVectorSearchBuilder outputFields(String... fieldNames) {
        this.outputFields = fieldNames;
        this.outputDataTypes = null; // Will be auto-inferred in search()
        return this;
    }

    /**
     * Set output fields (multiple fields, specify types)
     */
    public HybridScalarVectorSearchBuilder outputFields(String[] fieldNames, DataType[] dataTypes) {
        this.outputFields = fieldNames;
        this.outputDataTypes = dataTypes;
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
        if (outputFields == null || outputFields.length == 0) {
            throw new IllegalArgumentException("Output fields must be set");
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

        return client.hybridScalarVectorSearch(
            tableName,
            vecColumnName,
            metricType,
            queryVector,
            filterExpr,
            topk,
            outputFields,
            outputDataTypes
        );
    }
}

