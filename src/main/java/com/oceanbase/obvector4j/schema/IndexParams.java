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

public class IndexParams extends Visitable {
    ArrayList<IndexParam> index_params;

    public IndexParams() {
        index_params = new ArrayList<>();
    }

    public void addIndex(IndexParam index_param) {
        index_params.add(index_param);
    }

    @Override
    public String visit() {
        String[] vidx_defs = new String[index_params.size()];
        for (int i = 0; i < index_params.size(); i++) {
            IndexParam index_param = index_params.get(i);
            vidx_defs[i] = String.format("VECTOR INDEX %s(%s) %s",
                            index_param.getVidxName(),
                            index_param.getFieldName(),
                            index_params.get(i).visit());
        }
        return String.join(", ", vidx_defs);
    }
}
