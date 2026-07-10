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

package com.oceanbase.obvector4j.hybrid.core;

import com.oceanbase.obvector4j.version.OceanBaseVersion;

/**
 * Version gate for OceanBase 4.6.0+ {@code HYBRID_SEARCH} DSL APIs.
 */
public final class HybridSearchSupport {

    private HybridSearchSupport() {
    }

    /**
     * @throws UnsupportedOperationException if {@code version} is below 4.6.0
     */
    public static void require(OceanBaseVersion version) {
        if (version == null || !version.isAtLeast(OceanBaseVersion.HYBRID_SEARCH_SQL_MIN)) {
            String current = version != null ? version.toString() : "unknown";
            throw new UnsupportedOperationException(
                    "HYBRID_SEARCH DSL requires OceanBase 4.6.0 or later; "
                            + "versions below 4.6.0 are not supported. Current version: "
                            + current);
        }
    }
}
