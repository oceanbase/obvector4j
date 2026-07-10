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

package com.oceanbase.obvector4j.support;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Shared JDBC helpers for OceanBase integration tests.
 */
public final class OceanBaseTestSupport {

    private OceanBaseTestSupport() {
    }

    public static Connection openConnection(String jdbcUri, String user, String password)
            throws SQLException {
        return DriverManager.getConnection(jdbcUri, user, password);
    }

    public static void execSql(String jdbcUri, String user, String password, String sql)
            throws SQLException {
        try (Connection conn = openConnection(jdbcUri, user, password);
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    public static void dropTableIfExists(String jdbcUri, String user, String password, String tableName)
            throws SQLException {
        execSql(jdbcUri, user, password, "DROP TABLE IF EXISTS `" + tableName + "`");
    }

    public static void waitForIndex() throws InterruptedException {
        Thread.sleep(5000);
    }

    public static float[] vector(float... values) {
        return values;
    }

    public static String env(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value == null || value.trim().isEmpty()) ? defaultValue : value.trim();
    }

    public static String requiredEnv(String key) {
        String value = System.getenv(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException("Missing required env var: " + key);
        }
        return value.trim();
    }

    public static String buildJdbcUri(String host, String port, String database) {
        return String.format("jdbc:oceanbase://%s:%s/%s", host, port, database);
    }
}
