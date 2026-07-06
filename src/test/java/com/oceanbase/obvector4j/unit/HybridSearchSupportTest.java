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
