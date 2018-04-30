package com.wbiag.tool.regressiontest.action;

import javax.servlet.ServletRequest;

import com.wbiag.tool.regressiontest.model.TestSuiteData;
import com.wbiag.tool.regressiontest.testengine.TestSuiteEngine;
import com.wbiag.tool.regressiontest.testengine.TestSuiteEngineFactory;
import com.wbiag.tool.regressiontest.util.RequestHelper;

import com.workbrain.app.jsp.ActionContext;
import com.workbrain.sql.DBConnection;
import com.workbrain.util.NestedRuntimeException;

/**
 * @author bviveiros
 *
 * Executes a Test Suite and set the report Id to the request.
 * 
 */
public class TestSuiteRunAction extends TestSuiteAction {

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
		String result = ForwardTestSuite.RUN_TEST_SUITE_SUCCEED;
		ServletRequest request = actionContext.getRequest();
		
		// Get an instance of the Test Engine
		TestSuiteEngine testEngine = TestSuiteEngineFactory.getInstance(testSuite.getSuiteType());
		int reportId = 0;
		
		try {
			// Do we have a full test suite object or just the id.
			if (testSuite.getSuiteCreatedDate() == null) {
				reportId = testEngine.execute(testSuite.getSuiteId(), conn);
			} else {
				reportId = testEngine.execute(testSuite, conn);
			}
			
		} catch (Exception e) {
			throw new NestedRuntimeException("Exception executing Test Suite.", e);
		}
		
		// Save the report Id to be displayed.
		RequestHelper.setReportId(request, reportId);
		
		return result;
	}

}
