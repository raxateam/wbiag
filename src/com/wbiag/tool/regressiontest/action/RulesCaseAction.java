package com.wbiag.tool.regressiontest.action;

import java.util.Date;

import javax.servlet.ServletRequest;

import com.wbiag.tool.regressiontest.jsp.RulesCaseParamsBean;
import com.wbiag.tool.regressiontest.model.RulesCaseData;
import com.wbiag.tool.regressiontest.xml.OutputAttributeHelper;
import com.workbrain.app.jsp.Action;
import com.workbrain.server.jsp.JSPHelper;
import com.workbrain.util.DateHelper;
import com.workbrain.util.StringHelper;

/**
 * @author bviveiros
 *
 * All PayRulesTestCase Action classes should extend this class.
 * 
 */
public abstract class RulesCaseAction implements Action {

	public static final String DATE_FORMAT_INPUT = "yyyyMMdd HHmmss";
	public static final String DATE_FORMAT_DISPLAY = "MM/dd/yyyy";
	
	/**
	 * Create a default Test Case Name in the format:
	 * empName (startDate - endDate)
	 * or
	 * empName (startDate)
	 * 
	 * @param empId
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	protected String getDefaultCaseName(String empName, Date startDate, Date endDate) {
		
		StringBuffer defaultName = new StringBuffer();
		defaultName.append(empName);
		defaultName.append(" (");
		defaultName.append(DateHelper.convertDateString(startDate, DATE_FORMAT_DISPLAY));
		
		if (endDate != null) {
			defaultName.append(" - ");
			defaultName.append(DateHelper.convertDateString(endDate, DATE_FORMAT_DISPLAY));
		}
		
		defaultName.append(")");
		
		return defaultName.toString();
	}
	
	/**
	 * Create a default Test Case Description.  Format is same as Default Test Case Name above.
	 * 
	 * @param empName
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	protected String getDefaultCaseDesc(String empName, Date startDate, Date endDate) {
		return getDefaultCaseName(empName, startDate, endDate);
	}
	
	/**
	 * Populate the Pay Rules Test Case bean with the parameters in the request.
	 * 
	 * @param request
	 * @param testSuite
	 * @return
	 */
	protected void populateRulesCaseFromRequest(ServletRequest request, 
												RulesCaseParamsBean formBean)
												throws Exception {
		
		formBean.setTestSuiteId(request.getParameter("testSuiteId"));
		formBean.setRulesCaseId(request.getParameter("testCaseId"));
		formBean.setName(request.getParameter("testCaseName"));
		formBean.setDescription(request.getParameter("testCaseDescription"));
		formBean.setWbuId(JSPHelper.getWebLogin(request).getUserId());
		formBean.setEmpIds(request.getParameter("employeeIds"));
		formBean.setEmpNames(request.getParameter("employeeIds_label"));
		formBean.setTeamIds(request.getParameter("teamIds"));
		formBean.setPayGroupIds(request.getParameter("payGroupIds"));
		formBean.setCalcGroupIds(request.getParameter("calcGroupIds"));
		formBean.setShiftIds(request.getParameter("shiftIds"));
		
		formBean.setStartDate(request.getParameter("startDate"));
		
		if (StringHelper.isEmpty(request.getParameter("endDate"))) {
			formBean.setEndDate(formBean.getStartDate());
		} else {
			formBean.setEndDate(request.getParameter("endDate"));
		}
		
		formBean.setIncludeSubTeams(request.getParameter("includeSubTeams"));
		
		// Get the output attributes.
		formBean.setOutputAttrib(OutputAttributeHelper.getOutputAttribXML(request));
	}
	
	
	protected RulesCaseData createRulesCase(int empId, String empName, RulesCaseParamsBean formBean) {
		
		RulesCaseData testCase = new RulesCaseData();
		
		testCase.setSuiteId(Integer.parseInt(formBean.getTestSuiteId()));
		testCase.setCaseId(Integer.parseInt(formBean.getRulesCaseId()));
		testCase.setCaseName(formBean.getName());
		testCase.setCaseDesc(formBean.getDescription());
		testCase.setEmpId(empId);
		testCase.empName(empName);
		
		testCase.setCaseStartDate(
					com.workbrain.util.StringHelper.isEmpty(formBean.getStartDate()) ? null
					: DateHelper.parseDate(formBean.getStartDate(), DATE_FORMAT_INPUT)
					);

		testCase.setCaseEndDate(
					com.workbrain.util.StringHelper.isEmpty(formBean.getEndDate()) ? null
					: DateHelper.parseDate(formBean.getEndDate(), DATE_FORMAT_INPUT)
					);
	
		// Get the output attributes.
		testCase.setCaseOutputAttrib(formBean.getOutputAttrib());
		
		return testCase;
	}
}
