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

package com.oceanbase.obvector4j.unit;

import com.oceanbase.obvector4j.hybrid.core.HybridSearchDsl;
import com.oceanbase.obvector4j.hybrid.core.HybridSearchDslBuilder;
import com.oceanbase.obvector4j.version.OceanBaseVersion;
import com.oceanbase.obvector4j.filter.Filter;
import com.oceanbase.obvector4j.filter.FilterBuilder;
import junit.framework.TestCase;

public class HybridSearchDslTest extends TestCase {

    public void testOceanBaseVersionParseFromJdbcString() {
        OceanBaseVersion v = OceanBaseVersion.parse("5.7.25-OceanBase-v4.3.5.6");
        assertEquals(4, v.getMajor());
        assertEquals(3, v.getMinor());
        assertEquals(5, v.getPatch());
        assertFalse(v.isAtLeast(OceanBaseVersion.HYBRID_SEARCH_SQL_MIN));
    }

    public void testOceanBaseVersionParse() {
        OceanBaseVersion v = OceanBaseVersion.parse("OceanBase 4.6.0.0");
        assertEquals(4, v.getMajor());
        assertEquals(6, v.getMinor());
        assertEquals(0, v.getPatch());
        assertTrue(v.isAtLeast(OceanBaseVersion.HYBRID_SEARCH_SQL_MIN));
    }

    public void testOceanBaseVersionCompare() {
        OceanBaseVersion vSupported = OceanBaseVersion.parse("4.6.0.0");
        OceanBaseVersion vUnsupported = OceanBaseVersion.parse("4.5.2.0");
        assertTrue(vSupported.isAtLeast(OceanBaseVersion.HYBRID_SEARCH_SQL_MIN));
        assertFalse(vUnsupported.isAtLeast(OceanBaseVersion.HYBRID_SEARCH_SQL_MIN));
        assertTrue(vSupported.compareTo(vUnsupported) > 0);
    }

    public void testScalarVectorDslWithoutFilter() {
        float[] vector = {1.0f, 2.0f, 3.0f};
        String dsl = HybridSearchDslBuilder.buildScalarVectorDsl("embedding", vector, 5, null);
        assertTrue(dsl.contains("\"knn\""));
        assertTrue(dsl.contains("\"field\":\"embedding\""));
        assertTrue(dsl.contains("\"query_vector\":\"[1.0,2.0,3.0]\""));
        assertTrue(dsl.contains("\"size\":5"));
        assertFalse(dsl.contains("\"filter\""));
    }

    public void testScalarVectorDslWithFilter() {
        Filter filter = FilterBuilder.key("category_id").isEqualTo(1);
        float[] vector = {0.1f, 0.2f};
        String dsl = HybridSearchDslBuilder.buildScalarVectorDsl("vec", vector, 3, filter);
        assertTrue(dsl.contains("\"filter\""));
        assertTrue(dsl.contains("\"term\""));
        assertTrue(dsl.contains("\"category_id\":1"));
    }

    public void testTextVectorDslSingleField() {
        float[] vector = {1.0f, 2.0f, 3.0f};
        String dsl = HybridSearchDslBuilder.buildTextVectorDsl(
                "vector", vector, new String[]{"content"}, "oceanbase mysql", null, 10, null);
        assertTrue(dsl.contains("\"match\""));
        assertTrue(dsl.contains("\"content\":\"oceanbase mysql\""));
        assertTrue(dsl.contains("\"rank\""));
        assertTrue(dsl.contains("\"rrf\""));
    }

    public void testTextVectorDslMultiField() {
        float[] vector = {1.0f, 2.0f};
        String dsl = HybridSearchDslBuilder.buildTextVectorDsl(
                "embedding", vector, new String[]{"title", "content"}, "test query", null, 5, 20);
        assertTrue(dsl.contains("\"multi_match\""));
        assertTrue(dsl.contains("\"title\""));
        assertTrue(dsl.contains("\"content\""));
        assertTrue(dsl.contains("\"rank_window_size\":20"));
    }

    public void testCustomDslFromParts() {
        String dsl = HybridSearchDsl.create()
                .query("{\"match\":{\"content\":\"oceanbase\"}}")
                .knn("{\"field\":\"vector\",\"k\":5,\"query_vector\":\"[1.0,2.0,3.0]\"}")
                .rank("{\"rrf\":{\"rank_constant\":60,\"rank_window_size\":10}}")
                .size(10)
                .toJsonString();
        assertTrue(dsl.contains("\"query\""));
        assertTrue(dsl.contains("\"knn\""));
        assertTrue(dsl.contains("\"rank\""));
        assertTrue(dsl.contains("\"size\":10"));
    }

    public void testCustomDslRawOverride() {
        String raw = "{\"knn\":{\"field\":\"vec\",\"k\":3,\"query_vector\":\"[0,1,2]\"},\"size\":3}";
        assertEquals(raw, HybridSearchDsl.create().rawDsl(raw).toJsonString());
    }

    public void testCustomDslMerge() {
        String dsl = HybridSearchDsl.create()
                .knn("{\"field\":\"embedding\",\"k\":5,\"query_vector\":\"[1,2,3]\"}")
                .merge("{\"size\":5,\"min_score\":0.1}")
                .toJsonString();
        assertTrue(dsl.contains("\"min_score\":0.1"));
        assertTrue(dsl.contains("\"size\":5"));
    }

    public void testCustomDslRequiresQueryOrKnn() {
        try {
            HybridSearchDsl.create().size(10).toJsonString();
            fail("Expected IllegalStateException");
        } catch (IllegalStateException expected) {
            // ok
        }
    }
}
