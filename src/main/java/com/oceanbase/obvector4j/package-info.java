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
 * OceanBase Vector Store JDBC SDK — public entry points.
 *
 * <p>Start with {@link com.oceanbase.obvector4j.ObVecClient}. Subpackages:
 * <ul>
 *   <li>{@code schema} — table schema, index params, {@link com.oceanbase.obvector4j.schema.DataType}</li>
 *   <li>{@code model} — {@link com.oceanbase.obvector4j.model.Sqlizable} value types for rows</li>
 *   <li>{@code hybrid} — hybrid search engine and fluent builders</li>
 *   <li>{@code filter} — type-safe Filter API</li>
 *   <li>{@code version} — OceanBase version detection</li>
 *   <li>{@code json_table} — JSON virtual table (via {@link com.oceanbase.obvector4j.ObVecJsonClient})</li>
 * </ul>
 */
package com.oceanbase.obvector4j;
