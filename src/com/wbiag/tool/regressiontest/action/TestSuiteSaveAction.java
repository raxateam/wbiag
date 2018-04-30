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
 * Saves the Test Suite.
 *
 */
public class TestSuiteSaveAction extends TestSuiteAction {

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
		String result = ForwardTestSuite.SAVE_TEST_SUITE_SUCCEED;
		
		// Save the Test Suite.
		TestSuiteAccess tsAccess = new TestSuiteAccess(conn);
		
		try {
			tsAccess.updateTestSuite(testSuite);
		} catch (Exception e) {
			throw new NestedRuntimeException("Exception updating Test Suite.", e);
		}
		
		return result;
	}

}
