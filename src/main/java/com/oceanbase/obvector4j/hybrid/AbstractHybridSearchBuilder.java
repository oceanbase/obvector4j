package com.oceanbase.obvector4j.hybrid;

import com.oceanbase.obvector4j.schema.DataType;
import com.oceanbase.obvector4j.ObVecClient;
import java.util.ArrayList;

/**
 * Shared builder utilities for hybrid search fluent APIs.
 */
public abstract class AbstractHybridSearchBuilder<T extends AbstractHybridSearchBuilder<T>> {

    protected final ObVecClient client;
    protected String tableName;
    protected String[] outputFields;
    protected DataType[] outputDataTypes;

    protected AbstractHybridSearchBuilder(ObVecClient client) {
        this.client = client;
    }

    protected DataType[] resolveOutputFields(String[] defaultFields) throws Throwable {
        if (outputFields == null || outputFields.length == 0) {
            outputFields = defaultFields;
        }

        ArrayList<String> validFields = new ArrayList<>();
        ArrayList<DataType> validDataTypes = new ArrayList<>();
        for (int i = 0; i < outputFields.length; i++) {
            String field = outputFields[i];
            if (field != null && !field.trim().isEmpty()) {
                validFields.add(field);
                if (outputDataTypes != null && i < outputDataTypes.length) {
                    validDataTypes.add(outputDataTypes[i]);
                }
            }
        }
        if (validFields.isEmpty()) {
            throw new IllegalArgumentException("Output fields cannot be empty after filtering null/empty values");
        }
        outputFields = validFields.toArray(new String[0]);

        if (outputDataTypes == null) {
            outputDataTypes = new DataType[outputFields.length];
            for (int i = 0; i < outputFields.length; i++) {
                outputDataTypes[i] = client.inferColumnDataType(tableName, outputFields[i]);
            }
        } else {
            outputDataTypes = validDataTypes.toArray(new DataType[0]);
        }

        if (outputFields.length != outputDataTypes.length) {
            throw new IllegalArgumentException("Output fields count does not match data types count");
        }
        return outputDataTypes;
    }
}
