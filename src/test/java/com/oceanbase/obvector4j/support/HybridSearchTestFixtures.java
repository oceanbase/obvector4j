package com.oceanbase.obvector4j.support;

import java.sql.SQLException;

/**
 * Shared DDL helpers for hybrid search integration tests.
 */
public final class HybridSearchTestFixtures {

    public static final String TABLE_PREFIX = "OB_REMOTE_IT_";
    public static final String ROW_STORE_HEAP =
            " ORGANIZATION HEAP WITH COLUMN GROUP(all columns)";

    private HybridSearchTestFixtures() {
    }

    public static void createVectorIndex(
            SqlExecutor executor, String table, String indexName, String column) throws SQLException {
        executor.execute("CREATE VECTOR INDEX `" + indexName + "` ON `" + table + "`(`" + column + "`) "
                + "WITH (distance=l2, type=hnsw_sq, lib=vsag)");
    }

    public static void createFulltextIndex(
            SqlExecutor executor, String table, String indexName, String column) throws SQLException {
        executor.execute("CREATE FULLTEXT INDEX `" + indexName + "` ON `" + table + "`(`" + column + "`)");
    }

    public interface SqlExecutor {
        void execute(String sql) throws SQLException;
    }
}
