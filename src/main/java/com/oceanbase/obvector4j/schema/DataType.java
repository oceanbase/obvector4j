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

public enum DataType {
    BOOL,
    INT8,
    INT16,
    INT32,
    INT64,

    FLOAT,
    DOUBLE,

    STRING,
    VARCHAR,
    JSON,

    FLOAT_VECTOR;

    public String Convert2MySQL() {
        switch(this) {
            case BOOL: return "TINYINT";
            case INT8: return "TINYINT";
            case INT16: return "SMALLINT";
            case INT32: return "INT";
            case INT64: return "BIGINT";
            case FLOAT: return "FLOAT";
            case DOUBLE: return "DOUBLE";
            case STRING: return "LONGTEXT";
            case VARCHAR: return "VARCHAR";
            case JSON: return "JSON";
            case FLOAT_VECTOR: return "VECTOR";
            default: return null;
        }
    }
}
