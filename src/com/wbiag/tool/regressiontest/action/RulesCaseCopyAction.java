package com.wbiag.tool.regressiontest.action;

import javax.servlet.ServletRequest;

import com.wbiag.tool.regressiontest.access.RulesCaseAccess;
import com.wbiag.tool.regressiontest.model.RulesCaseData;
import com.wbiag.tool.regressiontest.model.RulesCaseResultExpected;
import com.workbrain.app.jsp.ActionContext;
import com.workbrain.sql.DBConnection;
import com.workbrain.util.StringHelper;

/**
 * @author bviveiros
 *
 * Retrieves a Pay Rules Test Case, clears some fields
 *
 */
public class RulesCaseCopyAction extends RulesCaseAction {

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

		RulesCaseData testCase = null;
		String result = ForwardRulesCase.COPY_TEST_CASE_SUCCEED;
		ServletRequest request = actionContext.getRequest();
		int caseId = Integer.parseInt(request.getParameter("testCaseId"));
		String copyToEmployeeIdList = request.getParameter("copyToEmpIdList");
		String copyToEmployeeNameList = request.getParameter("copyToEmpNameList");
		String defaultCaseName = null;
		String defaultCaseDescription = null;
		String empName = null;
		RulesCaseResultExpected expectedResult = null;
		
		// Get the Test Case.
		RulesCaseAccess tcAccess = new RulesCaseAccess(conn);
		
		try {
			testCase = (RulesCaseData) tcAccess.getTestCase(caseId);

			// Reset fields that cannot be copied.
			// The only fields not cleared are dates, and output attributes.
			testCase.setCaseId(0);
			testCase.setCaseExpectResult(null);
			testCase.setCaseCreatedDate(null);
			testCase.setEmpId(0);
			testCase.setCaseDesc(null);
			testCase.setCaseName(null);
	
			expectedResult = new RulesCaseResultExpected(conn);
			
			// For each empId in the list, create a Test Case.
			// Do not use the detokenizeStringIntArray since it is not available in WB4.0
			String[] employeeIds = StringHelper.detokenizeString(copyToEmployeeIdList, ",");
			String[] employeeNames = StringHelper.detokenizeString(copyToEmployeeNameList, ",");
			
			// Only change the empId, caseName, caseDescription, expectedResults.
			for (int i=0; i < employeeIds.length; i++) {
				
				empName = employeeNames[i];
				
				// Create a default Test Case Name.
				defaultCaseName = getDefaultCaseName(empName, 
													testCase.getCaseStartDate(), 
													testCase.getCaseEndDate());

				// Create a default Test Case Description.
				defaultCaseDescription = getDefaultCaseDesc(empName, 
													testCase.getCaseStartDate(), 
													testCase.getCaseEndDate());
				
				// Update the empId.
				// Update the Test Case Name and Description.
				testCase.setEmpId(Integer.parseInt(employeeIds[i]));
				testCase.empName(empName);
				testCase.setCaseName(defaultCaseName);
				testCase.setCaseDesc(defaultCaseDescription);
				testCase.setCaseExpectResult(null);
	
				// Query the expected results.
				expectedResult.setTestCase(testCase);
				testCase.setCaseExpectResult(expectedResult.getExpectedResult());
	
				// Add the Test Case.
				tcAccess.addTestCase(testCase);
			}

		} catch (Exception e) {
			throw e;
		}
		
		return result;
	}

}
