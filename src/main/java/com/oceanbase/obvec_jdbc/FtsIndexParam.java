package com.oceanbase.obvec_jdbc;

import java.util.Arrays;
import java.util.List;

/**
 * Full-text search index parameter.
 * 
 * Used to configure full-text search indexes with optional parser types.
 * 
 * Example usage:
 * <pre>{@code
 * // Default Space parser
 * FtsIndexParam ftsParam = new FtsIndexParam("ft_title", Arrays.asList("title"));
 * 
 * // IK parser for Chinese
 * FtsIndexParam ftsParam = new FtsIndexParam("ft_title", Arrays.asList("title"), FtsParser.IK);
 * 
 * // Custom parser
 * FtsIndexParam ftsParam = new FtsIndexParam("ft_title", Arrays.asList("title"), "thai_ftparser");
 * }</pre>
 */
public class FtsIndexParam {
    private String indexName;
    private List<String> fieldNames;
    private FtsParser parserType;
    private String customParserName; // For custom parsers not in enum
    
    /**
     * Create FTS index parameter with default Space parser
     * @param indexName index name
     * @param fieldNames list of field names to create full-text index on
     */
    public FtsIndexParam(String indexName, List<String> fieldNames) {
        this(indexName, fieldNames, (FtsParser) null);
    }
    
    /**
     * Create FTS index parameter with FtsParser enum
     * @param indexName index name
     * @param fieldNames list of field names to create full-text index on
     * @param parserType parser type enum (null for default Space parser)
     */
    public FtsIndexParam(String indexName, List<String> fieldNames, FtsParser parserType) {
        if (indexName == null || indexName.trim().isEmpty()) {
            throw new IllegalArgumentException("Index name cannot be null or empty");
        }
        if (fieldNames == null || fieldNames.isEmpty()) {
            throw new IllegalArgumentException("Field names cannot be null or empty");
        }
        this.indexName = indexName;
        this.fieldNames = fieldNames;
        this.parserType = parserType != null ? parserType : FtsParser.SPACE;
        this.customParserName = null;
    }
    
    /**
     * Create FTS index parameter with custom parser name (string)
     * @param indexName index name
     * @param fieldNames list of field names to create full-text index on
     * @param customParserName custom parser name (e.g., "thai_ftparser")
     */
    public FtsIndexParam(String indexName, List<String> fieldNames, String customParserName) {
        if (indexName == null || indexName.trim().isEmpty()) {
            throw new IllegalArgumentException("Index name cannot be null or empty");
        }
        if (fieldNames == null || fieldNames.isEmpty()) {
            throw new IllegalArgumentException("Field names cannot be null or empty");
        }
        if (customParserName == null || customParserName.trim().isEmpty()) {
            throw new IllegalArgumentException("Custom parser name cannot be null or empty");
        }
        this.indexName = indexName;
        this.fieldNames = fieldNames;
        this.parserType = null;
        this.customParserName = customParserName.toLowerCase().trim();
    }
    
    /**
     * Convenience constructor for single field
     * @param indexName index name
     * @param fieldName single field name
     * @param parserType parser type enum (null for default Space parser)
     */
    public FtsIndexParam(String indexName, String fieldName, FtsParser parserType) {
        this(indexName, Arrays.asList(fieldName), parserType);
    }
    
    /**
     * Convenience constructor for single field with custom parser
     * @param indexName index name
     * @param fieldName single field name
     * @param customParserName custom parser name
     */
    public FtsIndexParam(String indexName, String fieldName, String customParserName) {
        this(indexName, Arrays.asList(fieldName), customParserName);
    }
    
    public String getIndexName() {
        return indexName;
    }
    
    public List<String> getFieldNames() {
        return fieldNames;
    }
    
    public FtsParser getParserType() {
        return parserType;
    }
    
    public String getCustomParserName() {
        return customParserName;
    }
    
    /**
     * Get parser name string for SQL
     * @return parser name string, or null if default Space parser
     */
    public String getParserNameString() {
        if (customParserName != null) {
            return customParserName;
        }
        if (parserType != null && !parserType.isDefault()) {
            return parserType.getParserName();
        }
        return null; // Default Space parser, no need to specify in SQL
    }
    
    /**
     * Check if using default Space parser
     * @return true if default parser
     */
    public boolean isDefaultParser() {
        return (parserType == null || parserType.isDefault()) && customParserName == null;
    }
}

