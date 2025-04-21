package com.oceanbase.obvec_jdbc;

/**
 * Vector index parameters configuration.
 * Supports multiple vector index types: HNSW, HNSW_SQ, IVFFLAT, IVFSQ, IVFPQ, DAAT
 * 
 * Default index type is HNSW. Use constructor with VecIndexType to specify other types.
 */
public class IndexParam extends Visitable {
    // Default values for HNSW
    public static final int HNSW_DEFAULT_M = 16;
    public static final int HNSW_DEFAULT_EF_CONSTRUCTION = 200;
    public static final int HNSW_DEFAULT_EF_SEARCH = 64;
    public static final String OCEANBASE_DEFAULT_ALGO_LIB_VSAG = "vsag";
    public static final String OCEANBASE_DEFAULT_ALGO_LIB_OB = "OB";
    
    private String vidx_name;
    private String vector_field_name;
    private VecIndexType index_type = VecIndexType.HNSW; // Default to HNSW
    
    // HNSW parameters
    private int m = HNSW_DEFAULT_M;
    private int ef_construction = HNSW_DEFAULT_EF_CONSTRUCTION;
    private int ef_search = HNSW_DEFAULT_EF_SEARCH;
    
    // IVF parameters
    private Integer nlist;
    private Integer samples_per_nlist;
    private Integer pq_m; // for IVFPQ
    
    // Sparse vector parameters (DAAT)
    private String sparse_index_type;
    
    // Common parameters
    private String lib = OCEANBASE_DEFAULT_ALGO_LIB_VSAG;
    private String metric_type = "l2";

    /**
     * Create IndexParam with default HNSW type
     * @param vidx_name index name
     * @param vector_field_name vector field name
     */
    public IndexParam(String vidx_name, String vector_field_name) {
        this(vidx_name, vector_field_name, VecIndexType.HNSW);
    }
    
    /**
     * Create IndexParam with specified index type
     * @param vidx_name index name
     * @param vector_field_name vector field name
     * @param index_type vector index type
     */
    public IndexParam(String vidx_name, String vector_field_name, VecIndexType index_type) {
        this.vidx_name = vidx_name;
        this.vector_field_name = vector_field_name;
        this.index_type = index_type;
        
        // Set defaults based on index type
        if (index_type.isHNSWSerial()) {
            this.lib = OCEANBASE_DEFAULT_ALGO_LIB_VSAG;
            this.metric_type = "l2";
        } else if (index_type.isIVFSerial()) {
            this.lib = OCEANBASE_DEFAULT_ALGO_LIB_OB;
            this.metric_type = "l2";
        } else if (index_type.isSparseVector()) {
            this.lib = OCEANBASE_DEFAULT_ALGO_LIB_OB;
            this.metric_type = "inner_product"; // Sparse vector must use inner_product
        }
    }
    
    /**
     * Create IndexParam from string type
     * @param vidx_name index name
     * @param vector_field_name vector field name
     * @param indexTypeStr index type string (e.g., "hnsw", "ivf_flat", "daat")
     */
    public IndexParam(String vidx_name, String vector_field_name, String indexTypeStr) {
        this(vidx_name, vector_field_name, VecIndexType.fromString(indexTypeStr));
    }

    // ========== HNSW Parameters ==========
    
    /**
     * Set M parameter for HNSW (number of bi-directional links)
     * @param m M value (typically 4-64)
     * @return this for method chaining
     */
    public IndexParam M(int m) {
        if (!index_type.isHNSWSerial()) {
            throw new UnsupportedOperationException("M parameter is only supported for HNSW index types");
        }
        this.m = m;
        return this;
    }

    /**
     * Set ef_construction parameter for HNSW
     * @param ef_construction ef_construction value (typically 100-500)
     * @return this for method chaining
     */
    public IndexParam EfConstruction(int ef_construction) {
        if (!index_type.isHNSWSerial()) {
            throw new UnsupportedOperationException("ef_construction parameter is only supported for HNSW index types");
        }
        this.ef_construction = ef_construction;
        return this;
    }

    /**
     * Set ef_search parameter for HNSW
     * @param ef_search ef_search value (typically 16-512)
     * @return this for method chaining
     */
    public IndexParam EfSearch(int ef_search) {
        if (!index_type.isHNSWSerial()) {
            throw new UnsupportedOperationException("ef_search parameter is only supported for HNSW index types");
        }
        this.ef_search = ef_search;
        return this;
    }
    
    // ========== IVF Parameters ==========
    
    /**
     * Set nlist parameter for IVF indexes
     * @param nlist number of clusters (typically 100-10000)
     * @return this for method chaining
     */
    public IndexParam Nlist(int nlist) {
        if (!index_type.isIVFSerial()) {
            throw new UnsupportedOperationException("nlist parameter is only supported for IVF index types");
        }
        this.nlist = nlist;
        return this;
    }
    
    /**
     * Set samples_per_nlist parameter for IVF indexes
     * @param samples_per_nlist samples per cluster
     * @return this for method chaining
     */
    public IndexParam SamplesPerNlist(int samples_per_nlist) {
        if (!index_type.isIVFSerial()) {
            throw new UnsupportedOperationException("samples_per_nlist parameter is only supported for IVF index types");
        }
        this.samples_per_nlist = samples_per_nlist;
        return this;
    }
    
    /**
     * Set m parameter for IVFPQ (product quantization)
     * @param pq_m number of sub-vectors for PQ
     * @return this for method chaining
     */
    public IndexParam PQM(int pq_m) {
        if (index_type != VecIndexType.IVFPQ) {
            throw new UnsupportedOperationException("pq_m parameter is only supported for IVFPQ index type");
        }
        this.pq_m = pq_m;
        return this;
    }
    
    // ========== Sparse Vector Parameters ==========
    
    /**
     * Set sparse index type for DAAT
     * @param sparse_index_type sparse index type
     * @return this for method chaining
     */
    public IndexParam SparseIndexType(String sparse_index_type) {
        if (!index_type.isSparseVector()) {
            throw new UnsupportedOperationException("sparse_index_type parameter is only supported for DAAT index type");
        }
        this.sparse_index_type = sparse_index_type;
        return this;
    }
    
    // ========== Common Parameters ==========

    /**
     * Set algorithm library (vsag or OB)
     * @param lib library name
     * @return this for method chaining
     */
    public IndexParam Lib(String lib) {
        this.lib = lib;
        return this;
    }

    /**
     * Set metric type (distance function)
     * @param metric_type metric type: "l2", "inner_product", "ip", or "cosine"
     * @return this for method chaining
     */
    public IndexParam MetricType(String metric_type) {
        if (index_type.isSparseVector() && !metric_type.equalsIgnoreCase("inner_product") && !metric_type.equalsIgnoreCase("ip")) {
            throw new IllegalArgumentException("Metric type must be 'inner_product' for sparse vector index (DAAT)");
        }
        if (!checkMetricType(metric_type)) {
            throw new UnsupportedOperationException("Metric Type is not supported: " + metric_type);
        }
        this.metric_type = metric_type;
        return this;
    }
    
    // ========== Getters ==========

    public String getVidxName() {
        return this.vidx_name;
    }

    public String getFieldName() {
        return this.vector_field_name;
    }
    
    public VecIndexType getIndexType() {
        return this.index_type;
    }
    
    public Integer getM() {
        return m;
    }
    
    public Integer getEfConstruction() {
        return ef_construction;
    }
    
    public Integer getEfSearch() {
        return ef_search;
    }
    
    public Integer getNlist() {
        return nlist;
    }
    
    public Integer getSamplesPerNlist() {
        return samples_per_nlist;
    }
    
    public Integer getPQM() {
        return pq_m;
    }
    
    public String getSparseIndexType() {
        return sparse_index_type;
    }
    
    public String getLib() {
        return lib;
    }
    
    public String getMetricType() {
        return metric_type;
    }

    // ========== Validation ==========
    
    private boolean checkMetricType(String metric_type) {
        if (metric_type == null) {
            return false;
        }
        String metric_type_lower = metric_type.toLowerCase();
        return metric_type_lower.equals("l2") || 
               metric_type_lower.equals("inner_product") || 
               metric_type_lower.equals("ip") ||
               metric_type_lower.equals("cosine");
    }
    
    /**
     * Validate parameters before building SQL
     */
    private void validate() {
        if (index_type.isIVFSerial()) {
            if (index_type == VecIndexType.IVFPQ && pq_m == null) {
                throw new IllegalStateException("pq_m parameter must be configured for IVFPQ index type");
            }
        }
    }

    // ========== SQL Generation ==========
    
    @Override
    public String visit() {
        validate();
        
        StringBuilder params = new StringBuilder();
        
        // Handle lib
        if (lib != null) {
            params.append("lib=").append(lib);
        }
        
        // Handle distance/metric_type
        if (metric_type != null) {
            if (params.length() > 0) params.append(",");
            String distance = metric_type.equalsIgnoreCase("ip") ? "inner_product" : metric_type;
            params.append("distance=").append(distance);
        }
        
        // Handle HNSW parameters
        if (index_type.isHNSWSerial()) {
            if (params.length() > 0) params.append(",");
            params.append("m=").append(m);
            if (params.length() > 0) params.append(",");
            params.append("ef_construction=").append(ef_construction);
            if (params.length() > 0) params.append(",");
            params.append("ef_search=").append(ef_search);
        }
        
        // Handle IVF parameters
        if (index_type.isIVFSerial()) {
            if (nlist != null) {
                if (params.length() > 0) params.append(",");
                params.append("nlist=").append(nlist);
            }
            if (samples_per_nlist != null) {
                if (params.length() > 0) params.append(",");
                params.append("samples_per_nlist=").append(samples_per_nlist);
            }
            if (pq_m != null) {
                if (params.length() > 0) params.append(",");
                params.append("m=").append(pq_m);
            }
        }
        
        // Handle sparse vector parameters
        if (index_type.isSparseVector()) {
            if (sparse_index_type != null) {
                if (params.length() > 0) params.append(",");
                params.append("type=").append(sparse_index_type);
            }
        }
        
        // Add type parameter (except for sparse vector which uses type for sparse_index_type)
        if (!index_type.isSparseVector()) {
            if (params.length() > 0) params.append(",");
            params.append("type=").append(index_type.getAlgorithmName());
        }
        
        return "WITH(" + params.toString() + ")";
    }
}
