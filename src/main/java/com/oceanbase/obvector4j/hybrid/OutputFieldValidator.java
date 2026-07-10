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

package com.oceanbase.obvector4j.hybrid;

import com.oceanbase.obvector4j.schema.DataType;

/**
 * Validates and sanitizes output field names for hybrid search queries.
 */
public final class OutputFieldValidator {

    public static final class ValidatedFields {
        private final String[] fields;
        private final DataType[] dataTypes;

        public ValidatedFields(String[] fields, DataType[] dataTypes) {
            this.fields = fields;
            this.dataTypes = dataTypes;
        }

        public String[] getFields() {
            return fields;
        }

        public DataType[] getDataTypes() {
            return dataTypes;
        }
    }

    private OutputFieldValidator() {
    }

    public static String[] sanitizeStringFields(String[] fields, String fieldLabel) {
        if (fields == null || fields.length == 0) {
            return fields;
        }
        java.util.ArrayList<String> validFields = new java.util.ArrayList<>();
        for (String field : fields) {
            if (field != null) {
                String trimmedField = field.trim();
                if (!trimmedField.isEmpty()) {
                    validFields.add(trimmedField);
                }
            }
        }
        if (validFields.isEmpty()) {
            throw new IllegalArgumentException(fieldLabel + " cannot be empty after filtering null/empty values");
        }
        return validFields.toArray(new String[0]);
    }

    public static ValidatedFields validateOutputFields(String[] outputFields, DataType[] outputDataTypes) {
        if (outputFields == null || outputFields.length == 0) {
            return new ValidatedFields(outputFields, outputDataTypes);
        }
        java.util.ArrayList<String> validOutputFields = new java.util.ArrayList<>();
        java.util.ArrayList<DataType> validOutputDataTypes = new java.util.ArrayList<>();
        for (int i = 0; i < outputFields.length; i++) {
            String field = outputFields[i];
            if (field != null) {
                String trimmedField = field.trim();
                if (!trimmedField.isEmpty()) {
                    validOutputFields.add(trimmedField);
                    if (outputDataTypes != null && i < outputDataTypes.length) {
                        validOutputDataTypes.add(outputDataTypes[i]);
                    }
                }
            }
        }
        if (validOutputFields.isEmpty()) {
            throw new IllegalArgumentException("Output fields cannot be empty after filtering null/empty values");
        }
        return new ValidatedFields(
                validOutputFields.toArray(new String[0]),
                validOutputDataTypes.isEmpty() ? null : validOutputDataTypes.toArray(new DataType[0]));
    }
}
