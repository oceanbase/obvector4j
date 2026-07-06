package com.oceanbase.obvector4j.filter;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Converts {@link Filter} objects and legacy filter expressions to SQL WHERE clauses.
 */
public final class FilterSqlConverter {

    private FilterSqlConverter() {
    }

    public static String toWhereClause(Object filterExpr) {
        if (filterExpr == null) {
            return null;
        }
        if (filterExpr instanceof String) {
            String filterStr = ((String) filterExpr).trim();
            return filterStr.isEmpty() ? null : filterStr;
        }
        if (filterExpr instanceof Filter) {
            return fromFilter((Filter) filterExpr);
        }
        throw new IllegalArgumentException("Filter expression must be either String or Filter object");
    }

    private static String fromFilter(Filter filter) {
        if (filter == null) {
            return null;
        }

        Filter.Type type = filter.getType();
        String key = filter.getKey();
        Object value = filter.getValue();

        switch (type) {
            case EQUAL:
                return formatCondition(key, "=", value);
            case NOT_EQUAL:
                return formatCondition(key, "!=", value);
            case GREATER_THAN:
                return formatCondition(key, ">", value);
            case GREATER_THAN_OR_EQUAL:
                return formatCondition(key, ">=", value);
            case LESS_THAN:
                return formatCondition(key, "<", value);
            case LESS_THAN_OR_EQUAL:
                return formatCondition(key, "<=", value);
            case IN:
                return formatInCondition(key, filter.getValues());
            case NOT_IN:
                return formatNotInCondition(key, filter.getValues());
            case CONTAINS:
                return formatCondition(key, "LIKE", "%" + value + "%");
            case AND:
                return combineLogical("AND", fromFilter(filter.getLeft()), fromFilter(filter.getRight()));
            case OR:
                return combineLogical("OR", fromFilter(filter.getLeft()), fromFilter(filter.getRight()));
            case NOT:
                String expr = fromFilter(filter.getExpression());
                return expr != null ? "NOT (" + expr + ")" : null;
            default:
                throw new UnsupportedOperationException("Filter type " + type + " is not supported");
        }
    }

    private static String combineLogical(String operator, String left, String right) {
        if (left != null && right != null) {
            return "(" + left + ") " + operator + " (" + right + ")";
        }
        return left != null ? left : right;
    }

    private static String formatCondition(String key, String operator, Object value) {
        if (value instanceof String) {
            return "`" + key + "` " + operator + " '" + escapeSqlString((String) value) + "'";
        }
        return "`" + key + "` " + operator + " " + value;
    }

    private static String formatInCondition(String key, Collection<?> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        ArrayList<String> valueStrs = new ArrayList<>();
        for (Object val : values) {
            if (val instanceof String) {
                valueStrs.add("'" + escapeSqlString((String) val) + "'");
            } else {
                valueStrs.add(String.valueOf(val));
            }
        }
        return "`" + key + "` IN (" + String.join(", ", valueStrs) + ")";
    }

    private static String formatNotInCondition(String key, Collection<?> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        ArrayList<String> valueStrs = new ArrayList<>();
        for (Object val : values) {
            if (val instanceof String) {
                valueStrs.add("'" + escapeSqlString((String) val) + "'");
            } else {
                valueStrs.add(String.valueOf(val));
            }
        }
        return "`" + key + "` NOT IN (" + String.join(", ", valueStrs) + ")";
    }

    private static String escapeSqlString(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("'", "''").replace("\\", "\\\\");
    }
}
