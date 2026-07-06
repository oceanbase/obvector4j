package com.oceanbase.obvector4j.integration.remote;

import com.oceanbase.obvector4j.ObVecClient;
import com.oceanbase.obvector4j.hybrid.core.HybridSearch;
import com.oceanbase.obvector4j.hybrid.core.HybridSearchSupport;
import com.oceanbase.obvector4j.hybrid.core.dsl.HybridDsl;
import com.oceanbase.obvector4j.hybrid.core.dsl.HybridDslKeys;
import com.oceanbase.obvector4j.model.Sqlizable;
import com.oceanbase.obvector4j.support.RemoteOceanBaseTestBase;
import java.util.ArrayList;
import java.util.HashMap;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.junit.Assume;

/**
 * E2E tests aligned with OBServer HYBRID_SEARCH spec document (4.6.0+ DSL).
 */
public class HybridSearchDocIT extends RemoteOceanBaseTestBase {

    private static final String TABLE = "OB_DOC_TEST";
    private static volatile boolean docTableReady;
    private static final Object TABLE_LOCK = new Object();

    public HybridSearchDocIT(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(HybridSearchDocIT.class);
    }

    @Override
    protected void setUp() {
        assumeRemoteAvailable();
    }

    @Override
    protected void tearDown() throws Exception {
        synchronized (TABLE_LOCK) {
            if (docTableReady) {
                dropTableIfExists(TABLE);
                docTableReady = false;
            }
        }
    }

    private ObVecClient prepareHybridClient() throws Throwable {
        ObVecClient client = newClient();
        Assume.assumeTrue("HYBRID_SEARCH DSL requires OceanBase >= 4.6.0",
                client.supportsHybridSearchSql());
        ensureDocTable();
        return client;
    }

    private void ensureDocTable() throws Exception {
        synchronized (TABLE_LOCK) {
            if (!docTableReady) {
                dropTableIfExists(TABLE);
                setupDocTestTable();
                waitForIndex();
                docTableReady = true;
            }
        }
    }

    public void testHybridSearchEntryRequiresVersion() throws Throwable {
        ObVecClient client = prepareHybridClient();
        HybridSearchSupport.require(client.getOceanBaseVersion());
        HybridSearch hs = client.hybridSearch();
        assertNotNull(hs);
    }

    /** Document: single-path knn vector search */
    public void testKnnOnly() throws Throwable {
        ObVecClient client = prepareHybridClient();
        ArrayList<HashMap<String, Sqlizable>> rows = client.hybridSearch()
                .customSearch()
                .table(TABLE)
                .knn(HybridDsl.knn("vector_col", vector(0.1f, 0.2f, 0.3f, 0.4f), 5))
                .size(5)
                .outputFields("id", "title")
                .search();
        assertTrue(rows.size() > 0);
        assertEquals(1, Integer.parseInt(rows.get(0).get("id").toString()));
    }

    /** Document: knn + scalar filter (range id >= 5) */
    public void testKnnWithRangeFilter() throws Throwable {
        ObVecClient client = prepareHybridClient();
        ArrayList<HashMap<String, Sqlizable>> rows = client.hybridSearch()
                .customSearch()
                .table(TABLE)
                .knn(HybridDsl.knn("vector_col", vector(0.1f, 0.2f, 0.3f, 0.4f), 5)
                        .filter(HybridDsl.range("id").gte(5)))
                .size(5)
                .outputFields("id")
                .search();
        for (HashMap<String, Sqlizable> row : rows) {
            assertTrue(Integer.parseInt(row.get("id").toString()) >= 5);
        }
    }

    /** Document: single-path full-text match */
    public void testMatchOnly() throws Throwable {
        ObVecClient client = prepareHybridClient();
        ArrayList<HashMap<String, Sqlizable>> rows = client.hybridSearch()
                .customSearch()
                .table(TABLE)
                .query(HybridDsl.match("content", "Python JavaScript"))
                .size(10)
                .outputFields("id", "content")
                .search();
        assertTrue(rows.size() > 0);
        boolean hasPython = false;
        for (HashMap<String, Sqlizable> row : rows) {
            if (row.get("content").toString().contains("Python")) {
                hasPython = true;
            }
        }
        assertTrue(hasPython);
    }

    /** Document: full-text + bool filter (range id >= 5) */
    public void testMatchWithBoolFilter() throws Throwable {
        ObVecClient client = prepareHybridClient();
        ArrayList<HashMap<String, Sqlizable>> rows = client.hybridSearch()
                .customSearch()
                .table(TABLE)
                .query(HybridDsl.bool()
                        .must(HybridDsl.match("content", "Python JavaScript"))
                        .filter(HybridDsl.range("id").gte(5)))
                .size(10)
                .outputFields("id", "content")
                .search();
        assertTrue(rows.size() > 0);
        for (HashMap<String, Sqlizable> row : rows) {
            assertTrue(Integer.parseInt(row.get("id").toString()) >= 5);
        }
    }

    /** Document explain: bool must match + filter range + knn */
    public void testHybridBoolMatchFilterKnn() throws Throwable {
        ObVecClient client = prepareHybridClient();
        ArrayList<HashMap<String, Sqlizable>> rows = client.hybridSearch()
                .customSearch()
                .table(TABLE)
                .query(HybridDsl.bool()
                        .must(HybridDsl.match("content", "Python JavaScript"))
                        .filter(
                                HybridDsl.range("id").gte(3),
                                HybridDsl.range("id").lte(10)))
                .knn(HybridDsl.knn("vector_col", vector(0.1f, 0.2f, 0.3f, 0.4f), 10))
                .size(10)
                .outputFields("id")
                .search();
        assertTrue(rows.size() > 0);
        boolean hasFilteredHit = false;
        for (HashMap<String, Sqlizable> row : rows) {
            int id = Integer.parseInt(row.get("id").toString());
            if (id >= 3 && id <= 10) {
                hasFilteredHit = true;
            }
        }
        assertTrue("expected at least one id in [3,10] from text+filter branch", hasFilteredHit);
    }

    /** Document: multi-path knn array */
    public void testMultiKnn() throws Throwable {
        ObVecClient client = prepareHybridClient();
        ArrayList<HashMap<String, Sqlizable>> rows = client.hybridSearch()
                .customSearch()
                .table(TABLE)
                .knn(
                        HybridDsl.knn("vector_col", vector(0.1f, 0.2f, 0.3f, 0.4f), 5),
                        HybridDsl.knn("vector_col", vector(0.4f, 0.3f, 0.2f, 0.1f), 5))
                .size(7)
                .outputFields("id", "title")
                .search();
        assertTrue(rows.size() >= 5);
    }

    /** Document: text + vector + RRF */
    public void testTextVectorRrf() throws Throwable {
        ObVecClient client = prepareHybridClient();
        String dsl = HybridDsl.textVectorRrf(
                HybridDsl.match("content", "python javascript"),
                HybridDsl.knn("vector_col", vector(0.1f, 0.2f, 0.3f, 0.4f), 5),
                6, 10, 60).toJsonString();
        ArrayList<HashMap<String, Sqlizable>> rows = client.hybridSearch()
                .searchWithDsl(TABLE, dsl, new String[] {"id", "title"}, null);
        assertTrue(rows.size() > 0);
    }

    /** Document: WRRF with boost on query and knn */
    public void testTextVectorWrrfWithBoost() throws Throwable {
        ObVecClient client = prepareHybridClient();
        ArrayList<HashMap<String, Sqlizable>> rows = client.hybridSearch()
                .customSearch()
                .table(TABLE)
                .query(HybridDsl.match("content")
                        .query("python javascript")
                        .param(HybridDslKeys.BOOST, 0.3))
                .knn(HybridDsl.knn("vector_col", vector(0.1f, 0.2f, 0.3f, 0.4f), 5).boost(0.7))
                .rank(HybridDsl.rrf(10, 60))
                .size(6)
                .outputFields("id", "content")
                .search();
        assertTrue(rows.size() > 0);
    }

    /** Document: weighted_sum minmax + min_score */
    public void testWeightedSumMinmaxWithMinScore() throws Throwable {
        ObVecClient client = prepareHybridClient();
        ArrayList<HashMap<String, Sqlizable>> rows = client.hybridSearch()
                .customSearch()
                .table(TABLE)
                .query(HybridDsl.match("content")
                        .query("python javascript")
                        .param(HybridDslKeys.BOOST, 0.3))
                .knn(HybridDsl.knn("vector_col", vector(0.1f, 0.2f, 0.3f, 0.4f), 5).boost(0.7))
                .rank(HybridDsl.weightedSum(HybridDslKeys.Normalizer.MINMAX, 10))
                .minScore(0.0)
                .size(5)
                .outputFields("id")
                .search();
        assertNotNull(rows);
    }

    /** Document: bool should + filter (default minimum_should_match=0 when filter present) */
    public void testBoolShouldWithFilter() throws Throwable {
        ObVecClient client = prepareHybridClient();
        ArrayList<HashMap<String, Sqlizable>> rows = client.hybridSearch()
                .customSearch()
                .table(TABLE)
                .query(HybridDsl.bool()
                        .should(
                                HybridDsl.match("title", "data"),
                                HybridDsl.match("content", "python java"))
                        .filter(HybridDsl.range("id").gte(5)))
                .size(10)
                .outputFields("id", "title")
                .search();
        assertTrue(rows.size() > 0);
        for (HashMap<String, Sqlizable> row : rows) {
            assertTrue(Integer.parseInt(row.get("id").toString()) >= 5);
        }
    }

    /** Document: bool should + filter + explicit minimum_should_match */
    public void testBoolShouldMinimumShouldMatch() throws Throwable {
        ObVecClient client = prepareHybridClient();
        ArrayList<HashMap<String, Sqlizable>> rows = client.hybridSearch()
                .customSearch()
                .table(TABLE)
                .query(HybridDsl.bool()
                        .should(
                                HybridDsl.match("title", "data"),
                                HybridDsl.match("content", "python java"))
                        .filter(HybridDsl.range("id").gte(5))
                        .minimumShouldMatch(1))
                .size(10)
                .outputFields("id", "title")
                .search();
        assertTrue(rows.size() > 0);
        assertEquals("Data Systems", rows.get(0).get("title").toString());
    }

    /** Document: array_contains filter */
    public void testArrayContains() throws Throwable {
        ObVecClient client = prepareHybridClient();
        ArrayList<HashMap<String, Sqlizable>> rows = client.hybridSearch()
                .customSearch()
                .table(TABLE)
                .query(HybridDsl.bool()
                        .filter(HybridDsl.arrayContains("tags_array1", "ios")))
                .size(5)
                .outputFields("id", "tags_array1")
                .search();
        assertEquals(1, rows.size());
        assertEquals(10, Integer.parseInt(rows.get(0).get("id").toString()));
    }

    /** Document: json_extract via dotted field term (doc_json.name) */
    public void testJsonExtractTerm() throws Throwable {
        ObVecClient client = prepareHybridClient();
        ArrayList<HashMap<String, Sqlizable>> rows = client.hybridSearch()
                .customSearch()
                .table(TABLE)
                .query(HybridDsl.bool()
                        .filter(HybridDsl.term("doc_json.name", "doc2")))
                .size(5)
                .outputFields("id")
                .search();
        assertEquals(1, rows.size());
        assertEquals(2, Integer.parseInt(rows.get(0).get("id").toString()));
    }

    private void setupDocTestTable() throws Exception {
        execSql("CREATE TABLE `" + TABLE + "` ("
                + "id INT PRIMARY KEY, "
                + "title VARCHAR(255), "
                + "content TEXT, "
                + "vector_col VECTOR(4), "
                + "doc_json JSON, "
                + "tags_array1 ARRAY(VARCHAR(255))"
                + ") ORGANIZATION HEAP WITH COLUMN GROUP(all columns)");

        execSql("CREATE VECTOR INDEX idx_doc_vec ON `" + TABLE + "`(vector_col) "
                + "WITH (distance=l2, type=hnsw, lib=vsag)");
        execSql("CREATE FULLTEXT INDEX idx_doc_title ON `" + TABLE + "`(title)");
        execSql("CREATE FULLTEXT INDEX idx_doc_content ON `" + TABLE + "`(content)");
        try {
            execSql("CREATE SEARCH INDEX idx_doc_json ON `" + TABLE + "`(doc_json)");
            execSql("CREATE SEARCH INDEX idx_doc_tags ON `" + TABLE + "`(tags_array1)");
        } catch (Exception e) {
            System.out.println("SEARCH INDEX optional: " + e.getMessage());
        }

        execSql("INSERT INTO `" + TABLE + "` VALUES "
                + "(1, 'Machine Learning Basics', "
                + "'Introduction to machine learning algorithms and techniques', "
                + "'[0.1,0.2,0.3,0.4]', "
                + "'{\"name\":\"doc1\",\"tags\":[\"database\",\"oceanbase\"]}', "
                + "['database','oceanbase']), "
                + "(2, 'Data Doc', 'Reference data science', '[0.9,0.1,0.2,0.3]', "
                + "'{\"name\":\"doc2\",\"tags\":[\"database\",\"mysql\"]}', "
                + "['database','mysql']), "
                + "(4, 'Python Programming', "
                + "'Learn Python programming from beginner to advanced', "
                + "'[0.4,0.5,0.6,0.7]', "
                + "'{\"name\":\"doc4\",\"tags\":[\"programming\"]}', "
                + "['programming','python']), "
                + "(6, 'Data Systems', "
                + "'Relational database design and SQL optimization', "
                + "'[0.2,0.3,0.4,0.5]', "
                + "'{\"name\":\"doc6\",\"tags\":[\"database\"]}', "
                + "['database','sql']), "
                + "(7, 'Web Development', "
                + "'Modern web development with HTML CSS and JavaScript', "
                + "'[0.6,0.7,0.8,0.9]', "
                + "'{\"name\":\"doc7\",\"tags\":[\"web\"]}', "
                + "['web','javascript']), "
                + "(10, 'Mobile App Development', "
                + "'iOS and Android app development fundamentals', "
                + "'[0.1,0.9,0.3,0.7]', "
                + "'{\"name\":\"doc10\",\"tags\":[\"mobile\"]}', "
                + "['mobile','ios'])");
    }
}
