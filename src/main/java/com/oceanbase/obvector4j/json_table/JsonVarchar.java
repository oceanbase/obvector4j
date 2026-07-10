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

public class JsonVarchar extends JsonData {
    private String val = null;

    public JsonVarchar(int length, String val) {
        JsonVarchar.valid(length, val);
        this.val = val;
    }

    public static void valid(int length, String val) throws IllegalArgumentException {
        if (val == null) {
            return;
        }
        if (val.length() > length) {
            throw new IllegalArgumentException("value length is longer than " + length);
        }
    }

    @Override
    public String toJson() {
        if (this.val == null) {
            return "null";
        }
        String cp = new String(val);
        return "'" + cp + "'";
    }
}
