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

package com.oceanbase.obvector4j.schema;

import java.util.ArrayList;

public class ObCollectionSchema extends Visitable {
    private ArrayList<ObFieldSchema> fields;
    private IndexParams index_params = null;
    private boolean heapOrganized = false;

    public ObCollectionSchema() {
        fields = new ArrayList<>();
        index_params = null;
    }

    public void addField(ObFieldSchema field) {
        fields.add(field);
    }

    public void setIndexParams(IndexParams index_params) {
        this.index_params = index_params;
    }

    /**
     * Set whether the table should be created as ORGANIZATION HEAP with
     * COLUMN GROUP(all columns). Required for hybrid search (scalar-vector,
     * text-vector, HYBRID_SEARCH DSL).
     *
     * @param heapOrganized true to append ORGANIZATION HEAP WITH COLUMN GROUP(all columns)
     * @return this schema for fluent chaining
     */
    public ObCollectionSchema HeapOrganized(boolean heapOrganized) {
        this.heapOrganized = heapOrganized;
        return this;
    }

    public boolean getHeapOrganized() {
        return heapOrganized;
    }

    @Override
    public String visit() {
        String[] column_defs = new String[fields.size()];
        ArrayList<String> primary_keys = new ArrayList<>();
        for (int i = 0; i < fields.size(); i++) {
            ObFieldSchema field = fields.get(i);
            if (field.getIsPrimary()) {
                primary_keys.add(field.getName());
            }
            column_defs[i] = field.visit();
        }
        ArrayList<String> table_schema_strs = new ArrayList<>();
        String joined_column_def = String.join(", ", column_defs);
        table_schema_strs.add(joined_column_def);
        if (!primary_keys.isEmpty()) {
            String primary_key_def = String.format("PRIMARY KEY(%s)", String.join(", ", primary_keys));
            table_schema_strs.add(primary_key_def);
        }
        if (this.index_params != null) {
            table_schema_strs.add(index_params.visit());
        }
        String result = String.join(", ", table_schema_strs);
        return result;
    }

    /**
     * Returns the table-level suffix (e.g. ORGANIZATION HEAP WITH COLUMN GROUP(all columns))
     * to be appended after the closing parenthesis of CREATE TABLE.
     */
    public String visitTableOptions() {
        if (heapOrganized) {
            return " ORGANIZATION HEAP WITH COLUMN GROUP(all columns)";
        }
        return "";
    }

}
