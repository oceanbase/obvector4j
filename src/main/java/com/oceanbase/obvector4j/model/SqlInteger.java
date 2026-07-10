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

public class SqlInteger extends Sqlizable {
    private int int_val;

    public SqlInteger(int val) {
        int_val = val;
    }

    @Override
    public void toDB(int param_idx, PreparedStatement ps) throws SQLException {
        ps.setInt(param_idx, int_val);
    }

    @Override
    public String toString() {
        return String.valueOf(int_val);
    }
}
