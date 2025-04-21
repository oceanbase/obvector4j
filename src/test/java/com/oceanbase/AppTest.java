package com.oceanbase;

import java.util.logging.Level;

import com.oceanbase.obvec_jdbc.ObVecJsonClient;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Unit test for simple App.
 */
public class AppTest {
    
    /**
     * Initialize OceanBase container before all tests
     */
    @BeforeClass
    public static void setUpClass() throws Throwable {
        Class.forName("com.oceanbase.jdbc.Driver");
        OceanBaseContainerTestBase.initContainer();
    }
    
    /**
     * Clean up OceanBase container after all tests
     */
    @AfterClass
    public static void tearDownClass() throws Throwable {
        OceanBaseContainerTestBase.stopContainer();
    }

    /**
     * Rigourous Test :-)
     */
    @Test
    public void testApp() throws Throwable {
        String uri = OceanBaseContainerTestBase.getJdbcUrl();
        String user = OceanBaseContainerTestBase.getUsername();
        String password = OceanBaseContainerTestBase.getPassword();
        try {
            ObVecJsonClient client = new ObVecJsonClient(uri, user, password, "0", Level.INFO, false);
            client.reset();
            
            String sql = "create table `t2` (c1 int NOT NULL DEFAULT 10, c2 varchar(30) DEFAULT 'ca', c3 varchar not null, c4 decimal(10, 2), c5 timestamp default current_timestamp);";
            client.parseJsonTableSQL2NormalSQL(sql);

            sql = "ALTER TABLE t2 CHANGE COLUMN c2 changed_col INT";
            client.parseJsonTableSQL2NormalSQL(sql);

            sql = "ALTER TABLE t2 DROP c1";
            client.parseJsonTableSQL2NormalSQL(sql);

            sql = "ALTER TABLE t2 MODIFY COLUMN changed_col TIMESTAMP NULL DEFAULT current_timestamp";
            client.parseJsonTableSQL2NormalSQL(sql);

            sql = "ALTER TABLE t2 ADD COLUMN email VARCHAR(100) default 'example@example.com'";
            client.parseJsonTableSQL2NormalSQL(sql);

            sql = "ALTER TABLE t2 ALTER email DROP DEFAULT";
            client.parseJsonTableSQL2NormalSQL(sql);

            sql = "ALTER TABLE t2 ADD COLUMN email2 VARCHAR(100)";
            client.parseJsonTableSQL2NormalSQL(sql);

            sql = "ALTER TABLE t2 RENAME TO alter_test";
            client.parseJsonTableSQL2NormalSQL(sql);

            client.parseJsonTableSQL2NormalSQL(
                "DROP TABLE IF EXISTS t2"
            );
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
