package com.oceanbase.obvector4j.support;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.junit.Assume;

/**
 * Base for remote OceanBase integration tests.
 *
 * <p>Enable with {@code OCEANBASE_REMOTE_IT=1} and connection env vars:
 * {@code OCEANBASE_URI} or {@code OCEANBASE_HOST}/{@code OCEANBASE_PORT}/
 * {@code OCEANBASE_USER}/{@code OCEANBASE_PASSWORD}/{@code OCEANBASE_DATABASE}.
 */
public abstract class RemoteOceanBaseTestBase extends junit.framework.TestCase {

    protected static String jdbcUri;
    protected static String user;
    protected static String password;
    protected static boolean remoteEnabled;
    protected static String skipReason;

    static {
        remoteEnabled = "1".equals(System.getenv("OCEANBASE_REMOTE_IT"))
                || "true".equalsIgnoreCase(System.getenv("OCEANBASE_REMOTE_IT"));
        if (!remoteEnabled) {
            skipReason = "Set OCEANBASE_REMOTE_IT=1 to run remote OceanBase integration tests";
        } else {
            try {
                Class.forName("com.oceanbase.jdbc.Driver");
                jdbcUri = System.getenv("OCEANBASE_URI");
                if (jdbcUri == null || jdbcUri.trim().isEmpty()) {
                    String host = OceanBaseTestSupport.requiredEnv("OCEANBASE_HOST");
                    String port = OceanBaseTestSupport.requiredEnv("OCEANBASE_PORT");
                    user = OceanBaseTestSupport.requiredEnv("OCEANBASE_USER");
                    password = OceanBaseTestSupport.requiredEnv("OCEANBASE_PASSWORD");
                    String database = OceanBaseTestSupport.env("OCEANBASE_DATABASE", "test");
                    jdbcUri = OceanBaseTestSupport.buildJdbcUri(host, port, database);
                } else {
                    user = OceanBaseTestSupport.requiredEnv("OCEANBASE_USER");
                    password = OceanBaseTestSupport.requiredEnv("OCEANBASE_PASSWORD");
                }
                try (Connection conn = OceanBaseTestSupport.openConnection(jdbcUri, user, password);
                     java.sql.Statement stmt = conn.createStatement()) {
                    stmt.execute("SELECT 1");
                }
                skipReason = null;
            } catch (Throwable e) {
                remoteEnabled = false;
                skipReason = "Remote OceanBase unavailable: " + e.getMessage();
            }
        }
    }

    protected static void assumeRemoteAvailable() {
        Assume.assumeTrue(skipReason, remoteEnabled);
    }

    public RemoteOceanBaseTestBase(String name) {
        super(name);
    }

    protected com.oceanbase.obvector4j.ObVecClient newClient() throws Throwable {
        return new com.oceanbase.obvector4j.ObVecClient(jdbcUri, user, password);
    }

    protected Connection newConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUri, user, password);
    }

    protected void execSql(String sql) throws SQLException {
        OceanBaseTestSupport.execSql(jdbcUri, user, password, sql);
    }

    protected void dropTableIfExists(String tableName) throws SQLException {
        OceanBaseTestSupport.dropTableIfExists(jdbcUri, user, password, tableName);
    }

    protected void waitForIndex() throws InterruptedException {
        OceanBaseTestSupport.waitForIndex();
    }

    protected static float[] vector(float... values) {
        return OceanBaseTestSupport.vector(values);
    }
}
