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
 * Builder for full-text + vector hybrid search.
 */
public class HybridTextVectorSearchBuilder extends AbstractHybridSearchBuilder<HybridTextVectorSearchBuilder> {

    private String vecColumnName = "embedding";
    private String metricType = "cosine";
    private float[] queryVector;
    private String[] textFields;
    private String textQuery;
    private Object filterExpr;
    private int topk = 10;
    private Integer rankWindowSize = null;

    public HybridTextVectorSearchBuilder(ObVecClient client) {
        super(client);
    }

    public HybridTextVectorSearchBuilder table(String tableName) {
        this.tableName = tableName;
        return this;
    }

    public HybridTextVectorSearchBuilder vectorColumn(String vecColumnName) {
        this.vecColumnName = vecColumnName;
        return this;
    }

    public HybridTextVectorSearchBuilder metric(String metricType) {
        this.metricType = metricType;
        return this;
    }

    public HybridTextVectorSearchBuilder queryVector(float[] queryVector) {
        this.queryVector = queryVector;
        return this;
    }

    public HybridTextVectorSearchBuilder textField(String textField) {
        this.textFields = new String[]{textField};
        return this;
    }

    public HybridTextVectorSearchBuilder textFields(String... textFields) {
        this.textFields = textFields;
        return this;
    }

    public HybridTextVectorSearchBuilder textQuery(String textQuery) {
        this.textQuery = textQuery;
        return this;
    }

    public HybridTextVectorSearchBuilder topk(int topk) {
        this.topk = topk;
        return this;
    }

    public HybridTextVectorSearchBuilder outputField(String fieldName) {
        this.outputFields = new String[]{fieldName};
        this.outputDataTypes = null;
        return this;
    }

    public HybridTextVectorSearchBuilder outputField(String fieldName, DataType dataType) {
        this.outputFields = new String[]{fieldName};
        this.outputDataTypes = new DataType[]{dataType};
        return this;
    }

    public HybridTextVectorSearchBuilder outputFields(String... fieldNames) {
        this.outputFields = fieldNames;
        this.outputDataTypes = null;
        return this;
    }

    public HybridTextVectorSearchBuilder outputFields(String[] fieldNames, DataType[] dataTypes) {
        this.outputFields = fieldNames;
        this.outputDataTypes = dataTypes;
        return this;
    }

    public HybridTextVectorSearchBuilder outputAll() {
        this.outputFields = null;
        this.outputDataTypes = null;
        return this;
    }

    public HybridTextVectorSearchBuilder filter(String filterExpr) {
        this.filterExpr = filterExpr;
        return this;
    }

    public HybridTextVectorSearchBuilder filter(Filter filter) {
        this.filterExpr = filter;
        return this;
    }

    public HybridTextVectorSearchBuilder rankWindowSize(Integer rankWindowSize) {
        this.rankWindowSize = rankWindowSize;
        return this;
    }

    public ArrayList<HashMap<String, Sqlizable>> search() throws Exception {
        if (tableName == null || tableName.isEmpty()) {
            throw new IllegalArgumentException("Table name cannot be empty");
        }
        if (queryVector == null || queryVector.length == 0) {
            throw new IllegalArgumentException("Query vector cannot be empty");
        }
        if (textFields == null || textFields.length == 0) {
            throw new IllegalArgumentException("Full-text search fields cannot be empty");
        }
        if (textQuery == null || textQuery.isEmpty()) {
            throw new IllegalArgumentException("Full-text search query text cannot be empty");
        }

        textFields = OutputFieldValidator.sanitizeStringFields(textFields, "Full-text search fields");
        resolveOutputFields(textFields);

        return client.hybridTextVectorSearch(
                tableName, vecColumnName, metricType, queryVector, textFields, textQuery,
                filterExpr, topk, outputFields, outputDataTypes, rankWindowSize);
    }
}
