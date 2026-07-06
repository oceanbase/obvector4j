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
