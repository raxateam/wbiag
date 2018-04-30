package com.wbiag.tool.regressiontest.util;

import java.util.List;

import javax.servlet.ServletRequest;

import com.wbiag.tool.regressiontest.jsp.RulesCaseParamsBean;
import com.wbiag.tool.regressiontest.model.TestSuiteData;

/**
 * @author bviveiros
 *
 * Methods to set/get objects to/from request attributes.
 */
public class RequestHelper {

	private static final String TEST_SUITE_BEAN_ATTRIB = "testSuiteData";
	private static final String TEST_CASE_PARAMS_ATTRIB = "testCaseParameters";
	private static final String TEST_CASE_LIST_ATTRIB = "testCaseList";
	private static final String TEST_REPORT_ID_ATTRIB = "reportId";
	

	/*
	 * Get/Set objects to/from the request. 
	 */ 
	
	// Test Suite.
	public static TestSuiteData getTestSuiteBean(ServletRequest request) {
		return (TestSuiteData) request.getAttribute(TEST_SUITE_BEAN_ATTRIB);
	}
	public static void setTestSuiteBean(ServletRequest request, TestSuiteData testSuite) {
		request.setAttribute(TEST_SUITE_BEAN_ATTRIB, testSuite);
	}
	
	// Test Case List.
	public static List getTestCaseList(ServletRequest request) {
		return (List) request.getAttribute(TEST_CASE_LIST_ATTRIB);
	}
	public static void setTestCaseList(ServletRequest request, List testCaseList) {
		request.setAttribute(TEST_CASE_LIST_ATTRIB, testCaseList);
	}

	// Test Case Parameters.
	public static RulesCaseParamsBean getRulesCaseParameters(ServletRequest request) {
		return (RulesCaseParamsBean) request.getAttribute(TEST_CASE_PARAMS_ATTRIB);
	}
	public static void setRulesCaseParameters(ServletRequest request, RulesCaseParamsBean testCaseParams) {
		request.setAttribute(TEST_CASE_PARAMS_ATTRIB, testCaseParams);
	}

	// Report Id.
	public static int getReportId(ServletRequest request) {
		Integer reportObj = (Integer) request.getAttribute(TEST_REPORT_ID_ATTRIB);
		int reportId = 0;
		if (reportObj != null) {
			reportId = reportObj.intValue();
		}
		return reportId;
	}
	public static void setReportId(ServletRequest request, int reportId) {
		request.setAttribute(TEST_REPORT_ID_ATTRIB, new Integer(reportId));
	}
	
}
