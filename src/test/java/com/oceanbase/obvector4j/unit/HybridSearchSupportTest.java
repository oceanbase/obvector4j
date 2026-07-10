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

import com.oceanbase.obvector4j.hybrid.core.HybridSearchSupport;
import com.oceanbase.obvector4j.version.OceanBaseVersion;
import junit.framework.TestCase;

public class HybridSearchSupportTest extends TestCase {

    public void testRequireAcceptsMinVersionAndAbove() {
        HybridSearchSupport.require(OceanBaseVersion.parse("4.6.0.0"));
        HybridSearchSupport.require(OceanBaseVersion.parse("5.0.1"));
    }

    public void testRequireRejectsBelowMinVersion() {
        try {
            HybridSearchSupport.require(OceanBaseVersion.parse("4.5.2.0"));
            fail("expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            assertTrue(e.getMessage().contains("4.6.0"));
            assertTrue(e.getMessage().contains("4.5.2"));
        }
    }

    public void testRequireRejectsNullVersion() {
        try {
            HybridSearchSupport.require(null);
            fail("expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            assertTrue(e.getMessage().contains("4.6.0"));
        }
    }
}
