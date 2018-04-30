package com.wbiag.tool.regressiontest.action;


import javax.servlet.ServletRequest;

import com.wbiag.tool.regressiontest.model.TestSuiteData;
import com.wbiag.tool.regressiontest.util.RequestHelper;
import com.workbrain.app.jsp.ActionContext;
import com.workbrain.sql.DBConnection;

/**
 * @author bviveiros
 *
 * Init a Test Suite.  This is used before brining up a blank Test Suite page.
 * 
 */
public class TestSuiteInitAction extends TestSuiteAction {

	/* (non-Javadoc)
	 * @see com.workbrain.app.jsp.Action#createRequest(javax.servlet.ServletRequest, com.workbrain.sql.DBConnection)
	 */
	public Object createRequest(ServletRequest request, DBConnection conn)
			throws Exception {

		return null;
	}

	/* (non-Javadoc)
	 * @see com.workbrain.app.jsp.Action#process(com.workbrain.sql.DBConnection, com.workbrain.app.jsp.ActionContext, java.lang.Object)
	 */
	public String process(DBConnection conn, ActionContext actionContext, Object formBean)
			throws Exception {
		
		String result = ForwardTestSuite.INIT_TEST_SUITE_SUCCEED;
		ServletRequest request = actionContext.getRequest();
		
		// Create an empty Test Suite.
		TestSuiteData testSuite = new TestSuiteData();
		
		// Store the Test Suite bean.
		RequestHelper.setTestSuiteBean(request, testSuite);
		
		return result;
	}
	
}
