package com.oceanbase.obvector4j.integration.remote;

import com.oceanbase.obvector4j.ObVecClient;
import com.oceanbase.obvector4j.version.OceanBaseVersion;
import com.oceanbase.obvector4j.model.SqlDouble;
import com.oceanbase.obvector4j.model.SqlInteger;
import com.oceanbase.obvector4j.model.SqlText;
import com.oceanbase.obvector4j.model.SqlVector;
import com.oceanbase.obvector4j.model.Sqlizable;
import com.oceanbase.obvector4j.filter.Filter;
import com.oceanbase.obvector4j.filter.FilterBuilder;
import com.oceanbase.obvector4j.support.HybridSearchTestFixtures;
import com.oceanbase.obvector4j.support.RemoteOceanBaseTestBase;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Remote integration tests for hybrid search against a real OceanBase cluster.
 */
public class HybridSearchRemoteIT extends RemoteOceanBaseTestBase {

    public HybridSearchRemoteIT(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(HybridSearchRemoteIT.class);
    }

    @Override
    protected void setUp() {
        assumeRemoteAvailable();
    }

    public void testOceanBaseVersionDetected() throws Throwable {
        ObVecClient client = newClient();
        OceanBaseVersion version = client.getOceanBaseVersion();
        boolean hybridSql = client.supportsHybridSearchSql();
        System.out.println("Connected OceanBase version: " + version
                + ", HYBRID_SEARCH SQL supported: " + hybridSql);
        assertTrue("OceanBase major version should be >= 4", version.getMajor() >= 4);
        if (hybridSql) {
            System.out.println("Using HYBRID_SEARCH SQL native path (>= 4.6.0)");
        } else {
            System.out.println("Using legacy hybrid search compatibility path (< 4.6.0)");
        }
    }

    public void testScalarVectorSearchWithFilter() throws Throwable {
        String table = HybridSearchTestFixtures.TABLE_PREFIX + "PRODUCTS";
        setupProductTable(table);

        ObVecClient client = newClient();
        Filter filter = FilterBuilder.and(
                FilterBuilder.and(
                        FilterBuilder.key("category_id").isEqualTo(1),
                        FilterBuilder.key("price").isGreaterThanOrEqualTo(80.0)),
                FilterBuilder.key("price").isLessThanOrEqualTo(200.0));

        ArrayList<HashMap<String, Sqlizable>> results = client.scalarVectorSearch()
                .table(table)
                .vectorColumn("embedding")
                .queryVector(vector(0.5f, 0.1f, 0.6f, 0.9f))
                .filter(filter)
                .topk(5)
                .outputFields("product_id", "product_name", "price", "category_id")
                .search();

        assertNotNull(results);
        assertTrue("Expected filtered results", results.size() > 0);

        Set<Integer> productIds = new HashSet<>();
        for (HashMap<String, Sqlizable> row : results) {
            assertEquals(1, Integer.parseInt(row.get("category_id").toString()));
            double price = Double.parseDouble(row.get("price").toString());
            assertTrue(price >= 80.0 && price <= 200.0);
            productIds.add(Integer.parseInt(row.get("product_id").toString()));
        }
        assertTrue(productIds.contains(1));
        assertFalse(productIds.contains(3));
    }

    public void testTextVectorHybridSearch() throws Throwable {
        String table = HybridSearchTestFixtures.TABLE_PREFIX + "DOC_HYBRID";
        dropTableIfExists(table);
        execSql("CREATE TABLE `" + table + "` ("
                + "c1 INT PRIMARY KEY, vector VECTOR(3), title VARCHAR(255), content VARCHAR(255)"
                + ")" + HybridSearchTestFixtures.ROW_STORE_HEAP);
        HybridSearchTestFixtures.createVectorIndex(this::execSql, table, "idx_vec", "vector");
        HybridSearchTestFixtures.createFulltextIndex(this::execSql, table, "idx_title", "title");
        HybridSearchTestFixtures.createFulltextIndex(this::execSql, table, "idx_content", "content");

        execSql("INSERT INTO `" + table + "` VALUES "
                + "(1, '[1,2,3]', 'hello world', 'oceanbase elasticsearch database'), "
                + "(2, '[1,2,1]', 'hello world query', 'oceanbase mysql database'), "
                + "(3, '[1,1,1]', 'hello world', 'oceanbase oracle database'), "
                + "(4, '[1,3,1]', 'real world', 'postgres oracle database')");
        waitForIndex();

        ObVecClient client = newClient();
        ArrayList<HashMap<String, Sqlizable>> results = client.textVectorSearch()
                .table(table)
                .vectorColumn("vector")
                .queryVector(vector(1f, 2f, 3f))
                .textFields("content")
                .textQuery("oceanbase mysql")
                .metric("l2")
                .topk(10)
                .rankWindowSize(10)
                .outputFields("c1", "content")
                .search();

        assertNotNull(results);
        assertTrue("Expected hybrid search results", results.size() > 0);

        boolean containsMysqlRow = false;
        for (HashMap<String, Sqlizable> row : results) {
            if (row.get("content").toString().contains("mysql")) {
                containsMysqlRow = true;
            }
        }
        assertTrue("Results should include oceanbase mysql row", containsMysqlRow);
    }

    public void testPureVectorSearch() throws Throwable {
        String table = HybridSearchTestFixtures.TABLE_PREFIX + "DOC_VEC";
        dropTableIfExists(table);
        execSql("CREATE TABLE `" + table + "` ("
                + "c1 INT PRIMARY KEY, vector VECTOR(3), content VARCHAR(255)"
                + ")" + HybridSearchTestFixtures.ROW_STORE_HEAP);
        HybridSearchTestFixtures.createVectorIndex(this::execSql, table, "idx_vec", "vector");

        execSql("INSERT INTO `" + table + "` VALUES "
                + "(1, '[1,2,3]', 'oceanbase elasticsearch database'), "
                + "(2, '[1,2,1]', 'oceanbase mysql database'), "
                + "(3, '[1,1,1]', 'oceanbase oracle database')");
        waitForIndex();

        ObVecClient client = newClient();
        ArrayList<HashMap<String, Sqlizable>> results = client.scalarVectorSearch()
                .table(table)
                .vectorColumn("vector")
                .queryVector(vector(1f, 2f, 3f))
                .metric("l2")
                .topk(3)
                .outputFields("c1", "content")
                .search();

        assertNotNull(results);
        assertTrue(results.size() > 0);
        assertEquals(1, Integer.parseInt(results.get(0).get("c1").toString()));
    }

    public void testHybridSearchSqlNativePath() throws Throwable {
        ObVecClient client = newClient();
        assertTrue("HYBRID_SEARCH SQL requires OceanBase >= 4.6.0",
                client.supportsHybridSearchSql());

        String table = HybridSearchTestFixtures.TABLE_PREFIX + "HYBRID_SQL";
        dropTableIfExists(table);
        execSql("CREATE TABLE `" + table + "` ("
                + "c1 INT PRIMARY KEY, vector VECTOR(3), content VARCHAR(255)"
                + ")" + HybridSearchTestFixtures.ROW_STORE_HEAP);
        HybridSearchTestFixtures.createVectorIndex(this::execSql, table, "idx_vec", "vector");
        execSql("INSERT INTO `" + table + "` VALUES (1, '[1,2,3]', 'oceanbase mysql database')");
        waitForIndex();

        ArrayList<HashMap<String, Sqlizable>> results = client.scalarVectorSearch()
                .table(table)
                .vectorColumn("vector")
                .queryVector(vector(1f, 2f, 3f))
                .topk(1)
                .outputFields("c1")
                .search();

        assertEquals(1, results.size());
        assertEquals(1, Integer.parseInt(results.get(0).get("c1").toString()));
    }

    public void testCustomHybridSearchDsl() throws Throwable {
        ObVecClient client = newClient();
        assertTrue(client.supportsHybridSearchSql());

        String table = HybridSearchTestFixtures.TABLE_PREFIX + "CUSTOM_DSL";
        dropTableIfExists(table);
        execSql("CREATE TABLE `" + table + "` ("
                + "c1 INT PRIMARY KEY, vector VECTOR(3), content VARCHAR(255)"
                + ")" + HybridSearchTestFixtures.ROW_STORE_HEAP);
        HybridSearchTestFixtures.createVectorIndex(this::execSql, table, "idx_vec", "vector");
        HybridSearchTestFixtures.createFulltextIndex(this::execSql, table, "idx_content", "content");
        execSql("INSERT INTO `" + table + "` VALUES "
                + "(1, '[1,2,3]', 'oceanbase mysql database'), "
                + "(2, '[1,2,1]', 'oceanbase oracle database')");
        waitForIndex();

        ArrayList<HashMap<String, Sqlizable>> results = client.customHybridSearch()
                .table(table)
                .queryDsl("{\"match\":{\"content\":\"oceanbase mysql\"}}")
                .knnDsl("{\"field\":\"vector\",\"k\":5,\"query_vector\":\"[1.0,2.0,3.0]\"}")
                .rankDsl("{\"rrf\":{\"rank_constant\":60,\"rank_window_size\":10}}")
                .size(3)
                .outputFields("c1", "content")
                .search();

        assertNotNull(results);
        assertTrue(results.size() > 0);

        ArrayList<HashMap<String, Sqlizable>> queryRows = client.querySql(
                "SELECT c1 FROM `" + table + "` WHERE c1 = 1");
        assertEquals(1, queryRows.size());
    }

    /**
     * doc_table style: HYBRID_SEARCH native SQL for text + vector RRF (4.6.0+).
     */
    public void testHybridSearchTextVectorRrfNativeSql() throws Throwable {
        ObVecClient client = newClient();
        assertTrue(client.supportsHybridSearchSql());

        String table = HybridSearchTestFixtures.TABLE_PREFIX + "DOC_RRF_SQL";
        dropTableIfExists(table);
        execSql("CREATE TABLE `" + table + "` ("
                + "c1 INT PRIMARY KEY, vector VECTOR(3), query VARCHAR(255), content VARCHAR(255)"
                + ")" + HybridSearchTestFixtures.ROW_STORE_HEAP);
        HybridSearchTestFixtures.createVectorIndex(this::execSql, table, "idx_vec", "vector");
        HybridSearchTestFixtures.createFulltextIndex(this::execSql, table, "idx_query", "query");
        HybridSearchTestFixtures.createFulltextIndex(this::execSql, table, "idx_content", "content");

        execSql("INSERT INTO `" + table + "` VALUES "
                + "(1, '[1,2,3]', 'hello world', 'oceanbase elasticsearch database'), "
                + "(2, '[1,2,1]', 'hello world, what is your name', 'oceanbase mysql database'), "
                + "(3, '[1,1,1]', 'hello world, how are you', 'oceanbase oracle database')");
        waitForIndex();

        ArrayList<HashMap<String, Sqlizable>> results = client.textVectorSearch()
                .table(table)
                .vectorColumn("vector")
                .queryVector(vector(1f, 2f, 3f))
                .textField("content")
                .textQuery("oceanbase mysql")
                .metric("l2")
                .topk(5)
                .rankWindowSize(10)
                .outputFields("c1", "content")
                .search();

        assertNotNull(results);
        assertTrue(results.size() > 0);
        boolean hasMysql = false;
        for (HashMap<String, Sqlizable> row : results) {
            if (row.get("content").toString().contains("mysql")) {
                hasMysql = true;
            }
        }
        assertTrue("RRF hybrid search should return mysql-related row", hasMysql);
    }

    /**
     * HYBRID_SEARCH native SQL: text + vector with scalar Filter DSL (4.6.0+).
     */
    public void testHybridSearchTextVectorWithFilterNativeSql() throws Throwable {
        ObVecClient client = newClient();
        assertTrue(client.supportsHybridSearchSql());

        String table = HybridSearchTestFixtures.TABLE_PREFIX + "DOC_FILTER_SQL";
        dropTableIfExists(table);
        execSql("CREATE TABLE `" + table + "` ("
                + "c1 INT PRIMARY KEY, status INT, vector VECTOR(3), content VARCHAR(255)"
                + ")" + HybridSearchTestFixtures.ROW_STORE_HEAP);
        HybridSearchTestFixtures.createVectorIndex(this::execSql, table, "idx_vec", "vector");
        HybridSearchTestFixtures.createFulltextIndex(this::execSql, table, "idx_content", "content");

        execSql("INSERT INTO `" + table + "` VALUES "
                + "(1, 1, '[1,2,3]', 'oceanbase mysql database'), "
                + "(2, 0, '[1,2,1]', 'oceanbase mysql inactive'), "
                + "(3, 1, '[1,1,1]', 'oceanbase oracle database')");
        waitForIndex();

        Filter filter = FilterBuilder.key("status").isEqualTo(1);
        ArrayList<HashMap<String, Sqlizable>> results = client.textVectorSearch()
                .table(table)
                .vectorColumn("vector")
                .queryVector(vector(1f, 2f, 3f))
                .textField("content")
                .textQuery("oceanbase mysql")
                .filter(filter)
                .topk(5)
                .outputFields("c1", "status", "content")
                .search();

        assertNotNull(results);
        for (HashMap<String, Sqlizable> row : results) {
            assertEquals(1, Integer.parseInt(row.get("status").toString()));
            assertFalse(row.get("content").toString().contains("inactive"));
        }
    }

    private void setupProductTable(String table) throws Throwable {
        dropTableIfExists(table);
        execSql("CREATE TABLE `" + table + "` ("
                + "product_id INT PRIMARY KEY, product_name VARCHAR(255), "
                + "category_id INT, price DECIMAL(10,2), embedding VECTOR(4)"
                + ")" + HybridSearchTestFixtures.ROW_STORE_HEAP);
        HybridSearchTestFixtures.createVectorIndex(this::execSql, table, "idx_emb", "embedding");

        ObVecClient client = newClient();
        ArrayList<Sqlizable[]> rows = new ArrayList<>();
        rows.add(new Sqlizable[] {
                new SqlInteger(1), new SqlText("Keyboard"), new SqlInteger(1),
                new SqlDouble(149.0), new SqlVector(vector(0.5f, 0.1f, 0.6f, 0.9f))
        });
        rows.add(new Sqlizable[] {
                new SqlInteger(2), new SqlText("Headset"), new SqlInteger(1),
                new SqlDouble(89.0), new SqlVector(vector(0.1f, 0.9f, 0.2f, 0.0f))
        });
        rows.add(new Sqlizable[] {
                new SqlInteger(3), new SqlText("Yoga Mat"), new SqlInteger(2),
                new SqlDouble(49.99), new SqlVector(vector(0.1f, 0.9f, 0.3f, 0.0f))
        });
        rows.add(new Sqlizable[] {
                new SqlInteger(4), new SqlText("Monitor"), new SqlInteger(1),
                new SqlDouble(299.0), new SqlVector(vector(0.4f, 0.2f, 0.7f, 0.1f))
        });
        client.insert(table,
                new String[] {"product_id", "product_name", "category_id", "price", "embedding"},
                rows);
        waitForIndex();
    }
}
