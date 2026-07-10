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
import com.oceanbase.obvector4j.ObVecClient;
import com.oceanbase.obvector4j.model.Sqlizable;
import com.oceanbase.obvector4j.filter.Filter;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Builder for scalar + vector hybrid search.
 */
public class HybridScalarVectorSearchBuilder extends AbstractHybridSearchBuilder<HybridScalarVectorSearchBuilder> {

    private String vecColumnName = "embedding";
    private String metricType = "l2";
    private float[] queryVector;
    private Object filterExpr;
    private int topk = 10;

    public HybridScalarVectorSearchBuilder(ObVecClient client) {
        super(client);
    }

    public HybridScalarVectorSearchBuilder table(String tableName) {
        this.tableName = tableName;
        return this;
    }

    public HybridScalarVectorSearchBuilder vectorColumn(String vecColumnName) {
        this.vecColumnName = vecColumnName;
        return this;
    }

    public HybridScalarVectorSearchBuilder metric(String metricType) {
        this.metricType = metricType;
        return this;
    }

    public HybridScalarVectorSearchBuilder queryVector(float[] queryVector) {
        this.queryVector = queryVector;
        return this;
    }

    public HybridScalarVectorSearchBuilder filter(String filterExpr) {
        this.filterExpr = filterExpr;
        return this;
    }

    public HybridScalarVectorSearchBuilder filter(Filter filter) {
        this.filterExpr = filter;
        return this;
    }

    public HybridScalarVectorSearchBuilder topk(int topk) {
        this.topk = topk;
        return this;
    }

    public HybridScalarVectorSearchBuilder outputField(String fieldName) {
        this.outputFields = new String[]{fieldName};
        this.outputDataTypes = null;
        return this;
    }

    public HybridScalarVectorSearchBuilder outputField(String fieldName, DataType dataType) {
        this.outputFields = new String[]{fieldName};
        this.outputDataTypes = new DataType[]{dataType};
        return this;
    }

    public HybridScalarVectorSearchBuilder outputFields(String... fieldNames) {
        this.outputFields = fieldNames;
        this.outputDataTypes = null;
        return this;
    }

    public HybridScalarVectorSearchBuilder outputFields(String[] fieldNames, DataType[] dataTypes) {
        this.outputFields = fieldNames;
        this.outputDataTypes = dataTypes;
        return this;
    }

    public ArrayList<HashMap<String, Sqlizable>> search() throws Exception {
        if (tableName == null || tableName.isEmpty()) {
            throw new IllegalArgumentException("Table name cannot be empty");
        }
        if (queryVector == null || queryVector.length == 0) {
            throw new IllegalArgumentException("Query vector cannot be empty");
        }
        if (outputFields == null || outputFields.length == 0) {
            throw new IllegalArgumentException("Output fields must be set");
        }

        resolveOutputFields(null);

        return client.hybridScalarVectorSearch(
                tableName, vecColumnName, metricType, queryVector, filterExpr,
                topk, outputFields, outputDataTypes);
    }
}
