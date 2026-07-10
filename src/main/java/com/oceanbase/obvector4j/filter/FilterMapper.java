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

package com.oceanbase.obvector4j.filter;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.Collection;

/**
 * Maps Filter objects to OceanBase JSON query format.
 * Converts type-safe Filter objects into the JSON structure required by OceanBase's hybrid search.
 */
public class FilterMapper {

    /**
     * Converts a Filter to a flat condition list for HYBRID_SEARCH knn.filter / query.bool.filter.
     *
     * @param filter The filter to convert
     * @return JSONArray of query conditions, or null if filter is null
     */
    public static JSONArray toFilterConditionList(Filter filter) {
        if (filter == null) {
            return null;
        }
        JSONArray mustArray = new JSONArray();
        processFilter(filter, mustArray);
        return mustArray.isEmpty() ? null : mustArray;
    }

    /**
     * Processes a filter and adds its conditions to the mustArray.
     * Handles both comparison filters and logical filters.
     */
    private static void processFilter(Filter filter, JSONArray mustArray) {
        Filter.Type type = filter.getType();

        switch (type) {
            case EQUAL:
                addTermCondition(mustArray, filter.getKey(), filter.getValue());
                break;
            case NOT_EQUAL:
                addNotEqualCondition(mustArray, filter.getKey(), filter.getValue());
                break;
            case GREATER_THAN:
                addRangeCondition(mustArray, filter.getKey(), "gt", filter.getValue());
                break;
            case GREATER_THAN_OR_EQUAL:
                addRangeCondition(mustArray, filter.getKey(), "gte", filter.getValue());
                break;
            case LESS_THAN:
                addRangeCondition(mustArray, filter.getKey(), "lt", filter.getValue());
                break;
            case LESS_THAN_OR_EQUAL:
                addRangeCondition(mustArray, filter.getKey(), "lte", filter.getValue());
                break;
            case IN:
                addInCondition(mustArray, filter.getKey(), filter.getValues());
                break;
            case NOT_IN:
                addNotInCondition(mustArray, filter.getKey(), filter.getValues());
                break;
            case CONTAINS:
                // ContainsString is not directly supported in OceanBase JSON format
                // We'll convert it to a term query with wildcard matching
                addTermCondition(mustArray, filter.getKey(), "*" + filter.getValue() + "*");
                break;
            case AND:
                // For AND, process both left and right filters
                processFilter(filter.getLeft(), mustArray);
                processFilter(filter.getRight(), mustArray);
                break;
            case OR:
                // For OR, we need to create a should array
                JSONArray shouldArray = new JSONArray();
                processFilterForOr(filter.getLeft(), shouldArray);
                processFilterForOr(filter.getRight(), shouldArray);

                JSONObject boolObj = new JSONObject();
                boolObj.put("should", shouldArray);
                JSONObject boolWrapper = new JSONObject();
                boolWrapper.put("bool", boolObj);
                mustArray.add(boolWrapper);
                break;
            case NOT:
                // For NOT, we need to create a must_not array
                JSONArray mustNotArray = new JSONArray();
                processFilterForNot(filter.getExpression(), mustNotArray);

                JSONObject boolObj2 = new JSONObject();
                boolObj2.put("must_not", mustNotArray);
                JSONObject boolWrapper2 = new JSONObject();
                boolWrapper2.put("bool", boolObj2);
                mustArray.add(boolWrapper2);
                break;
            default:
                throw new UnsupportedOperationException(
                        "Unsupported filter type: " + type);
        }
    }

    /**
     * Processes a filter for OR conditions (should array).
     */
    private static void processFilterForOr(Filter filter, JSONArray shouldArray) {
        Filter.Type type = filter.getType();

        switch (type) {
            case EQUAL:
                JSONObject termObj = createTermObject(filter.getKey(), filter.getValue());
                shouldArray.add(termObj);
                break;
            case GREATER_THAN:
                JSONObject rangeObj1 = createRangeObject(filter.getKey(), "gt", filter.getValue());
                shouldArray.add(rangeObj1);
                break;
            case GREATER_THAN_OR_EQUAL:
                JSONObject rangeObj2 = createRangeObject(filter.getKey(), "gte", filter.getValue());
                shouldArray.add(rangeObj2);
                break;
            case LESS_THAN:
                JSONObject rangeObj3 = createRangeObject(filter.getKey(), "lt", filter.getValue());
                shouldArray.add(rangeObj3);
                break;
            case LESS_THAN_OR_EQUAL:
                JSONObject rangeObj4 = createRangeObject(filter.getKey(), "lte", filter.getValue());
                shouldArray.add(rangeObj4);
                break;
            case IN:
                // For IN in OR context, add each value as a separate should condition
                for (Object value : filter.getValues()) {
                    JSONObject termObj2 = createTermObject(filter.getKey(), value);
                    shouldArray.add(termObj2);
                }
                break;
            case OR:
                // Nested OR: process recursively
                processFilterForOr(filter.getLeft(), shouldArray);
                processFilterForOr(filter.getRight(), shouldArray);
                break;
            case AND:
                // AND in OR context: create a bool must for the AND part
                JSONArray andMustArray = new JSONArray();
                processFilter(filter, andMustArray);
                JSONObject boolObj = new JSONObject();
                boolObj.put("must", andMustArray);
                JSONObject boolWrapper = new JSONObject();
                boolWrapper.put("bool", boolObj);
                shouldArray.add(boolWrapper);
                break;
            default:
                // For other filter types, convert to must array and wrap
                JSONArray nestedMustArray = new JSONArray();
                processFilter(filter, nestedMustArray);
                if (!nestedMustArray.isEmpty()) {
                    JSONObject boolObj2 = new JSONObject();
                    boolObj2.put("must", nestedMustArray);
                    JSONObject boolWrapper2 = new JSONObject();
                    boolWrapper2.put("bool", boolObj2);
                    shouldArray.add(boolWrapper2);
                }
                break;
        }
    }

    /**
     * Processes a filter for NOT conditions (must_not array).
     */
    private static void processFilterForNot(Filter filter, JSONArray mustNotArray) {
        Filter.Type type = filter.getType();

        switch (type) {
            case EQUAL:
                JSONObject termObj = createTermObject(filter.getKey(), filter.getValue());
                mustNotArray.add(termObj);
                break;
            case GREATER_THAN:
                JSONObject rangeObj1 = createRangeObject(filter.getKey(), "gt", filter.getValue());
                mustNotArray.add(rangeObj1);
                break;
            case GREATER_THAN_OR_EQUAL:
                JSONObject rangeObj2 = createRangeObject(filter.getKey(), "gte", filter.getValue());
                mustNotArray.add(rangeObj2);
                break;
            case LESS_THAN:
                JSONObject rangeObj3 = createRangeObject(filter.getKey(), "lt", filter.getValue());
                mustNotArray.add(rangeObj3);
                break;
            case LESS_THAN_OR_EQUAL:
                JSONObject rangeObj4 = createRangeObject(filter.getKey(), "lte", filter.getValue());
                mustNotArray.add(rangeObj4);
                break;
            case IN:
                // For NOT IN, add each value as a separate must_not condition
                for (Object value : filter.getValues()) {
                    JSONObject termObj2 = createTermObject(filter.getKey(), value);
                    mustNotArray.add(termObj2);
                }
                break;
            default:
                // For complex filters, wrap in a bool must
                JSONArray nestedMustArray = new JSONArray();
                processFilter(filter, nestedMustArray);
                if (!nestedMustArray.isEmpty()) {
                    JSONObject boolObj = new JSONObject();
                    boolObj.put("must", nestedMustArray);
                    JSONObject boolWrapper = new JSONObject();
                    boolWrapper.put("bool", boolObj);
                    mustNotArray.add(boolWrapper);
                }
                break;
        }
    }

    /**
     * Adds a term (equality) condition to the mustArray.
     */
    private static void addTermCondition(JSONArray mustArray, String key, Object value) {
        JSONObject termObj = createTermObject(key, value);
        mustArray.add(termObj);
    }

    /**
     * Creates a term query object.
     */
    private static JSONObject createTermObject(String key, Object value) {
        // Validate key to avoid "Unknown column ''" errors
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Filter key cannot be null or empty");
        }
        String trimmedKey = key.trim();

        JSONObject termObj = new JSONObject();
        JSONObject fieldObj = new JSONObject();
        Object convertedValue = convertValue(value);
        fieldObj.put(trimmedKey, convertedValue);
        termObj.put("term", fieldObj);
        return termObj;
    }

    /**
     * Adds a NOT EQUAL condition.
     * Since OceanBase doesn't directly support !=, we'll use must_not with term.
     */
    private static void addNotEqualCondition(JSONArray mustArray, String key, Object value) {
        JSONObject boolObj = new JSONObject();
        JSONArray mustNotArray = new JSONArray();
        JSONObject termObj = createTermObject(key, value);
        mustNotArray.add(termObj);
        boolObj.put("must_not", mustNotArray);
        JSONObject boolWrapper = new JSONObject();
        boolWrapper.put("bool", boolObj);
        mustArray.add(boolWrapper);
    }

    /**
     * Adds a range condition to the mustArray.
     * Merges multiple range conditions for the same field.
     */
    private static void addRangeCondition(JSONArray mustArray, String key, String operator, Object value) {
        // Validate key to avoid "Unknown column ''" errors
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Filter key cannot be null or empty");
        }
        String trimmedKey = key.trim();

        // Check if there's already a range condition for this field
        JSONObject existingRange = findExistingRange(mustArray, trimmedKey);

        if (existingRange != null) {
            // Merge with existing range
            JSONObject rangeObj = (JSONObject) existingRange.get("range");
            JSONObject fieldObj = (JSONObject) rangeObj.get(trimmedKey);
            Object convertedValue = convertValue(value);
            fieldObj.put(operator, convertedValue);
        } else {
            // Create new range condition
            JSONObject rangeObj = createRangeObject(trimmedKey, operator, value);
            mustArray.add(rangeObj);
        }
    }

    /**
     * Creates a range query object.
     */
    private static JSONObject createRangeObject(String key, String operator, Object value) {
        // Validate key to avoid "Unknown column ''" errors
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Filter key cannot be null or empty");
        }
        String trimmedKey = key.trim();

        JSONObject rangeObj = new JSONObject();
        JSONObject rangeFieldObj = new JSONObject();
        JSONObject fieldRangeObj = new JSONObject();
        Object convertedValue = convertValue(value);
        fieldRangeObj.put(operator, convertedValue);
        rangeFieldObj.put(trimmedKey, fieldRangeObj);
        rangeObj.put("range", rangeFieldObj);
        return rangeObj;
    }

    /**
     * Finds an existing range condition for the given key in the mustArray.
     */
    private static JSONObject findExistingRange(JSONArray mustArray, String key) {
        if (key == null || key.trim().isEmpty()) {
            return null;
        }
        String trimmedKey = key.trim();
        for (Object item : mustArray) {
            if (item instanceof JSONObject) {
                JSONObject jsonObj = (JSONObject) item;
                Object rangeObj = jsonObj.get("range");
                if (rangeObj instanceof JSONObject) {
                    JSONObject range = (JSONObject) rangeObj;
                    if (range.containsKey(trimmedKey)) {
                        return jsonObj;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Adds an IN condition.
     * Converts IN to multiple OR conditions (should array).
     */
    private static void addInCondition(JSONArray mustArray, String key, Collection<?> values) {
        if (values == null || values.isEmpty()) {
            // Empty IN means no matches
            return;
        }

        // For IN, we create a should array with multiple term queries
        JSONArray shouldArray = new JSONArray();
        for (Object value : values) {
            JSONObject termObj = createTermObject(key, value);
            shouldArray.add(termObj);
        }

        JSONObject boolObj = new JSONObject();
        boolObj.put("should", shouldArray);
        JSONObject boolWrapper = new JSONObject();
        boolWrapper.put("bool", boolObj);
        mustArray.add(boolWrapper);
    }

    /**
     * Adds a NOT IN condition.
     */
    private static void addNotInCondition(JSONArray mustArray, String key, Collection<?> values) {
        if (values == null || values.isEmpty()) {
            // Empty NOT IN means all matches
            return;
        }

        // For NOT IN, we create a must_not array with multiple term queries
        JSONArray mustNotArray = new JSONArray();
        for (Object value : values) {
            JSONObject termObj = createTermObject(key, value);
            mustNotArray.add(termObj);
        }

        JSONObject boolObj = new JSONObject();
        boolObj.put("must_not", mustNotArray);
        JSONObject boolWrapper = new JSONObject();
        boolWrapper.put("bool", boolObj);
        mustArray.add(boolWrapper);
    }

    /**
     * Converts a value to the appropriate type for JSON.
     */
    private static Object convertValue(Object value) {
        if (value instanceof Number) {
            // Keep numbers as-is
            return value;
        } else if (value instanceof String) {
            // Try to parse as number if possible
            String strValue = (String) value;
            try {
                if (strValue.contains(".")) {
                    return Double.parseDouble(strValue);
                } else {
                    return Long.parseLong(strValue);
                }
            } catch (NumberFormatException e) {
                // Return as string if not a number
                return strValue;
            }
        } else {
            return value;
        }
    }
}
