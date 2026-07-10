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

// MySQL get
public class JsonBool extends JsonData {
    private Boolean val = null;

    public JsonBool(boolean val) {
        JsonBool.validate(val);
        this.val = Boolean.valueOf(val);
    }

    public static void validate(boolean val) throws IllegalArgumentException {
        // do nothing
    }

    @Override
    public String toJson() {
        if (this.val == null) {
            return "null";
        }
        if (this.val) {
            return "1";
        } else {
            return "0";
        }
    }
}
