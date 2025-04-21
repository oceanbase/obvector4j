package com.oceanbase.obvec_jdbc;

/**
 * Full-text search parser type enumeration.
 * 
 * OceanBase supports multiple full-text search parsers for different languages and use cases:
 * <ul>
 *   <li><b>SPACE</b>: Default space parser - splits text by spaces (default, no need to specify)</li>
 *   <li><b>IK</b>: IK Analyzer - Chinese word segmentation parser</li>
 *   <li><b>NGRAM</b>: N-gram parser - splits text into n-character sequences</li>
 *   <li><b>NGRAM2</b>: NGRAM2 parser - improved n-gram parser (supported from V4.3.5 BP2+)</li>
 *   <li><b>BASIC_ENGLISH</b>: Basic English parser - English language parser</li>
 *   <li><b>JIEBA</b>: Jieba parser - Chinese word segmentation using Jieba</li>
 * </ul>
 * 
 * Custom parser names can also be used as strings (e.g., "thai_ftparser")
 * 
 * @see <a href="https://www.oceanbase.com/docs/common-oceanbase-database-standalone-1000000003577789">OceanBase Full-Text Search Documentation</a>
 */
public enum FtsParser {
    /**
     * Space parser - default parser, splits text by spaces
     * Use null or don't specify parser_type to use default Space parser
     */
    SPACE(""),
    
    /**
     * IK Analyzer - Chinese word segmentation parser
     * Best for: Chinese text search
     */
    IK("ik"),
    
    /**
     * N-gram parser - splits text into n-character sequences
     * Best for: Asian languages, prefix/suffix matching
     */
    NGRAM("ngram"),
    
    /**
     * NGRAM2 parser - improved n-gram parser
     * Supported from OceanBase V4.3.5 BP2+
     * Best for: Asian languages with better performance
     */
    NGRAM2("ngram2"),
    
    /**
     * Basic English parser - English language parser
     * Best for: English text search
     */
    BASIC_ENGLISH("beng"),
    
    /**
     * Jieba parser - Chinese word segmentation using Jieba
     * Best for: Chinese text search with Jieba segmentation
     */
    JIEBA("jieba");
    
    private final String parserName;
    
    FtsParser(String parserName) {
        this.parserName = parserName;
    }
    
    /**
     * Get the parser name string used in SQL
     * @return parser name, empty string for SPACE (default)
     */
    public String getParserName() {
        return parserName;
    }
    
    /**
     * Parse string to FtsParser (case-insensitive)
     * @param parserStr parser string (e.g., "ik", "ngram", "beng", "jieba")
     * @return FtsParser, SPACE if null or empty
     * @throws IllegalArgumentException if parser is not recognized
     */
    public static FtsParser fromString(String parserStr) {
        if (parserStr == null || parserStr.trim().isEmpty()) {
            return SPACE; // default
        }
        String lower = parserStr.toLowerCase().trim();
        for (FtsParser parser : FtsParser.values()) {
            if (parser.parserName.equals(lower)) {
                return parser;
            }
        }
        // If not found in enum, it might be a custom parser name
        // Return null to indicate custom parser (caller should use string directly)
        throw new IllegalArgumentException("Unrecognized FtsParser: " + parserStr + 
            ". Supported parsers: ik, ngram, ngram2, beng, jieba. " +
            "For custom parsers, use string directly instead of enum.");
    }
    
    /**
     * Check if this is the default Space parser
     * @return true if SPACE
     */
    public boolean isDefault() {
        return this == SPACE;
    }
}

