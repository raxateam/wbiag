package com.wbiag.tool.regressiontest.action;

import javax.servlet.ServletRequest;

import com.wbiag.tool.regressiontest.model.TestSuiteData;
import com.wbiag.tool.regressiontest.testengine.TestSuiteTypes;
import com.workbrain.app.jsp.Action;
import com.workbrain.server.jsp.JSPHelper;
import com.workbrain.util.DateHelper;
import com.workbrain.util.StringHelper;

/**
 * @author bviveiros
 *
 * All TestSuite Action classes should extend this class.
 * 
 */
public abstract class TestSuiteAction implements Action {

	public static final String DATE_TIME_FORMAT = "MM/dd/yyyy HH:mm";

	
	/**
	 * Populate the Test Suite bean with the parameters in the request.
	 * 
	 * @param request
	 * @param testSuite
	 * @return
	 */
	protected void populateTestSuiteFromRequest(ServletRequest request, TestSuiteData testSuite) {
		
		if (testSuite == null) {
			testSuite = new TestSuiteData();
		}
		
		testSuite.setSuiteId(Integer.parseInt(StringHelper.getRequestParam(request, "testSuiteId", "0")));
		testSuite.setSuiteName(request.getParameter("testSuiteName"));
		testSuite.setSuiteDesc(request.getParameter("testSuiteDescription"));
		
		// Get the WBU name from the request.
		if (!StringHelper.isEmpty(request.getParameter("creatorWbuName"))) {
			testSuite.setSuiteCreatorName(request.getParameter("creatorWbuName"));
		} else {
			testSuite.setSuiteCreatorName(JSPHelper.getWebLogin(request).getUserName());
		}

		String strDate = request.getParameter("createdDate");
		
		testSuite.setSuiteCreatedDate(
					com.workbrain.util.StringHelper.isEmpty(strDate) ? null
					: DateHelper.parseDate(strDate, DATE_TIME_FORMAT)
					);

		strDate = request.getParameter("lastRun");
			
		testSuite.setSuiteExecuteDate(
					com.workbrain.util.StringHelper.isEmpty(strDate) ? null
					:DateHelper.parseDate(strDate, DATE_TIME_FORMAT)
					);

		// Test Suite type is PayRules.
		testSuite.setSuiteType(TestSuiteTypes.PAY_RULES);
	}
	
}
