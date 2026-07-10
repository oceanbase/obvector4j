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

/**
 * Hybrid search: legacy compatibility path (&lt; 4.6.0) and shared builder base classes.
 *
 * <p>OceanBase 4.6.0+ {@code HYBRID_SEARCH} DSL syntax lives in
 * {@link com.oceanbase.obvector4j.hybrid.core} — use {@link com.oceanbase.obvector4j.ObVecClient#hybridSearch()}.
 *
 * <p>Version-adaptive builders: {@link com.oceanbase.obvector4j.hybrid.HybridTextVectorSearchBuilder},
 * {@link com.oceanbase.obvector4j.hybrid.HybridScalarVectorSearchBuilder}.
 */
package com.oceanbase.obvector4j.hybrid;
