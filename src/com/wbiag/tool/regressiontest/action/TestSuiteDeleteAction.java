package com.wbiag.tool.regressiontest.action;

import javax.servlet.ServletRequest;

import com.wbiag.tool.regressiontest.access.TestSuiteAccess;
import com.wbiag.tool.regressiontest.model.TestSuiteData;
import com.workbrain.app.jsp.ActionContext;
import com.workbrain.sql.DBConnection;
import com.workbrain.util.NestedRuntimeException;

/**
 * @author bviveiros
 *
 * Deletes a Test Suite.
 * 
 */
public class TestSuiteDeleteAction extends TestSuiteAction {

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
		String result = ForwardTestSuite.DELETE_TEST_SUITE_SUCCEED;
		
		// Delete the Test Suite.
		TestSuiteAccess tsAccess = new TestSuiteAccess(conn);
		
		try {
			tsAccess.deleteTestSuite(testSuite.getSuiteId());
		} catch (Exception e) {
			throw new NestedRuntimeException("Exception deleting Test Suite: ", e);
		}
		
		return result;
	}

}
