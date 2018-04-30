package com.wbiag.tool.regressiontest.action;

import java.util.StringTokenizer;

import javax.servlet.ServletRequest;

import com.wbiag.tool.regressiontest.access.RulesCaseAccess;
import com.wbiag.tool.regressiontest.model.RulesCaseData;
import com.wbiag.tool.regressiontest.model.RulesCaseResultExpected;
import com.workbrain.app.jsp.ActionContext;
import com.workbrain.sql.DBConnection;

/**
 * @author bviveiros
 *
 * Re-generates a snapshot of a Pay Rules Test Case.
 *
 */
public class RulesCaseReCreateAction extends RulesCaseAction {

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

		ServletRequest request = actionContext.getRequest();
		String result = ForwardRulesCase.RECREATE_TEST_CASE_SUCCEED;
		String updateTestCaseIds = request.getParameter("testCaseId");;

		RulesCaseData testCase = null;
		RulesCaseResultExpected expectedResult = null;
		int testCaseId = 0;
		RulesCaseAccess tcAccess = new RulesCaseAccess(conn);

		try {
			// For each test case id.
			StringTokenizer tokenizer = new StringTokenizer(updateTestCaseIds, ",");
			while (tokenizer.hasMoreTokens()) {
			
				testCaseId = Integer.parseInt(tokenizer.nextToken());

				// Query the test case.
				testCase = (RulesCaseData) tcAccess.getTestCase(testCaseId);

				// Clear the expected result so it will be re-generated.
				testCase.setCaseExpectResult(null);
				
				// Query the expected results.
				expectedResult = new RulesCaseResultExpected(conn);
				expectedResult.setTestCase(testCase);
				testCase.setCaseExpectResult(expectedResult.getExpectedResult());
			
				// Update the test case.
				tcAccess.updateTestCase(testCase);
			}

		} catch (Exception e) {
			throw e;
		}
		
		return result;
	}

}
