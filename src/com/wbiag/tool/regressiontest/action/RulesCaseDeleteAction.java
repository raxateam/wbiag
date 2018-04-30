package com.wbiag.tool.regressiontest.action;

import javax.servlet.ServletRequest;

import com.wbiag.tool.regressiontest.access.RulesCaseAccess;
import com.workbrain.app.jsp.ActionContext;
import com.workbrain.sql.DBConnection;

/**
 * @author bviveiros
 *
 * Deletes a Pay Rules Test Case.
 * 
 */
public class RulesCaseDeleteAction extends RulesCaseAction {

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

		String result = ForwardRulesCase.DELETE_TEST_CASE_SUCCEED;
		ServletRequest request = actionContext.getRequest();
		String caseIdList = request.getParameter("testCaseId");
		
		// Delete the Test Case.
		RulesCaseAccess tcAccess = new RulesCaseAccess(conn);
		
		try {
			tcAccess.deleteTestCaseList(caseIdList);
		} catch (Exception e) {
			throw e;
		}
		
		return result;
	}

}
