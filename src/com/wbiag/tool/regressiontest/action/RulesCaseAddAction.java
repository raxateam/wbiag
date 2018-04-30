package com.wbiag.tool.regressiontest.action;

import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletRequest;

import com.wbiag.tool.regressiontest.access.RulesCaseAccess;
import com.wbiag.tool.regressiontest.jsp.RulesCaseParamsBean;
import com.wbiag.tool.regressiontest.model.RulesCaseData;
import com.wbiag.tool.regressiontest.model.RulesCaseResultExpected;
import com.wbiag.util.GetEmployeeHelper;
import com.wbiag.util.GetEmployeeParameters;
import com.workbrain.app.jsp.ActionContext;
import com.workbrain.app.ta.model.EmployeeData;
import com.workbrain.server.jsp.JSPHelper;
import com.workbrain.sql.DBConnection;
import com.workbrain.util.DateHelper;
import com.workbrain.util.StringHelper;

/**
 * @author bviveiros
 * 
 * Adds a Pay Rule Test Case to a Test Suite.
 *
 */
public class RulesCaseAddAction extends RulesCaseAction {

	/* (non-Javadoc)
	 * @see com.workbrain.app.jsp.Action#createRequest(javax.servlet.ServletRequest, com.workbrain.sql.DBConnection)
	 */
	public Object createRequest(ServletRequest request, DBConnection conn)
			throws Exception {

		RulesCaseParamsBean formBean = new RulesCaseParamsBean();
		populateRulesCaseFromRequest(request, formBean);
		return formBean;
	}

	/* (non-Javadoc)
	 * @see com.workbrain.app.jsp.Action#process(com.workbrain.sql.DBConnection, com.workbrain.app.jsp.ActionContext, java.lang.Object)
	 */
	public String process(DBConnection conn, ActionContext actionContext, Object formBean)
			throws Exception {

		RulesCaseParamsBean testCaseParams = (RulesCaseParamsBean) formBean;
		RulesCaseData testCase = null; 
		
		String result = ForwardRulesCase.ADD_TEST_CASE_SUCCEED;
		RulesCaseResultExpected expectedResult = null;
		ServletRequest request = actionContext.getRequest();
		
		EmployeeData employee = null;
		List employeeList = loadEmployees(conn, testCaseParams);
		
		if (employeeList == null) {
			return ForwardRulesCase.ADD_TEST_CASE_FAILED;
		}
		
		try {
			// Create a test case for each employee.
			Iterator i = employeeList.iterator();
			while (i.hasNext()) {
				
				employee = (EmployeeData) i.next();
			
				// Create the test case object.
				testCase = createRulesCase(employee.getEmpId(), employee.getEmpName(), testCaseParams);
				
				// If a name doesn't exist then set a default name and description.
				if (StringHelper.isEmpty(testCaseParams.getName())) {
				
					testCase.setCaseName(getDefaultCaseName(employee.getEmpName(), 
													testCase.getCaseStartDate(), 
													testCase.getCaseEndDate()));
		
					if (StringHelper.isEmpty(testCase.getCaseDesc())) {
						testCase.setCaseDesc(getDefaultCaseDesc(employee.getEmpName(), 
													testCase.getCaseStartDate(), 
													testCase.getCaseEndDate()));
					}
				}
			
				// Generate the expected results.
				expectedResult = new RulesCaseResultExpected(testCase, conn);
				testCase.setCaseExpectResult(expectedResult.getExpectedResult());
				
				// Store the Test Case to the database.
				RulesCaseAccess tcAccess = new RulesCaseAccess(conn);
				tcAccess.addTestCase(testCase);
			}

		} catch (Exception e) {
			throw e;
		}
		
		return result;
	}

	/*
	 * Get a list of EmployeeData objects based on the query parameters.
	 *  
	 * @param conn
	 * @param testCaseParams
	 * @return
	 * @throws Exception
	 */
	private List loadEmployees(DBConnection conn, RulesCaseParamsBean testCaseParams) 
								throws Exception {
		
		GetEmployeeParameters params = new GetEmployeeParameters();
		
		params.setWbuId(Integer.parseInt(testCaseParams.getWbuId()));

		params.setCalcGroupIds(testCaseParams.getCalcGroupIds());
		params.setEmpIds(testCaseParams.getEmpIds());
		params.setPayGroupIds(testCaseParams.getPayGroupIds());
		params.setShiftIds(testCaseParams.getShiftIds());
		params.setTeamIds(testCaseParams.getTeamIds());
		
		params.setIncludeSubTeams(JSPHelper.getBoolean(testCaseParams.getIncludeSubTeams()));

		params.setStartDate(DateHelper.parseDate(testCaseParams.getStartDate(), DATE_FORMAT_INPUT));
		params.setEndDate(DateHelper.parseDate(testCaseParams.getEndDate(), DATE_FORMAT_INPUT));
		
		return GetEmployeeHelper.loadEmployees(conn, params);
	}


}
