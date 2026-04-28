package com.oceanbase;

import java.util.ArrayList;
import java.util.HashMap;

import com.oceanbase.obvec_jdbc.DataType;
import com.oceanbase.obvec_jdbc.IndexParam;
import com.oceanbase.obvec_jdbc.IndexParams;
import com.oceanbase.obvec_jdbc.ObCollectionSchema;
import com.oceanbase.obvec_jdbc.ObFieldSchema;
import com.oceanbase.obvec_jdbc.ObVecClient;
import com.oceanbase.obvec_jdbc.SqlInteger;
import com.oceanbase.obvec_jdbc.SqlText;
import com.oceanbase.obvec_jdbc.SqlDouble;
import com.oceanbase.obvec_jdbc.SqlVector;
import com.oceanbase.obvec_jdbc.Sqlizable;
import com.oceanbase.obvec_jdbc.filter.Filter;
import com.oceanbase.obvec_jdbc.filter.FilterBuilder;

import java.util.Arrays;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for hybrid search functionality.
 */
public class HybridSearchTest extends TestCase {
    
    private static String URI;
    private static String USER;
    private static String PASSWORD;
    
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public HybridSearchTest(String testName) {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(HybridSearchTest.class);
    }

    /**
     * Initialize OceanBase container before all tests
     */
    static {
        try {
            Class.forName("com.oceanbase.jdbc.Driver");
            OceanBaseContainerTestBase.initContainer();
            URI = OceanBaseContainerTestBase.getJdbcUrl();
            USER = OceanBaseContainerTestBase.getUsername();
            PASSWORD = OceanBaseContainerTestBase.getPassword();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /**
     * Test full-text + vector hybrid search using simplified API
     */
    public void testHybridTextVectorSearch() {
        try {
            Class.forName("com.oceanbase.jdbc.Driver");
            
            String tb_name = "HYBRID_TEXT_VECTOR_TEST";
            ObVecClient ob = new ObVecClient(URI, USER, PASSWORD);
            
            // Drop existing table
            if (ob.hasCollection(tb_name)) {
                ob.dropCollection(tb_name);
            }
            
            // Create table schema
            ObCollectionSchema collectionSchema = new ObCollectionSchema();
            
            // Vector field (128 dimensions)
            ObFieldSchema vec_field = new ObFieldSchema("embedding", DataType.FLOAT_VECTOR);
            vec_field.Dim(128).IsNullable(false);
            collectionSchema.addField(vec_field);
            
            // Text fields for full-text search
            ObFieldSchema title_field = new ObFieldSchema("title", DataType.STRING);
            title_field.IsNullable(false);
            collectionSchema.addField(title_field);
            
            ObFieldSchema content_field = new ObFieldSchema("content", DataType.STRING);
            content_field.IsNullable(true);
            collectionSchema.addField(content_field);
            
            // Create vector index
            IndexParams index_params = new IndexParams();
            IndexParam index_param = new IndexParam("vidx_embedding", "embedding");
            index_params.addIndex(index_param);
            collectionSchema.setIndexParams(index_params);
            
            ob.createCollection(tb_name, collectionSchema);
            
            // Create full-text indexes
            try {
                ob.createFulltextIndex(tb_name, "ft_title", "title");
            } catch (Throwable e) {
                // Ignore if index creation fails
            }
            
            try {
                ob.createFulltextIndex(tb_name, "ft_content", "content");
            } catch (Throwable e) {
                // Ignore if index creation fails
            }
            
            // Insert test data
            ArrayList<Sqlizable[]> insert_rows = new ArrayList<>();
            
            Sqlizable[] row1 = {
                new SqlVector(generateRandomVector(128)),
                new SqlText("OceanBase Database Introduction"),
                new SqlText("OceanBase is a distributed relational database system that supports vector search")
            };
            insert_rows.add(row1);
            
            Sqlizable[] row2 = {
                new SqlVector(generateRandomVector(128)),
                new SqlText("Vector Search Technology"),
                new SqlText("Vector search enables semantic similarity search in databases using embedding vectors")
            };
            insert_rows.add(row2);
            
            Sqlizable[] row3 = {
                new SqlVector(generateRandomVector(128)),
                new SqlText("Hybrid Search in OceanBase"),
                new SqlText("Hybrid search combines full-text search and vector search for better results")
            };
            insert_rows.add(row3);
            
            Sqlizable[] row4 = {
                new SqlVector(generateRandomVector(128)),
                new SqlText("Database Management System"),
                new SqlText("Database systems provide efficient data storage and retrieval capabilities")
            };
            insert_rows.add(row4);
            
            ob.insert(tb_name, new String[] {"embedding", "title", "content"}, insert_rows);
            
            // Wait for index building
            Thread.sleep(3000);
            
            float[] queryVector = generateRandomVector(128);
            
            ArrayList<HashMap<String, Sqlizable>> results1 = ob.textVectorSearch()
                .table(tb_name)
                .queryVector(queryVector)
                .textFields("title", "content")
                .textQuery("OceanBase database")
                .metric("cosine")
                .topk(3)
                .search();
            
            assertNotNull("Results should not be null", results1);
            assertTrue("Should return results", results1.size() > 0);
            
            ArrayList<HashMap<String, Sqlizable>> results2 = ob.textVectorSearch()
                .table(tb_name)
                .queryVector(generateRandomVector(128))
                .textField("title")
                .textQuery("OceanBase")
                .topk(2)
                .outputField("title")
                .search();
            
            assertNotNull("Results should not be null", results2);
            assertTrue("Should return results", results2.size() > 0);
            
            ArrayList<HashMap<String, Sqlizable>> results3 = ob.textVectorSearch()
                .table(tb_name)
                .queryVector(generateRandomVector(128))
                .textFields("title", "content")
                .textQuery("database")
                .topk(2)
                .outputFields("title")  // Only output title
                .search();
            
            assertNotNull("Results should not be null", results3);
            
        } catch (Throwable e) {
            e.printStackTrace();
            fail("Test failed with exception: " + e.getMessage());
        }
    }

    /**
     * Test scalar + vector hybrid search using simplified API
     */
    public void testHybridScalarVectorSearch() {
        try {
            Class.forName("com.oceanbase.jdbc.Driver");
            
            String tb_name = "HYBRID_SCALAR_VECTOR_TEST";
            ObVecClient ob = new ObVecClient(URI, USER, PASSWORD);
            
            // Drop existing table
            if (ob.hasCollection(tb_name)) {
                ob.dropCollection(tb_name);
            }
            
            // Create table schema
            ObCollectionSchema collectionSchema = new ObCollectionSchema();
            
            // Vector field (64 dimensions)
            ObFieldSchema vec_field = new ObFieldSchema("embedding", DataType.FLOAT_VECTOR);
            vec_field.Dim(64).IsNullable(false);
            collectionSchema.addField(vec_field);
            
            // Scalar fields for filtering
            ObFieldSchema price_field = new ObFieldSchema("price", DataType.DOUBLE);
            price_field.IsNullable(false);
            collectionSchema.addField(price_field);
            
            ObFieldSchema category_field = new ObFieldSchema("category_id", DataType.INT32);
            category_field.IsNullable(false);
            collectionSchema.addField(category_field);
            
            ObFieldSchema status_field = new ObFieldSchema("status", DataType.INT32);
            status_field.IsNullable(false);
            collectionSchema.addField(status_field);
            
            // Create vector index
            IndexParams index_params = new IndexParams();
            IndexParam index_param = new IndexParam("vidx_embedding", "embedding");
            index_params.addIndex(index_param);
            collectionSchema.setIndexParams(index_params);
            
            ob.createCollection(tb_name, collectionSchema);
            
            // Insert test data
            ArrayList<Sqlizable[]> insert_rows = new ArrayList<>();
            
            Sqlizable[] row1 = {
                new SqlVector(generateRandomVector(64)),
                new SqlDouble(100.0),
                new SqlInteger(1),
                new SqlInteger(1)
            };
            insert_rows.add(row1);
            
            Sqlizable[] row2 = {
                new SqlVector(generateRandomVector(64)),
                new SqlDouble(200.0),
                new SqlInteger(1),
                new SqlInteger(1)
            };
            insert_rows.add(row2);
            
            Sqlizable[] row3 = {
                new SqlVector(generateRandomVector(64)),
                new SqlDouble(150.0),
                new SqlInteger(2),
                new SqlInteger(1)
            };
            insert_rows.add(row3);
            
            Sqlizable[] row4 = {
                new SqlVector(generateRandomVector(64)),
                new SqlDouble(300.0),
                new SqlInteger(1),
                new SqlInteger(0)
            };
            insert_rows.add(row4);
            
            Sqlizable[] row5 = {
                new SqlVector(generateRandomVector(64)),
                new SqlDouble(80.0),
                new SqlInteger(1),
                new SqlInteger(1)
            };
            insert_rows.add(row5);
            
            ob.insert(tb_name, new String[] {"embedding", "price", "category_id", "status"}, insert_rows);
            
            // Wait for index building
            Thread.sleep(3000);
            
            float[] queryVector = generateRandomVector(64);
            
            ArrayList<HashMap<String, Sqlizable>> results1 = ob.scalarVectorSearch()
                .table(tb_name)
                .queryVector(queryVector)
                .filter("category_id = 1 AND price >= 50 AND price <= 250 AND status = 1")
                .metric("l2")
                .topk(5)
                .outputFields("price", "category_id", "status")
                .search();
            
            assertNotNull("Results should not be null", results1);
            assertTrue("Should return results", results1.size() > 0);
            
            // Verify filter conditions
            for (HashMap<String, Sqlizable> row : results1) {
                Sqlizable categoryId = row.get("category_id");
                Sqlizable price = row.get("price");
                Sqlizable status = row.get("status");
                
                assertNotNull("category_id should not be null", categoryId);
                assertNotNull("price should not be null", price);
                assertNotNull("status should not be null", status);
                
                int catId = Integer.parseInt(categoryId.toString());
                double priceVal = Double.parseDouble(price.toString());
                int statusVal = Integer.parseInt(status.toString());
                
                assertEquals("category_id should be 1", 1, catId);
                assertTrue("price should be >= 50", priceVal >= 50.0);
                assertTrue("price should be <= 250", priceVal <= 250.0);
                assertEquals("status should be 1", 1, statusVal);
            }
            
            ArrayList<HashMap<String, Sqlizable>> results2 = ob.scalarVectorSearch()
                .table(tb_name)
                .queryVector(generateRandomVector(64))
                .filter("category_id = 1")
                .topk(3)
                .outputField("price")
                .search();
            
            assertNotNull("Results should not be null", results2);
            
            ArrayList<HashMap<String, Sqlizable>> results3 = ob.scalarVectorSearch()
                .table(tb_name)
                .queryVector(generateRandomVector(64))
                .filter("price >= 100 AND price <= 200")
                .topk(3)
                .outputFields(
                    new String[]{"price", "category_id"},
                    new DataType[]{DataType.DOUBLE, DataType.INT32}
                )
                .search();
            
            assertNotNull("Results should not be null", results3);
            
        } catch (Throwable e) {
            e.printStackTrace();
            fail("Test failed with exception: " + e.getMessage());
        }
    }

    /**
     * Test new Filter API with type-safe Filter objects
     */
    public void testFilterAPI() {
        try {
            Class.forName("com.oceanbase.jdbc.Driver");
            
            String tb_name = "HYBRID_SCALAR_VECTOR_FILTER_TEST";
            ObVecClient ob = new ObVecClient(URI, USER, PASSWORD);
            
            // Drop existing table
            if (ob.hasCollection(tb_name)) {
                ob.dropCollection(tb_name);
            }
            
            // Create table schema
            ObCollectionSchema collectionSchema = new ObCollectionSchema();
            
            // Vector field (64 dimensions)
            ObFieldSchema vec_field = new ObFieldSchema("embedding", DataType.FLOAT_VECTOR);
            vec_field.Dim(64).IsNullable(false);
            collectionSchema.addField(vec_field);
            
            // Scalar fields for filtering
            ObFieldSchema price_field = new ObFieldSchema("price", DataType.DOUBLE);
            price_field.IsNullable(false);
            collectionSchema.addField(price_field);
            
            ObFieldSchema category_field = new ObFieldSchema("category_id", DataType.INT32);
            category_field.IsNullable(false);
            collectionSchema.addField(category_field);
            
            ObFieldSchema status_field = new ObFieldSchema("status", DataType.INT32);
            status_field.IsNullable(false);
            collectionSchema.addField(status_field);
            
            // Create vector index
            IndexParams index_params = new IndexParams();
            IndexParam index_param = new IndexParam("vidx_embedding", "embedding");
            index_params.addIndex(index_param);
            collectionSchema.setIndexParams(index_params);
            
            ob.createCollection(tb_name, collectionSchema);
            
            // Insert test data
            ArrayList<Sqlizable[]> insert_rows = new ArrayList<>();
            
            Sqlizable[] row1 = {
                new SqlVector(generateRandomVector(64)),
                new SqlDouble(100.0),
                new SqlInteger(1),
                new SqlInteger(1)
            };
            insert_rows.add(row1);
            
            Sqlizable[] row2 = {
                new SqlVector(generateRandomVector(64)),
                new SqlDouble(200.0),
                new SqlInteger(1),
                new SqlInteger(1)
            };
            insert_rows.add(row2);
            
            Sqlizable[] row3 = {
                new SqlVector(generateRandomVector(64)),
                new SqlDouble(150.0),
                new SqlInteger(2),
                new SqlInteger(1)
            };
            insert_rows.add(row3);
            
            Sqlizable[] row4 = {
                new SqlVector(generateRandomVector(64)),
                new SqlDouble(300.0),
                new SqlInteger(1),
                new SqlInteger(0)
            };
            insert_rows.add(row4);
            
            Sqlizable[] row5 = {
                new SqlVector(generateRandomVector(64)),
                new SqlDouble(80.0),
                new SqlInteger(1),
                new SqlInteger(1)
            };
            insert_rows.add(row5);
            
            Sqlizable[] row6 = {
                new SqlVector(generateRandomVector(64)),
                new SqlDouble(250.0),
                new SqlInteger(3),
                new SqlInteger(1)
            };
            insert_rows.add(row6);
            
            ob.insert(tb_name, new String[] {"embedding", "price", "category_id", "status"}, insert_rows);
            
            // Wait for index building
            Thread.sleep(3000);
            
            float[] queryVector = generateRandomVector(64);
            
            Filter filter1 = FilterBuilder.key("category_id").isEqualTo(1);
            ArrayList<HashMap<String, Sqlizable>> results1 = ob.scalarVectorSearch()
                .table(tb_name)
                .queryVector(queryVector)
                .filter(filter1)
                .topk(10)
                .outputFields("price", "category_id", "status")
                .search();
            
            assertNotNull("Results should not be null", results1);
            assertTrue("Should return results", results1.size() > 0);
            
            for (HashMap<String, Sqlizable> row : results1) {
                Sqlizable categoryId = row.get("category_id");
                assertNotNull("category_id should not be null", categoryId);
                int catId = Integer.parseInt(categoryId.toString());
                assertEquals("category_id should be 1", 1, catId);
            }
            
            Filter filter2 = FilterBuilder.and(
                FilterBuilder.key("price").isGreaterThanOrEqualTo(100.0),
                FilterBuilder.key("price").isLessThanOrEqualTo(200.0)
            );
            
            ArrayList<HashMap<String, Sqlizable>> results2 = ob.scalarVectorSearch()
                .table(tb_name)
                .queryVector(generateRandomVector(64))
                .filter(filter2)
                .topk(10)
                .outputFields("price", "category_id")
                .search();
            
            assertNotNull("Results should not be null", results2);
            
            for (HashMap<String, Sqlizable> row : results2) {
                Sqlizable price = row.get("price");
                assertNotNull("price should not be null", price);
                double priceVal = Double.parseDouble(price.toString());
                assertTrue("price should be >= 100", priceVal >= 100.0);
                assertTrue("price should be <= 200", priceVal <= 200.0);
            }
            
            Filter filter3 = FilterBuilder.and(
                FilterBuilder.key("category_id").isEqualTo(1),
                FilterBuilder.and(
                    FilterBuilder.and(
                        FilterBuilder.key("price").isGreaterThanOrEqualTo(50.0),
                        FilterBuilder.key("price").isLessThanOrEqualTo(250.0)
                    ),
                    FilterBuilder.key("status").isEqualTo(1)
                )
            );
            
            ArrayList<HashMap<String, Sqlizable>> results3 = ob.scalarVectorSearch()
                .table(tb_name)
                .queryVector(generateRandomVector(64))
                .filter(filter3)
                .topk(10)
                .outputFields("price", "category_id", "status")
                .search();
            
            assertNotNull("Results should not be null", results3);
            
            for (HashMap<String, Sqlizable> row : results3) {
                int catId = Integer.parseInt(row.get("category_id").toString());
                double priceVal = Double.parseDouble(row.get("price").toString());
                int statusVal = Integer.parseInt(row.get("status").toString());
                
                assertEquals("category_id should be 1", 1, catId);
                assertTrue("price should be >= 50", priceVal >= 50.0);
                assertTrue("price should be <= 250", priceVal <= 250.0);
                assertEquals("status should be 1", 1, statusVal);
            }
            
            Filter filter4 = FilterBuilder.key("category_id").isIn(1, 2);
            
            ArrayList<HashMap<String, Sqlizable>> results4 = ob.scalarVectorSearch()
                .table(tb_name)
                .queryVector(generateRandomVector(64))
                .filter(filter4)
                .topk(10)
                .outputFields("category_id")
                .search();
            
            assertNotNull("Results should not be null", results4);
            
            for (HashMap<String, Sqlizable> row : results4) {
                int catId = Integer.parseInt(row.get("category_id").toString());
                assertTrue("category_id should be 1 or 2", catId == 1 || catId == 2);
            }
            
            List<Integer> categoryList = Arrays.asList(1, 3);
            Filter filter5 = FilterBuilder.key("category_id").isIn(categoryList);
            
            ArrayList<HashMap<String, Sqlizable>> results5 = ob.scalarVectorSearch()
                .table(tb_name)
                .queryVector(generateRandomVector(64))
                .filter(filter5)
                .topk(10)
                .outputFields("category_id")
                .search();
            
            assertNotNull("Results should not be null", results5);
            
            Filter filter6 = FilterBuilder.and(
                FilterBuilder.key("price").isGreaterThan(100.0),
                FilterBuilder.key("price").isLessThan(300.0)
            );
            
            ArrayList<HashMap<String, Sqlizable>> results6 = ob.scalarVectorSearch()
                .table(tb_name)
                .queryVector(generateRandomVector(64))
                .filter(filter6)
                .topk(10)
                .outputFields("price")
                .search();
            
            assertNotNull("Results should not be null", results6);
            
            for (HashMap<String, Sqlizable> row : results6) {
                double priceVal = Double.parseDouble(row.get("price").toString());
                assertTrue("price should be > 100", priceVal > 100.0);
                assertTrue("price should be < 300", priceVal < 300.0);
            }
            
            ArrayList<HashMap<String, Sqlizable>> results7 = ob.scalarVectorSearch()
                .table(tb_name)
                .queryVector(generateRandomVector(64))
                .filter("category_id = 1 AND price >= 100 AND price <= 200")
                .topk(10)
                .outputFields("price", "category_id")
                .search();
            
            assertNotNull("Results should not be null", results7);
            
            for (HashMap<String, Sqlizable> row : results7) {
                int catId = Integer.parseInt(row.get("category_id").toString());
                double priceVal = Double.parseDouble(row.get("price").toString());
                assertEquals("category_id should be 1", 1, catId);
                assertTrue("price should be >= 100", priceVal >= 100.0);
                assertTrue("price should be <= 200", priceVal <= 200.0);
            }
            
        } catch (Throwable e) {
            e.printStackTrace();
            fail("Test failed with exception: " + e.getMessage());
        }
    }

    /**
     * Test Filter API with hybrid text vector search
     */
    public void testFilterAPIWithTextVectorSearch() {
        try {
            Class.forName("com.oceanbase.jdbc.Driver");
            
            String tb_name = "HYBRID_TEXT_VECTOR_FILTER_TEST";
            ObVecClient ob = new ObVecClient(URI, USER, PASSWORD);
            
            // Drop existing table
            if (ob.hasCollection(tb_name)) {
                ob.dropCollection(tb_name);
            }
            
            // Create table schema
            ObCollectionSchema collectionSchema = new ObCollectionSchema();
            
            // Vector field (128 dimensions)
            ObFieldSchema vec_field = new ObFieldSchema("embedding", DataType.FLOAT_VECTOR);
            vec_field.Dim(128).IsNullable(false);
            collectionSchema.addField(vec_field);
            
            // Text fields for full-text search
            ObFieldSchema title_field = new ObFieldSchema("title", DataType.STRING);
            title_field.IsNullable(false);
            collectionSchema.addField(title_field);
            
            // Scalar field for filtering
            ObFieldSchema category_field = new ObFieldSchema("category_id", DataType.INT32);
            category_field.IsNullable(false);
            collectionSchema.addField(category_field);
            
            ObFieldSchema price_field = new ObFieldSchema("price", DataType.DOUBLE);
            price_field.IsNullable(false);
            collectionSchema.addField(price_field);
            
            // Create vector index
            IndexParams index_params = new IndexParams();
            IndexParam index_param = new IndexParam("vidx_embedding", "embedding");
            index_params.addIndex(index_param);
            collectionSchema.setIndexParams(index_params);
            
            ob.createCollection(tb_name, collectionSchema);
            
            // Create full-text indexes
            try {
                ob.createFulltextIndex(tb_name, "ft_title", "title");
            } catch (Throwable e) {
                // Ignore if index creation fails
            }
            
            // Insert test data
            ArrayList<Sqlizable[]> insert_rows = new ArrayList<>();
            
            Sqlizable[] row1 = {
                new SqlVector(generateRandomVector(128)),
                new SqlText("OceanBase Database Introduction"),
                new SqlInteger(1),
                new SqlDouble(100.0)
            };
            insert_rows.add(row1);
            
            Sqlizable[] row2 = {
                new SqlVector(generateRandomVector(128)),
                new SqlText("Vector Search Technology"),
                new SqlInteger(1),
                new SqlDouble(200.0)
            };
            insert_rows.add(row2);
            
            Sqlizable[] row3 = {
                new SqlVector(generateRandomVector(128)),
                new SqlText("Hybrid Search in OceanBase"),
                new SqlInteger(2),
                new SqlDouble(150.0)
            };
            insert_rows.add(row3);
            
            Sqlizable[] row4 = {
                new SqlVector(generateRandomVector(128)),
                new SqlText("Database Management System"),
                new SqlInteger(1),
                new SqlDouble(300.0)
            };
            insert_rows.add(row4);
            
            ob.insert(tb_name, new String[] {"embedding", "title", "category_id", "price"}, insert_rows);
            
            // Wait for index building
            Thread.sleep(3000);
            
            // Test: Filter with text vector search
            float[] queryVector = generateRandomVector(128);
            
            Filter filter = FilterBuilder.and(
                FilterBuilder.key("category_id").isEqualTo(1),
                FilterBuilder.key("price").isLessThanOrEqualTo(250.0)
            );
            
            ArrayList<HashMap<String, Sqlizable>> results = ob.textVectorSearch()
                .table(tb_name)
                .queryVector(queryVector)
                .textFields("title")
                .textQuery("OceanBase database")
                .filter(filter)
                .topk(10)
                .outputFields("title", "category_id", "price")
                .search();
            
            assertNotNull("Results should not be null", results);
            
            for (HashMap<String, Sqlizable> row : results) {
                int catId = Integer.parseInt(row.get("category_id").toString());
                double priceVal = Double.parseDouble(row.get("price").toString());
                assertEquals("category_id should be 1", 1, catId);
                assertTrue("price should be <= 250", priceVal <= 250.0);
            }
            
        } catch (Throwable e) {
            e.printStackTrace();
            fail("Test failed with exception: " + e.getMessage());
        }
    }

    /**
     * Test original API methods for backward compatibility
     */
    public void testOriginalHybridSearchAPI() {
        try {
            Class.forName("com.oceanbase.jdbc.Driver");
            
            String tb_name = "HYBRID_TEXT_VECTOR_TEST_ORIGINAL";
            ObVecClient ob = new ObVecClient(URI, USER, PASSWORD);
            
            // Drop existing table
            if (ob.hasCollection(tb_name)) {
                ob.dropCollection(tb_name);
            }
            
            // Create table schema
            ObCollectionSchema collectionSchema = new ObCollectionSchema();
            
            ObFieldSchema vec_field = new ObFieldSchema("embedding", DataType.FLOAT_VECTOR);
            vec_field.Dim(128).IsNullable(false);
            collectionSchema.addField(vec_field);
            
            ObFieldSchema title_field = new ObFieldSchema("title", DataType.STRING);
            title_field.IsNullable(false);
            collectionSchema.addField(title_field);
            
            IndexParams index_params = new IndexParams();
            IndexParam index_param = new IndexParam("vidx_embedding", "embedding");
            index_params.addIndex(index_param);
            collectionSchema.setIndexParams(index_params);
            
            ob.createCollection(tb_name, collectionSchema);
            
            // Create full-text index
            try {
                ob.createFulltextIndex(tb_name, "ft_title", "title");
            } catch (Throwable e) {
                // Ignore if index already exists
            }
            
            // Insert test data
            ArrayList<Sqlizable[]> insert_rows = new ArrayList<>();
            Sqlizable[] row1 = {
                new SqlVector(generateRandomVector(128)),
                new SqlText("OceanBase Database")
            };
            insert_rows.add(row1);
            ob.insert(tb_name, new String[] {"embedding", "title"}, insert_rows);
            
            Thread.sleep(3000);
            
            // Test original API
            float[] queryVector = generateRandomVector(128);
            
            ArrayList<HashMap<String, Sqlizable>> results = ob.hybridTextVectorSearch(
                tb_name,
                "embedding",
                "cosine",
                queryVector,
                new String[] {"title"},
                "OceanBase",
                null,  // filter_expr
                3,
                new String[] {"title"},
                new DataType[] {DataType.STRING},
                null
            );
            
            assertNotNull("Results should not be null", results);
            
        } catch (Throwable e) {
            e.printStackTrace();
            fail("Test failed with exception: " + e.getMessage());
        }
    }

    /**
     * Quick start example: Full-text + Vector Hybrid Search
     */
    public void testQuickStartFullTextVectorSearch() {
        try {
            Class.forName("com.oceanbase.jdbc.Driver");
            
            String tb_name = "HYBRID_TEXT_VECTOR_QUICKSTART";
            ObVecClient ob = new ObVecClient(URI, USER, PASSWORD);
            
            // Drop existing table
            if (ob.hasCollection(tb_name)) {
                ob.dropCollection(tb_name);
            }
            
            // Create table schema
            ObCollectionSchema collectionSchema = new ObCollectionSchema();
            
            // Vector field (128 dimensions)
            ObFieldSchema vec_field = new ObFieldSchema("embedding", DataType.FLOAT_VECTOR);
            vec_field.Dim(128).IsNullable(false);
            collectionSchema.addField(vec_field);
            
            // Text fields
            ObFieldSchema title_field = new ObFieldSchema("title", DataType.STRING);
            title_field.IsNullable(false);
            collectionSchema.addField(title_field);
            
            ObFieldSchema content_field = new ObFieldSchema("content", DataType.STRING);
            content_field.IsNullable(true);
            collectionSchema.addField(content_field);
            
            // Create vector index
            IndexParams index_params = new IndexParams();
            IndexParam index_param = new IndexParam("vidx_embedding", "embedding");
            index_params.addIndex(index_param);
            collectionSchema.setIndexParams(index_params);
            
            ob.createCollection(tb_name, collectionSchema);
            
            // Create full-text indexes
            try {
                ob.createFulltextIndex(tb_name, "ft_title", "title");
                ob.createFulltextIndex(tb_name, "ft_content", "content");
            } catch (Throwable e) {
                // Ignore if index creation fails
            }
            
            // Insert test data
            ArrayList<Sqlizable[]> insert_rows = new ArrayList<>();
            
            Sqlizable[] row1 = {
                new SqlVector(generateRandomVector(128)),
                new SqlText("OceanBase Database Introduction"),
                new SqlText("OceanBase is a distributed relational database system that supports vector search")
            };
            insert_rows.add(row1);
            
            Sqlizable[] row2 = {
                new SqlVector(generateRandomVector(128)),
                new SqlText("Vector Search Technology"),
                new SqlText("Vector search enables semantic similarity search in databases using embedding vectors")
            };
            insert_rows.add(row2);
            
            Sqlizable[] row3 = {
                new SqlVector(generateRandomVector(128)),
                new SqlText("Hybrid Search in OceanBase"),
                new SqlText("Hybrid search combines full-text search and vector search for better results")
            };
            insert_rows.add(row3);
            
            ob.insert(tb_name, new String[] {"embedding", "title", "content"}, insert_rows);
            
            // Wait for index building
            Thread.sleep(3000);
            
            float[] queryVector = generateRandomVector(128);
            
            ArrayList<HashMap<String, Sqlizable>> results = ob.textVectorSearch()
                .table(tb_name)
                .queryVector(queryVector)
                .textFields("title", "content")
                .textQuery("OceanBase database")
                .metric("cosine")
                .topk(3)
                .search();
            
            assertNotNull("Results should not be null", results);
            assertTrue("Should return results", results.size() > 0);
            
            for (HashMap<String, Sqlizable> row : results) {
                assertNotNull("title should not be null", row.get("title"));
                assertNotNull("content should not be null", row.get("content"));
            }
            
        } catch (Throwable e) {
            e.printStackTrace();
            fail("Test failed with exception: " + e.getMessage());
        }
    }

    /**
     * Quick start example: Scalar + Vector Hybrid Search
     */
    public void testQuickStartScalarVectorSearch() {
        try {
            Class.forName("com.oceanbase.jdbc.Driver");
            
            String tb_name = "HYBRID_SCALAR_VECTOR_QUICKSTART";
            ObVecClient ob = new ObVecClient(URI, USER, PASSWORD);
            
            // Drop existing table
            if (ob.hasCollection(tb_name)) {
                ob.dropCollection(tb_name);
            }
            
            // Create table schema
            ObCollectionSchema collectionSchema = new ObCollectionSchema();
            
            // Vector field (64 dimensions)
            ObFieldSchema vec_field = new ObFieldSchema("embedding", DataType.FLOAT_VECTOR);
            vec_field.Dim(64).IsNullable(false);
            collectionSchema.addField(vec_field);
            
            // Scalar fields
            ObFieldSchema price_field = new ObFieldSchema("price", DataType.DOUBLE);
            price_field.IsNullable(false);
            collectionSchema.addField(price_field);
            
            ObFieldSchema category_field = new ObFieldSchema("category_id", DataType.INT32);
            category_field.IsNullable(false);
            collectionSchema.addField(category_field);
            
            ObFieldSchema status_field = new ObFieldSchema("status", DataType.INT32);
            status_field.IsNullable(false);
            collectionSchema.addField(status_field);
            
            // Create vector index
            IndexParams index_params = new IndexParams();
            IndexParam index_param = new IndexParam("vidx_embedding", "embedding");
            index_params.addIndex(index_param);
            collectionSchema.setIndexParams(index_params);
            
            ob.createCollection(tb_name, collectionSchema);
            
            // Insert test data
            ArrayList<Sqlizable[]> insert_rows = new ArrayList<>();
            
            Sqlizable[] row1 = {
                new SqlVector(generateRandomVector(64)),
                new SqlDouble(100.0),
                new SqlInteger(1),
                new SqlInteger(1)
            };
            insert_rows.add(row1);
            
            Sqlizable[] row2 = {
                new SqlVector(generateRandomVector(64)),
                new SqlDouble(200.0),
                new SqlInteger(1),
                new SqlInteger(1)
            };
            insert_rows.add(row2);
            
            Sqlizable[] row3 = {
                new SqlVector(generateRandomVector(64)),
                new SqlDouble(150.0),
                new SqlInteger(2),
                new SqlInteger(1)
            };
            insert_rows.add(row3);
            
            Sqlizable[] row4 = {
                new SqlVector(generateRandomVector(64)),
                new SqlDouble(300.0),
                new SqlInteger(1),
                new SqlInteger(0)
            };
            insert_rows.add(row4);
            
            Sqlizable[] row5 = {
                new SqlVector(generateRandomVector(64)),
                new SqlDouble(80.0),
                new SqlInteger(1),
                new SqlInteger(1)
            };
            insert_rows.add(row5);
            
            ob.insert(tb_name, new String[] {"embedding", "price", "category_id", "status"}, insert_rows);
            
            // Wait for index building
            Thread.sleep(3000);
            
            float[] queryVector = generateRandomVector(64);
            
            ArrayList<HashMap<String, Sqlizable>> results = ob.scalarVectorSearch()
                .table(tb_name)
                .queryVector(queryVector)
                .filter("category_id = 1 AND price >= 50 AND price <= 250 AND status = 1")
                .metric("l2")
                .topk(5)
                .outputFields("price", "category_id", "status")
                .search();
            
            assertNotNull("Results should not be null", results);
            assertTrue("Should return results", results.size() > 0);
            
            boolean allValid = true;
            for (HashMap<String, Sqlizable> row : results) {
                int categoryId = Integer.parseInt(row.get("category_id").toString());
                double price = Double.parseDouble(row.get("price").toString());
                int status = Integer.parseInt(row.get("status").toString());
                
                if (categoryId != 1 || price < 50 || price > 250 || status != 1) {
                    allValid = false;
                    break;
                }
            }
            assertTrue("All results should satisfy filter conditions", allValid);
            
        } catch (Throwable e) {
            e.printStackTrace();
            fail("Test failed with exception: " + e.getMessage());
        }
    }

    /**
     * Generate random vector for testing
     */
    private float[] generateRandomVector(int dim) {
        float[] vec = new float[dim];
        for (int i = 0; i < dim; i++) {
            vec[i] = (float) Math.random();
        }
        return vec;
    }
}
