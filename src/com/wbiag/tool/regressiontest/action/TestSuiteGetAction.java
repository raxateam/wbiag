package com.wbiag.tool.regressiontest.action;

import java.util.List;

import javax.servlet.ServletRequest;

import com.wbiag.tool.regressiontest.access.ITestCaseAccess;
import com.wbiag.tool.regressiontest.access.TestCaseAccessFactory;
import com.wbiag.tool.regressiontest.access.TestSuiteAccess;
import com.wbiag.tool.regressiontest.model.TestSuiteData;
import com.wbiag.tool.regressiontest.util.RequestHelper;
import com.workbrain.app.jsp.ActionContext;
import com.workbrain.sql.DBConnection;
import com.workbrain.util.NestedRuntimeException;

/**
 * @author bviveiros
 *
 * Retrieves a Test Suite and Test Case List and stores them
 * in the request.
 * 
 */
public class TestSuiteGetAction extends TestSuiteAction {

	/* (non-Javadoc)
	 * @see com.workbrain.app.jsp.Action#createRequest(javax.servlet.ServletRequest, com.workbrain.sql.DBConnection)
	 */
	public Object createRequest(ServletRequest request, DBConnection conn)
			throws Exception {

		TestSuiteData testSuite = new TestSuiteData();
		populateTestSuiteFromRequest(request, testSuite);
		return testSuite;
	}

	/* (non-Javadoc)
	 * @see com.workbrain.app.jsp.Action#process(com.workbrain.sql.DBConnection, com.workbrain.app.jsp.ActionContext, java.lang.Object)
	 */
	public String process(DBConnection conn, ActionContext actionContext, Object formBean)
			throws Exception {

		TestSuiteData testSuite = (TestSuiteData) formBean;
		int testSuiteId = testSuite.getSuiteId();
		String result = ForwardTestSuite.GET_TEST_SUITE_SUCCEED;
		ServletRequest request = actionContext.getRequest();
		List testCaseList = null;
		
		// Retrieve the Test Suite.
		testSuite = getTestSuite(conn, testSuiteId);
		
		// Retrieve the Test Case List
		testCaseList = getTestCaseList(conn, testSuite);
		
		// Store the Test Suite bean.
		RequestHelper.setTestSuiteBean(request, testSuite);
		
		// Store the Test Case list.
		RequestHelper.setTestCaseList(request, testCaseList);
		
		return result;
	}

	/*
	 * Get the Test Suite List for the Test Case.  Returns a List of ITestCaseData
	 * 
	 * @param conn
	 * @param testSuite
	 * @param testCaseList
	 * @return
	 * @throws Exception
	 */
	private List getTestCaseList(DBConnection conn, TestSuiteData testSuite) throws Exception {

		ITestCaseAccess caseAccess = TestCaseAccessFactory.getInstance(testSuite.getSuiteType(), conn);
		List testCaseList = null;
		
		try {
			testCaseList = caseAccess.getTestCaseList(testSuite.getSuiteId());
		} catch (Exception e) {
			throw new NestedRuntimeException("Exception retrieving Test Case List: ", e);
		}
		return testCaseList;
	}

	/*
	 * Get the Test Suite with the given Id.
	 * 
	 * @param conn
	 * @param testSuite
	 * @param testSuiteId
	 * @return
	 * @throws Exception
	 */
	private TestSuiteData getTestSuite(DBConnection conn, int testSuiteId) throws Exception {

		TestSuiteAccess tsAccess = new TestSuiteAccess(conn);
		TestSuiteData testSuite = null;
		
		try {
			testSuite = tsAccess.getTestSuite(testSuiteId);
		} catch (Exception e) {
			throw new NestedRuntimeException("Exception retrieving Test Suite: ", e);
		}
		return testSuite;
	}

}
