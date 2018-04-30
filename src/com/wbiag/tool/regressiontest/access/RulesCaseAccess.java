package com.wbiag.tool.regressiontest.access;

import java.io.IOException;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.wbiag.tool.regressiontest.model.RulesCaseData;
import com.wbiag.tool.regressiontest.model.ITestCaseData;
import com.workbrain.app.ta.db.RecordAccess;
import com.workbrain.sql.DBConnection;
import com.workbrain.util.NestedRuntimeException;
import com.workbrain.util.StringHelper;

/**
 * @author bviveiros
 *
 * Access class for the WBIAG_RULES_CASE table.
 */
public class RulesCaseAccess extends RecordAccess implements ITestCaseAccess {

    private static final String COL_EMP_NAME = "EMP_NAME";
	private static final String COL_CASE_START_DATE = "CASE_START_DATE";
	private static final String COL_CASE_OUTPUT_ATTRIB = "CASE_OUTPUT_ATTRIB";
	private static final String COL_CASE_NAME = "CASE_NAME";
	private static final String COL_CASE_LAST_UPDATED_DATE = "CASE_UPDATED_DATE";
	private static final String COL_CASE_ID = "CASE_ID";
	private static final String COL_CASE_EXPECTED_RESULTS = "CASE_EXPECT_RESULT";
	private static final String COL_CASE_END_DATE = "CASE_END_DATE";
	private static final String COL_CASE_DESCRIPTION = "CASE_DESC";
	private static final String COL_CASE_CREATED_DATE = "CASE_CREATED_DATE";
	private static final String COL_EMP_ID = "EMP_ID";
	private static final String COL_TEST_SUITE_ID = "SUITE_ID";
    
	public static final String TEST_CASE_TABLE = "WBIAG_RULES_CASE";
    public static final String TEST_CASE_PRI_KEY = COL_CASE_ID;
    public static final String TEST_CASE_SEQ = "SEQ_CASE_ID";

    
    /**
	 * @param c
	 */
	public RulesCaseAccess(DBConnection c) {
		super(c);
	}

	/* (non-Javadoc)
	 * @see com.wbiag.tool.regressiontest.access.ITestCaseAccess#getTestCase(int)
	 */
	public ITestCaseData getTestCase(int testCaseId) throws SQLException {

		RulesCaseData testCase = null;
		String sql = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;

		// Note: Cannot use the RecordAccess.loadRecordData because
		// we need to join with EMPLOYEE table to get the EMP_NAME.
		
		sql = "SELECT tc.*, e.EMP_NAME"
			+ " FROM " + TEST_CASE_TABLE + " tc, EMPLOYEE e"
			+ " where tc.EMP_ID = e.EMP_ID"
			+ " and tc." + TEST_CASE_PRI_KEY + "=?";

		try {
			pstmt = getDBConnection().prepareStatement(sql);
			
			pstmt.setInt(1, testCaseId);
			
			rs = pstmt.executeQuery();
			
			testCase = getTestCaseData(rs);
			
		} catch (SQLException e) {
			throw e;
			
		} finally {
			if (rs != null) {
				rs.close();
			}
			if (pstmt != null) {
				pstmt.close();
			}
		}
		
        return testCase;
	}

	/* (non-Javadoc)
	 * @see com.wbiag.tool.regressiontest.access.ITestCaseAccess#addTestCase(com.wbiag.tool.regressiontest.model.TestCaseData)
	 */
	public void addTestCase(ITestCaseData testCase) throws SQLException, IOException {
	
		RulesCaseData rulesTestCase = (RulesCaseData)testCase;
		
		rulesTestCase.setCaseId( getDBConnection().getDBSequence( TEST_CASE_SEQ ).getNextValue() );
        
		// Ensure there is a Test Suite name and convert it to UPPERCASE.
		if (!StringHelper.isEmpty(rulesTestCase.getCaseName())) {
            rulesTestCase.setCaseName(rulesTestCase.getCaseName().toUpperCase());
        } else {
            throw new NestedRuntimeException ("Test Case Name cannot be empty.");
        }
        
		// Set the Create and Updated date to now.
        Date now = new Date();
        rulesTestCase.setCaseCreatedDate(now);
        rulesTestCase.setCaseUpdatedDate(now);
        
 		// Need to update any CLOB columns seperately.  If it is larger than 32K, it will get truncated
		// by the PreparedStatement.setString() method.  Use the DBConnection.updateClob() method instead.
		String clobExpectResult = rulesTestCase.getCaseExpectResult();
		String clobOutputAttrib = rulesTestCase.getCaseOutputAttrib();
		rulesTestCase.setCaseExpectResult("TEMP");
		rulesTestCase.setCaseOutputAttrib("TEMP");
		
		// Insert the record.
        insertRecordData( rulesTestCase, TEST_CASE_TABLE );
        
        // Update the CLOB columns.
		rulesTestCase.setCaseExpectResult(clobExpectResult);
        getDBConnection().updateClob(clobExpectResult, TEST_CASE_TABLE, COL_CASE_EXPECTED_RESULTS,
        								TEST_CASE_PRI_KEY, String.valueOf(rulesTestCase.getCaseId()));

        rulesTestCase.setCaseOutputAttrib(clobOutputAttrib);
        getDBConnection().updateClob(clobOutputAttrib, TEST_CASE_TABLE, COL_CASE_OUTPUT_ATTRIB,
        								TEST_CASE_PRI_KEY, String.valueOf(rulesTestCase.getCaseId()));

	}

	/* (non-Javadoc)
	 * @see com.wbiag.tool.regressiontest.access.ITestCaseAccess#updateTestCase(com.wbiag.tool.regressiontest.model.ITestCaseData)
	 */
	public void updateTestCase(ITestCaseData testCase) throws SQLException, IOException {

		RulesCaseData rulesTestCase = (RulesCaseData)testCase;
		
		// Set the Updated date to now.
        Date now = new Date();
        rulesTestCase.setCaseUpdatedDate(now);
        
 		// Need to update any CLOB columns seperately.  If it is larger than 32K, it will get truncated
		// by the PreparedStatement.setString() method.  Use the DBConnection.updateClob() method instead.
		String clobExpectResult = rulesTestCase.getCaseExpectResult();
		String clobOutputAttrib = rulesTestCase.getCaseOutputAttrib();
		rulesTestCase.setCaseExpectResult("TEMP");
		rulesTestCase.setCaseOutputAttrib("TEMP");

		// Update the record.
        updateRecordData(rulesTestCase, TEST_CASE_TABLE, TEST_CASE_PRI_KEY);

        // Update the CLOB columns.
		rulesTestCase.setCaseExpectResult(clobExpectResult);
        getDBConnection().updateClob(clobExpectResult, TEST_CASE_TABLE, COL_CASE_EXPECTED_RESULTS,
        								TEST_CASE_PRI_KEY, String.valueOf(rulesTestCase.getCaseId()));

        rulesTestCase.setCaseOutputAttrib(clobOutputAttrib);
        getDBConnection().updateClob(clobOutputAttrib, TEST_CASE_TABLE, COL_CASE_OUTPUT_ATTRIB,
        								TEST_CASE_PRI_KEY, String.valueOf(rulesTestCase.getCaseId()));
	}

	/* (non-Javadoc)
	 * @see com.wbiag.tool.regressiontest.access.ITestCaseAccess#deleteTestase(int)
	 */
	public void deleteTestCase(int testCaseId) throws SQLException {
		deleteRecordData(TEST_CASE_TABLE, TEST_CASE_PRI_KEY, testCaseId);
	}

	/* (non-Javadoc)
	 * @see com.wbiag.tool.regressiontest.access.ITestCaseAccess#getTestCaseList(int)
	 */
	public List getTestCaseList(int testSuiteId) throws SQLException {

		List testCaseList = null;
		String sql = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;

		// Note: Cannot use the RecordAccess.loadRecordData because
		// we need to join with EMPLOYEE table to get the EMP_NAME.
		
		sql = "SELECT tc.*, e.EMP_NAME"
			+ " FROM " + TEST_CASE_TABLE + " tc, EMPLOYEE e"
			+ " where tc.EMP_ID = e.EMP_ID"
			+ " and tc." + COL_TEST_SUITE_ID + "=?";

		try {
			pstmt = getDBConnection().prepareStatement(sql);
			
			pstmt.setInt(1, testSuiteId);
			
			rs = pstmt.executeQuery();
			
			testCaseList = getTestCaseDataList(rs);
			
		} catch (SQLException e) {
			throw e;
			
		} finally {
			if (rs != null) {
				rs.close();
			}
			if (pstmt != null) {
				pstmt.close();
			}
		}
		
        return testCaseList;
	}

	/* (non-Javadoc)
	 * @see com.wbiag.tool.regressiontest.access.ITestCaseAccess#deleteTestCaseList(java.lang.String)
	 */
	public void deleteTestCaseList(String idList) throws SQLException {

		String sql = "delete from " + TEST_CASE_TABLE 
					+ " where " + TEST_CASE_PRI_KEY 
					+ " in ("
					+ idList
					+ ")";

		PreparedStatement stmt = getDBConnection().prepareStatement(sql);
		
		stmt.executeUpdate();
		
		stmt.close();
	}

	private RulesCaseData getTestCaseData(ResultSet rs) throws SQLException {
		
		RulesCaseData testCase = null;
		
		if (rs.next()) {
			testCase = new RulesCaseData();
			populateTestCaseData(testCase, rs);
		}
		
		return testCase;
	}
	
	private List getTestCaseDataList(ResultSet rs) throws SQLException {
	
		List testCaseList = new ArrayList();
		RulesCaseData testCase = null;
		
		while (rs.next()) {
			testCase = new RulesCaseData();
			populateTestCaseData(testCase, rs);
			testCaseList.add(testCase);
		}
		
		if (testCaseList.size() == 0) {
			testCaseList = null;
		}
		
		return testCaseList;
	}
	
	private void populateTestCaseData(RulesCaseData testCase, ResultSet rs) throws SQLException {
				
		testCase.setEmpId(rs.getInt(COL_EMP_ID));
		testCase.setCaseCreatedDate(rs.getTimestamp(COL_CASE_CREATED_DATE));
		testCase.setCaseDesc(rs.getString(COL_CASE_DESCRIPTION));
		testCase.setCaseEndDate(rs.getTimestamp(COL_CASE_END_DATE));
		testCase.setCaseExpectResult(clobToString(rs.getClob(COL_CASE_EXPECTED_RESULTS)));
		testCase.setCaseId(rs.getInt(COL_CASE_ID));
		testCase.setCaseUpdatedDate(rs.getTimestamp(COL_CASE_LAST_UPDATED_DATE));
		testCase.setCaseName(rs.getString(COL_CASE_NAME));
		testCase.setCaseOutputAttrib(clobToString(rs.getClob(COL_CASE_OUTPUT_ATTRIB)));
		testCase.setCaseStartDate(rs.getTimestamp(COL_CASE_START_DATE));
		testCase.setSuiteId(rs.getInt(COL_TEST_SUITE_ID));
		
		testCase.empName(rs.getString(COL_EMP_NAME));
	}
	
	private String clobToString(Clob clob) throws SQLException {
		return clob.getSubString(1, (int)clob.length());
	}

}
