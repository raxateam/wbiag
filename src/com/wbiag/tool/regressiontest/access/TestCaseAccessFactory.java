package com.wbiag.tool.regressiontest.access;

import com.wbiag.tool.regressiontest.testengine.TestSuiteTypes;
import com.workbrain.sql.DBConnection;

/**
 * @author bviveiros
 *
 * A Test Suite will contain a list of only one type of Test Case.  
 * Each type of Test Case will have it's own database table and Access
 * class.  The Access class must implement ITestCaseAccess, and have it's
 * constructor in the getInstance() method of TestCaseAccessFactory.  
 */
public class TestCaseAccessFactory {

	/**
	 * For a given Test Suite Type, return an instance of the 
	 * corresponding Access class to perform database functions.  
	 * 
	 * @param testSuiteType
	 * @param conn
	 * @return
	 */
	public static ITestCaseAccess getInstance(String testSuiteType, DBConnection conn) 
	{
		ITestCaseAccess caseAccess = null;
		
		// First release only has a Pay Rules test case type.
		if (TestSuiteTypes.PAY_RULES.equals(testSuiteType)) {
			caseAccess = new RulesCaseAccess(conn);
		}
		
		return caseAccess;
	}
}
