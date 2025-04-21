package com.oceanbase.obvec_jdbc.filter;

import java.util.Arrays;
import java.util.Collection;

/**
 * Builder class for creating Filter objects in a fluent API style.
 * Provides convenient methods for building common filter expressions.
 */
public class FilterBuilder {

    /**
     * Creates a filter builder for a specific key.
     *
     * @param key The field name to filter on
     * @return A KeyFilterBuilder for chaining filter operations
     */
    public static KeyFilterBuilder key(String key) {
        return new KeyFilterBuilder(key);
    }

    /**
     * Creates an AND filter combining two filters.
     *
     * @param left  The left filter
     * @param right The right filter
     * @return An AND filter
     */
    public static Filter and(Filter left, Filter right) {
        return Filter.and(left, right);
    }

    /**
     * Creates an OR filter combining two filters.
     *
     * @param left  The left filter
     * @param right The right filter
     * @return An OR filter
     */
    public static Filter or(Filter left, Filter right) {
        return Filter.or(left, right);
    }

    /**
     * Creates a NOT filter negating a filter.
     *
     * @param filter The filter to negate
     * @return A NOT filter
     */
    public static Filter not(Filter filter) {
        return Filter.not(filter);
    }

    /**
     * Builder for creating filters on a specific key.
     */
    public static class KeyFilterBuilder {
        private final String key;

        private KeyFilterBuilder(String key) {
            if (key == null || key.isEmpty()) {
                throw new IllegalArgumentException("Key cannot be null or empty");
            }
            this.key = key;
        }

        /**
         * Creates an equality filter: key = value
         */
        public Filter isEqualTo(Object value) {
            return Filter.equal(key, value);
        }

        /**
         * Creates an inequality filter: key != value
         */
        public Filter isNotEqualTo(Object value) {
            return Filter.notEqual(key, value);
        }

        /**
         * Creates a greater than filter: key > value
         */
        public Filter isGreaterThan(Object value) {
            return Filter.greaterThan(key, value);
        }

        /**
         * Creates a greater than or equal filter: key >= value
         */
        public Filter isGreaterThanOrEqualTo(Object value) {
            return Filter.greaterThanOrEqual(key, value);
        }

        /**
         * Creates a less than filter: key < value
         */
        public Filter isLessThan(Object value) {
            return Filter.lessThan(key, value);
        }

        /**
         * Creates a less than or equal filter: key <= value
         */
        public Filter isLessThanOrEqualTo(Object value) {
            return Filter.lessThanOrEqual(key, value);
        }

        /**
         * Creates an IN filter: key IN (value1, value2, ...)
         */
        @SafeVarargs
        public final Filter isIn(Object... values) {
            return Filter.in(key, Arrays.asList(values));
        }

        /**
         * Creates an IN filter: key IN (values)
         */
        public Filter isIn(Collection<?> values) {
            return Filter.in(key, values);
        }

        /**
         * Creates a NOT IN filter: key NOT IN (value1, value2, ...)
         */
        @SafeVarargs
        public final Filter isNotIn(Object... values) {
            return Filter.notIn(key, Arrays.asList(values));
        }

        /**
         * Creates a NOT IN filter: key NOT IN (values)
         */
        public Filter isNotIn(Collection<?> values) {
            return Filter.notIn(key, values);
        }

        /**
         * Creates a contains string filter: key LIKE '%value%'
         */
        public Filter contains(String value) {
            return Filter.contains(key, value);
        }
    }
}
