package com.wbiag.tool.regressiontest.access;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import com.wbiag.tool.regressiontest.model.TestSuiteData;
import com.workbrain.app.ta.db.RecordAccess;
import com.workbrain.sql.DBConnection;
import com.workbrain.util.NestedRuntimeException;
import com.workbrain.util.StringHelper;

/**
 * @author bviveiros
 *
 * Access class for the WBIAG_TST_SUITE table.
 */
public class TestSuiteAccess extends RecordAccess {

    public static final String TEST_SUITE_TABLE = "WBIAG_TST_SUITE";
    public static final String TEST_SUITE_PRI_KEY = "SUITE_ID";
    public static final String TEST_SUITE_SEQ = "SEQ_SUITE_ID";

    /**
	 * @param c
	 */
	public TestSuiteAccess(DBConnection c) {
		super(c);
	}

	/**
	 * Creates a Test Suite.
	 * 
	 * @param testSuite
	 * @throws SQLException
	 */
	public void createTestSuite(TestSuiteData testSuite) throws SQLException {
        
		testSuite.setSuiteId( getDBConnection().getDBSequence( TEST_SUITE_SEQ ).getNextValue() );
        
		// Ensure there is a Test Suite name and convert it to UPPERCASE.
		if (!StringHelper.isEmpty(testSuite.getSuiteName())) {
            testSuite.setSuiteName(testSuite.getSuiteName().toUpperCase());
        } else {
            throw new NestedRuntimeException ("Test Suite Name cannot be empty.");
        }
        
		// Set the Create and Updated date to now.
        Date now = new Date();
        testSuite.setSuiteCreatedDate(now);
        testSuite.setSuiteUpdatedDate(now);
        
        // Insert the record.
        insertRecordData( testSuite, TEST_SUITE_TABLE );
	}

	/**
	 * Updates a Test Suite.
	 * 
	 * @param testSuite
	 */
	public void updateTestSuite(TestSuiteData testSuite) throws SQLException {
		
		testSuite.setSuiteUpdatedDate(new Date());
		updateRecordData( testSuite, TEST_SUITE_TABLE, TEST_SUITE_PRI_KEY );
	}
	
	
	/**
	 * Returns the Test Suite with the given Id.
	 * 
	 * @param testSuiteId
	 * @return
	 */
	public TestSuiteData getTestSuite(int testSuiteId) {
		
        TestSuiteData testSuite = new TestSuiteData();
        
        List records = loadRecordData( testSuite, TEST_SUITE_TABLE, TEST_SUITE_PRI_KEY + " = " + testSuiteId );
        
        if( records.size() > 0 ) {
            return (TestSuiteData) records.get( 0 );
        }

        return null;
	}
	
	
	/**
	 * Deletes the Test Suite with the given Id.
	 * 
	 * @param testSuiteId
	 * @throws SQLException
	 */
	public void deleteTestSuite(int testSuiteId) throws SQLException {
		deleteRecordData(TEST_SUITE_TABLE, TEST_SUITE_PRI_KEY, testSuiteId);
	}
}
