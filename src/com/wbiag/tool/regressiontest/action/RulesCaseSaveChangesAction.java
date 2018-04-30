package com.wbiag.tool.regressiontest.action;

import java.util.Date;

import javax.servlet.ServletRequest;

import com.wbiag.tool.regressiontest.access.RulesCaseAccess;
import com.wbiag.tool.regressiontest.model.RulesCaseData;
import com.wbiag.tool.regressiontest.model.RulesCaseResultExpected;
import com.workbrain.app.jsp.ActionContext;
import com.workbrain.sql.DBConnection;
import com.workbrain.util.DateHelper;
import com.workbrain.util.StringHelper;

/**
 * @author bviveiros
 *
 * Deletes a Pay Rules Test Case.
 * 
 */
public class RulesCaseSaveChangesAction extends RulesCaseAction {
	
	private final String TEST_CASE_ID = "testCaseId";
	private final String TC_NAME = "testCaseName_";
	private final String TC_DESC = "testCaseDesc_";
	private final String TC_EMPLOYEE_NAME = "editEmployee_";
	private final String TC_EMPLOYEE_ID = "editEmployee_";
	private final String TC_START_DATE = "startDate_";
	private final String TC_END_DATE = "endDate_";
	private final String DATE_FORMAT = "yyyyMMdd";
	
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

		String result = ForwardRulesCase.SAVE_CHANGES_TEST_CASE_SUCCEED;
		ServletRequest request = actionContext.getRequest();
		RulesCaseData testCase = null;	
		// Save all changes for each Test Case within the Test Suite.
		RulesCaseAccess tcAccess = null;
		RulesCaseResultExpected expectedResult = null;
		String[] caseIdList = StringHelper.detokenizeString(request.getParameter(TEST_CASE_ID),",");
		String reqTCName,reqTCDesc,reqTCEmpName;
		int reqTCEmpId = -1;
		Date reqTCStartDate,reqTCEndDate;
		boolean updateRequired;
		try {
//			with the list of case ids grab each value
			for(int i=0; i< caseIdList.length; i++){
				updateRequired = false;
				testCase = new RulesCaseData();
				reqTCName = request.getParameter(TC_NAME+caseIdList[i]);
				reqTCDesc = request.getParameter(TC_DESC+caseIdList[i]);
				reqTCEmpName = request.getParameter(TC_EMPLOYEE_NAME+caseIdList[i]+"_label");
				reqTCEmpId = Integer.parseInt(request.getParameter(TC_EMPLOYEE_ID+caseIdList[i]));
				reqTCStartDate = DateHelper.convertStringToDate(request.getParameter(TC_START_DATE+caseIdList[i]),DATE_FORMAT);
				reqTCEndDate = DateHelper.convertStringToDate(request.getParameter(TC_END_DATE+caseIdList[i]),DATE_FORMAT);
				tcAccess = new RulesCaseAccess(conn);
				testCase = (RulesCaseData)tcAccess.getTestCase(Integer.parseInt(caseIdList[i]));
				if(!testCase.getCaseName().equals(reqTCName)){
					testCase.setCaseName(reqTCName);
					updateRequired = true;
				}
				if(!testCase.getCaseDesc().equals(reqTCDesc)){
					testCase.setCaseDesc(reqTCDesc);
					updateRequired = true;
				}
				if(!testCase.empName().equals(reqTCEmpName)){
					testCase.empName(reqTCEmpName);
					testCase.setCaseExpectResult(null); //re-create expected results
					updateRequired = true;
				}
				if(testCase.getEmpId() != reqTCEmpId){
					testCase.setEmpId(reqTCEmpId);
					testCase.setCaseExpectResult(null); //re-create expected results
					updateRequired = true;
				}
				if(testCase.getCaseStartDate().compareTo(reqTCStartDate)!= 0){
					testCase.setCaseStartDate(reqTCStartDate);
					testCase.setCaseExpectResult(null); //re-create expected results
					updateRequired = true;
				}
				if(testCase.getCaseEndDate().compareTo(reqTCEndDate)!= 0){
					testCase.setCaseEndDate(reqTCEndDate);
					testCase.setCaseExpectResult(null); //re-create expected results
					updateRequired = true;
				}
				
				if(updateRequired){					
					expectedResult = new RulesCaseResultExpected(testCase, conn);
					testCase.setCaseExpectResult(expectedResult.getExpectedResult());
					//	call updateTestCase				
					tcAccess.updateTestCase(testCase);
				}
			}
		}catch (Exception e) {
			throw e;
		}
		
		return result;
	}

}
