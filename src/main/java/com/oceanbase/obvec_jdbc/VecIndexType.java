package com.oceanbase.obvec_jdbc;

/**
 * Vector index algorithm type enumeration.
 * 
 * OceanBase supports multiple vector index algorithms for different use cases:
 * <ul>
 *   <li><b>HNSW</b>: Hierarchical Navigable Small World - default algorithm, good balance of speed and accuracy</li>
 *   <li><b>HNSW_SQ</b>: HNSW with Scalar Quantization - more memory efficient than HNSW</li>
 *   <li><b>IVFFLAT</b>: Inverted File Flat - suitable for large datasets</li>
 *   <li><b>IVFSQ</b>: Inverted File Scalar Quantization - IVF with SQ8 quantization</li>
 *   <li><b>IVFPQ</b>: Inverted File Product Quantization - IVF with PQ, most memory efficient</li>
 *   <li><b>DAAT</b>: Document-at-a-time algorithm for sparse vectors</li>
 * </ul>
 * 
 * @see <a href="https://www.oceanbase.com/docs/common-oceanbase-database-standalone-1000000003577789">OceanBase Vector Index Documentation</a>
 */
public enum VecIndexType {
    /**
     * Hierarchical Navigable Small World - default HNSW algorithm
     * Best for: General purpose vector search with good balance of speed and accuracy
     * Parameters: m, ef_construction, ef_search
     */
    HNSW("hnsw"),
    
    /**
     * HNSW with Scalar Quantization
     * Best for: Memory-constrained environments
     * Parameters: m, ef_construction, ef_search
     */
    HNSW_SQ("hnsw_sq"),
    
    /**
     * Inverted File Flat - IVF flat algorithm
     * Best for: Large datasets with many vectors
     * Parameters: nlist, samples_per_nlist
     */
    IVFFLAT("ivf_flat"),
    
    /**
     * Inverted File Scalar Quantization - IVF with SQ8
     * Best for: Large datasets with memory constraints
     * Parameters: nlist, samples_per_nlist
     */
    IVFSQ("ivf_sq8"),
    
    /**
     * Inverted File Product Quantization - IVF with PQ
     * Best for: Very large datasets with strict memory constraints
     * Parameters: nlist, samples_per_nlist, m (for PQ)
     */
    IVFPQ("ivf_pq"),
    
    /**
     * Document-at-a-time algorithm for sparse vectors
     * Best for: Sparse vector search
     * Parameters: sparse_index_type
     */
    DAAT("daat");
    
    
    private final String algorithmName;
    
    VecIndexType(String algorithmName) {
        this.algorithmName = algorithmName;
    }
    
    /**
     * Get the algorithm name string used in SQL
     * @return algorithm name
     */
    public String getAlgorithmName() {
        return algorithmName;
    }
    
    /**
     * Parse string to VecIndexType (case-insensitive)
     * @param typeStr index type string (e.g., "hnsw", "HNSW", "ivf_flat")
     * @return VecIndexType
     * @throws IllegalArgumentException if type is not supported
     */
    public static VecIndexType fromString(String typeStr) {
        if (typeStr == null || typeStr.trim().isEmpty()) {
            return HNSW; // default
        }
        String lower = typeStr.toLowerCase().trim();
        for (VecIndexType type : VecIndexType.values()) {
            if (type.algorithmName.equals(lower)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unsupported vector index type: " + typeStr + 
            ". Supported types: hnsw, hnsw_sq, ivf_flat, ivf_sq8, ivf_pq, daat");
    }
    
    /**
     * Check if this is an HNSW serial index type (HNSW or HNSW_SQ)
     * @return true if HNSW or HNSW_SQ
     */
    public boolean isHNSWSerial() {
        return this == HNSW || this == HNSW_SQ;
    }
    
    /**
     * Check if this is an IVF serial index type
     * @return true if IVF type
     */
    public boolean isIVFSerial() {
        return this == IVFFLAT || this == IVFSQ || this == IVFPQ;
    }
    
    /**
     * Check if this is a product quantization index type
     * @return true if IVFPQ
     */
    public boolean isProductQuantization() {
        return this == IVFPQ;
    }
    
    /**
     * Check if this is a sparse vector index type
     * @return true if DAAT
     */
    public boolean isSparseVector() {
        return this == DAAT;
    }

}

