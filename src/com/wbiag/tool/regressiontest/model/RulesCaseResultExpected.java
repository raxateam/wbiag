package com.wbiag.tool.regressiontest.model;

import com.workbrain.sql.DBConnection;

/**
 * @author bviveiros
 *
 * Contains a XML String representation of the data for the expected result.
 */
public class RulesCaseResultExpected extends RulesCaseResult implements ITestCaseResultExpected {

	private String expectedResult = null;
	
	/**
	 * @param testCase
	 */
	public RulesCaseResultExpected(DBConnection conn) throws Exception {
		super();
		this.conn = conn;
	}
	
	public RulesCaseResultExpected(RulesCaseData testCase, DBConnection conn) throws Exception {
		this.conn = conn;
		setTestCase(testCase);
	}

	public void setTestCase(RulesCaseData testCase) throws Exception {
		
		this.testCase = testCase;
		
		// If the expected result has not been retrieved, then retrieve it.
		if (this.testCase.getCaseExpectResult() != null) {
			expectedResult = this.testCase.getCaseExpectResult();
		} else {
			expectedResult = retrieveResult();
		}
	}
	
	public String getExpectedResult() {
		return expectedResult;
	}
}
