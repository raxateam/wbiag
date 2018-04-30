package com.wbiag.tool.regressiontest.model;

import com.workbrain.sql.DBConnection;

/**
 * @author bviveiros
 *
 * Contains a XML String representation of the data for the actual result. 
 * Also contains the duration of the test case, and an exception if one was
 * thrown during the run of the test case.
 */
public class RulesCaseResultActual extends RulesCaseResult implements ITestCaseResultActual {

	private String actualResult = null;
	private Exception exceptionThrown = null;
	private long executionTimeSeconds = 0;
	
	/**
	 * @param testCase
	 */
	public RulesCaseResultActual(ITestCaseData testCase, DBConnection conn) throws Exception {

		this.conn = conn;
		this.testCase = (RulesCaseData) testCase;
		
		// Retrieve the actual result data.
		actualResult = retrieveResult();
	}

	public String getActualResult() {
		return actualResult;
	}


	/* (non-Javadoc)
	 * @see com.wbiag.tool.regressiontest.model.ITestCaseActualResult#setExceptionThrown(java.lang.Exception)
	 */
	public void setExceptionThrown(Exception exceptionThrown) {
		this.exceptionThrown = exceptionThrown;
	}

	/* (non-Javadoc)
	 * @see com.wbiag.tool.regressiontest.model.ITestCaseActualResult#getExceptionThrown()
	 */
	public Exception getExceptionThrown() {
		return this.exceptionThrown;
	}

	/* (non-Javadoc)
	 * @see com.wbiag.tool.regressiontest.model.ITestCaseActualResult#setExecutionTime(long)
	 */
	public void setExecutionTime(long executionTimeSeconds) {
		this.executionTimeSeconds = executionTimeSeconds;
	}

	/* (non-Javadoc)
	 * @see com.wbiag.tool.regressiontest.model.ITestCaseActualResult#getExecutionTime()
	 */
	public long getExecutionTime() {
		return this.executionTimeSeconds;
	}
}
