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

package com.oceanbase.obvector4j.json_table;

import java.sql.Timestamp;

public class JsonTimestamp extends JsonData {
    private Timestamp val = null;

    public JsonTimestamp(Timestamp val) {
        JsonTimestamp.validate(val);
        this.val = val;
    }

    public static void validate(Timestamp val) throws IllegalArgumentException {
        // do nothing
    }

    @Override
    public String toJson() {
        if (this.val == null) {
            return "null";
        }
        return "'" + val.toString() + "'";
    }

}
