package com.oceanbase.obvector4j.hybrid.core.dsl;

import org.json.simple.JSONObject;

/**
 * A HYBRID_SEARCH DSL query expression ({@code match}, {@code bool}, {@code term}, …).
 * Serializes to the JSON object placed under the top-level {@code query} key.
 */
public interface HybridDslExpr {

    JSONObject toJson();
}
