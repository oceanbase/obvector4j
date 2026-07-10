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

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;

public class SqlVector extends Sqlizable {
    private float[] vector_val;

    public SqlVector(float[] vector) {
        vector_val = vector;
    }

    @Override
    public void toDB(int param_idx, PreparedStatement ps) throws SQLException {
        ArrayList<String> float_str = new ArrayList<>();
        for (int i = 0; i < vector_val.length; i++) {
            float_str.add(String.valueOf(vector_val[i]));
        }
        String vector_str = "[" + String.join(", ", float_str) + "]";
        ps.setString(param_idx, vector_str);
    }

    @Override
    public String toString() {
        ArrayList<String> float_str = new ArrayList<>();
        for (int i = 0; i < vector_val.length; i++) {
            float_str.add(String.valueOf(vector_val[i]));
        }
        return "[" + String.join(", ", float_str) + "]";
    }
}
