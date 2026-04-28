package com.oceanbase.obvec_jdbc.filter;

import java.util.Collection;

/**
 * Unified Filter class that supports all filter operations.
 * Uses enum to distinguish between different operation types.
 */
public class Filter {
    
    /**
     * Filter operation type
     */
    public enum Type {
        // Comparison operations
        EQUAL,              // =
        NOT_EQUAL,          // !=
        GREATER_THAN,       // >
        GREATER_THAN_OR_EQUAL,  // >=
        LESS_THAN,          // <
        LESS_THAN_OR_EQUAL, // <=
        IN,                 // IN
        NOT_IN,             // NOT IN
        CONTAINS,           // LIKE '%value%'
        
        // Logical operations
        AND,                // AND
        OR,                 // OR
        NOT                 // NOT
    }
    
    private final Type type;
    private final String key;
    private final Object value;
    private final Collection<?> values;  // For IN/NOT_IN operations
    private final Filter left;            // For logical operations
    private final Filter right;           // For logical operations
    private final Filter expression;      // For NOT operation
    
    // Private constructors for different filter types
    
    /**
     * Constructor for comparison filters with single value
     */
    private Filter(Type type, String key, Object value) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Key cannot be null or empty");
        }
        if (value == null && type != Type.NOT_EQUAL) {
            throw new IllegalArgumentException("Comparison value cannot be null");
        }
        this.type = type;
        this.key = key;
        this.value = value;
        this.values = null;
        this.left = null;
        this.right = null;
        this.expression = null;
    }
    
    /**
     * Constructor for IN/NOT_IN filters
     */
    private Filter(Type type, String key, Collection<?> values) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Key cannot be null or empty");
        }
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("Comparison values cannot be null or empty");
        }
        this.type = type;
        this.key = key;
        this.value = null;
        this.values = values;
        this.left = null;
        this.right = null;
        this.expression = null;
    }
    
    /**
     * Constructor for AND/OR filters
     */
    private Filter(Type type, Filter left, Filter right) {
        if (left == null || right == null) {
            throw new IllegalArgumentException("Left and right filters cannot be null");
        }
        this.type = type;
        this.key = null;
        this.value = null;
        this.values = null;
        this.left = left;
        this.right = right;
        this.expression = null;
    }
    
    /**
     * Constructor for NOT filter
     */
    private Filter(Type type, Filter expression) {
        if (expression == null) {
            throw new IllegalArgumentException("Expression filter cannot be null");
        }
        this.type = type;
        this.key = null;
        this.value = null;
        this.values = null;
        this.left = null;
        this.right = null;
        this.expression = expression;
    }
    
    // Factory methods for comparison filters
    
    public static Filter equal(String key, Object value) {
        return new Filter(Type.EQUAL, key, value);
    }
    
    public static Filter notEqual(String key, Object value) {
        return new Filter(Type.NOT_EQUAL, key, value);
    }
    
    public static Filter greaterThan(String key, Object value) {
        return new Filter(Type.GREATER_THAN, key, value);
    }
    
    public static Filter greaterThanOrEqual(String key, Object value) {
        return new Filter(Type.GREATER_THAN_OR_EQUAL, key, value);
    }
    
    public static Filter lessThan(String key, Object value) {
        return new Filter(Type.LESS_THAN, key, value);
    }
    
    public static Filter lessThanOrEqual(String key, Object value) {
        return new Filter(Type.LESS_THAN_OR_EQUAL, key, value);
    }
    
    public static Filter in(String key, Collection<?> values) {
        return new Filter(Type.IN, key, values);
    }
    
    public static Filter notIn(String key, Collection<?> values) {
        return new Filter(Type.NOT_IN, key, values);
    }
    
    public static Filter contains(String key, String value) {
        return new Filter(Type.CONTAINS, key, value);
    }
    
    // Factory methods for logical filters
    
    public static Filter and(Filter left, Filter right) {
        return new Filter(Type.AND, left, right);
    }
    
    public static Filter or(Filter left, Filter right) {
        return new Filter(Type.OR, left, right);
    }
    
    public static Filter not(Filter expression) {
        return new Filter(Type.NOT, expression);
    }
    
    // Getters
    
    public Type getType() {
        return type;
    }
    
    public String getKey() {
        return key;
    }
    
    public Object getValue() {
        return value;
    }
    
    public Collection<?> getValues() {
        return values;
    }
    
    public Filter getLeft() {
        return left;
    }
    
    public Filter getRight() {
        return right;
    }
    
    public Filter getExpression() {
        return expression;
    }
}
