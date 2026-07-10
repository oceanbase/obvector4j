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

package com.oceanbase.obvector4j.model;

import com.oceanbase.obvector4j.schema.DataType;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SqlizableFactory {
    public static Sqlizable build(DataType dataType, ResultSet db_res, String col_name) throws SQLException {
        switch (dataType) {
            case BOOL:
            case INT8:
            case INT16:
            case INT32:
            case INT64:
            {
                return new SqlInteger(db_res.getInt(col_name));
            }
            case FLOAT:
            {
                return new SqlFloat(db_res.getFloat(col_name));
            }
            case DOUBLE:
            {
                return new SqlDouble(db_res.getDouble(col_name));
            }
            case STRING:
            case VARCHAR:
            case JSON: // TODO: use org.json
            {
                return new SqlText(db_res.getString(col_name));
            }
            case FLOAT_VECTOR:
            {
                return new SqlText(db_res.getString(col_name));
            }
            default: return null;
        }
    }
}
