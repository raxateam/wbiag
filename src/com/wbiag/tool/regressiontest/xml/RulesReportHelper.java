package com.wbiag.tool.regressiontest.xml;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.wbiag.tool.regressiontest.model.TestSuiteData;
import com.wbiag.tool.regressiontest.report.ReportDisplayRulesCase;
import com.wbiag.tool.regressiontest.report.ReportDisplayRulesDay;
import com.wbiag.tool.regressiontest.report.ReportDisplayRulesSuite;
import com.wbiag.tool.regressiontest.report.TestCaseCompareResult;
import com.wbiag.tool.regressiontest.report.TestCaseResult;
import com.wbiag.util.XMLHelper;
import com.workbrain.util.DateHelper;

/**
 * @author bviveiros
 *
 * Functions related to the Test Suite Report XML.
 * 
 * XML DTD is:
 * 
 * <testsuites>
 * 		<testsuite name="" tests="" failures="" errors="" time="">

 * 				<testcase name="" time="" />
 * 
 * 				<testcase name="" time="">
 *
 * 					<TESTCASE_RESULT work_summary_date="">
 * 						<failure></failure>
 * 						<expected_data></expected_data>
 * 						<actual_data></actual_data>
 * 					</TESTCASE_RESULT>
 * 
 * 					<TESTCASE_RESULT work_summary_date="">
 * 						<failure></failure>
 * 						<expected_data></expected_data>
 * 						<actual_data></actual_data>
 * 					</TESTCASE_RESULT>
 * 
 * 					<TESTCASE_RESULT work_summary_date="">
 * 						<error message=""></error>
 * 					</TESTCASE_RESULT>
 * 
 * 				</testcase>			
 * 
 * 		</testsuite>
 * </testsuites>
 * 
 */
public class RulesReportHelper {

	// XML Element and Attributes.
	private static final String ELEMENT_TEST_SUITES = "testsuites";
	private static final String ELEMENT_TEST_SUITE = "testsuite";
	private static final String ELEMENT_TEST_CASE_RESULT = "testcase_result";
	private static final String ELEMENT_TEST_CASE = "testcase";
	private static final String ELEMENT_FAILURE = "failure";
	private static final String ELEMENT_ERROR = "error";
	private static final String ELEMENT_EXPECTED_DATA = "expected_data";
	private static final String ELEMENT_ACTUAL_DATA = "actual_data";
	private static final String ATTRIBUTE_EXECUTED_DATE = "executed_date";
	private static final String ATTRIBUTE_TIME = "time";
	private static final String ATTRIBUTE_ERRORS = "errors";
	private static final String ATTRIBUTE_FAILURES = "failures";
	private static final String ATTRIBUTE_TESTS = "tests";
	private static final String ATTRIBUTE_NAME = "name";
	private static final String ATTRIBUTE_WORK_SUMMARY_DATE = "work_summary_date";
	private static final String ATTRIBUTE_EMP_NAME = "emp_name";
	
	
	private static final String DATE_TIME_FORMAT = "MM/dd/yyyy HH:mm:ss";
	private static final String DATE_FORMAT = "MM/dd/yyyy";
	
	Document doc = null;
	
	public RulesReportHelper() throws Exception {
		doc = XMLHelper.getNewDocument();
	}
	
	public RulesReportHelper(String reportXML) throws Exception {
		doc = XMLHelper.createDocument(reportXML);
	}
	public void setReportXML(String reportXML) throws Exception {
		doc = XMLHelper.createDocument(reportXML);
	}
	
	/**
	 * Return an XML representation of a report.
	 * 
	 * @param testSuite
	 * @param testCount
	 * @param failureCount
	 * @param errorCount
	 * @param executionTimeSeconds
	 * @param testCaseResults
	 * @return
	 */
	public String getReportXML(TestSuiteData testSuite,
								int testCount,
								int failureCount,
								int errorCount,
								long executionTimeSeconds,
								Date executedDate,
								List testCaseResults) {

		String reportXML = null;
		
		// Create the root node.
		Element rootNode = doc.createElement(ELEMENT_TEST_SUITES);
		
		// Create the Test Suite node.
		Element testSuiteNode = doc.createElement(ELEMENT_TEST_SUITE);
		testSuiteNode.setAttribute(ATTRIBUTE_NAME, testSuite.getSuiteName());
		testSuiteNode.setAttribute(ATTRIBUTE_TESTS, String.valueOf(testCount));
		testSuiteNode.setAttribute(ATTRIBUTE_FAILURES, String.valueOf(failureCount));
		testSuiteNode.setAttribute(ATTRIBUTE_ERRORS, String.valueOf(errorCount));
		testSuiteNode.setAttribute(ATTRIBUTE_TIME, String.valueOf(executionTimeSeconds));
		testSuiteNode.setAttribute(ATTRIBUTE_EXECUTED_DATE, DateHelper.convertDateString(executedDate, DATE_TIME_FORMAT));
		
		// For each Test Case.
		Element testCaseNode = null;
		Element testCaseDayResultNode = null;
		Element messageNode = null;
		TestCaseResult result = null;
		TestCaseCompareResult compareResult = null;
		
		try {
			Iterator i = testCaseResults.iterator();
			while (i.hasNext()) {
				
				result = (TestCaseResult) i.next();
				
				testCaseNode = doc.createElement(ELEMENT_TEST_CASE);
				testCaseNode.setAttribute(ATTRIBUTE_NAME, result.getCaseName());
				testCaseNode.setAttribute(ATTRIBUTE_TIME, String.valueOf(result.getExecutionTimeSeconds()));
				testCaseNode.setAttribute(ATTRIBUTE_EMP_NAME, result.getEmpName());
				
				if (!result.isAllPassed()) {
					
					Iterator j = result.getDailyCompareResults().iterator();
					while (j.hasNext()) {
	
						compareResult = (TestCaseCompareResult) j.next();
						
						testCaseDayResultNode = doc.createElement(ELEMENT_TEST_CASE_RESULT);
						testCaseDayResultNode.setAttribute(ATTRIBUTE_WORK_SUMMARY_DATE, 
															DateHelper.convertDateString(compareResult.getWorkSummaryDate(),
																							DATE_FORMAT));
						
						// Add the error message tag.
						if (compareResult.getStatus() == TestCaseCompareResult.STATUS_ERROR) {
							messageNode = doc.createElement(ELEMENT_ERROR);
						} else if (compareResult.getStatus() == TestCaseCompareResult.STATUS_FAILED) {
							messageNode = doc.createElement(ELEMENT_FAILURE);
						}
						messageNode.appendChild(doc.createTextNode(compareResult.getErrorMessage()));
						testCaseDayResultNode.appendChild(messageNode);
		
						// Add the expected result.
						if (compareResult.getExpectedData() != null) {
							messageNode = doc.createElement(ELEMENT_EXPECTED_DATA);
							messageNode.appendChild(getNodeFromText(doc, compareResult.getExpectedData()));
							testCaseDayResultNode.appendChild(messageNode);
						}
						
						// Add the actual result.
						if (compareResult.getActualData() != null) {
							messageNode = doc.createElement(ELEMENT_ACTUAL_DATA);
							messageNode.appendChild(getNodeFromText(doc, compareResult.getActualData()));
							testCaseDayResultNode.appendChild(messageNode);
						}
						
						// Add the results of the day to the results of the case.
						testCaseNode.appendChild(testCaseDayResultNode);
					}
					
				}
				
				// Add the Test Case to the Test Suite.
				testSuiteNode.appendChild(testCaseNode);
			}
			
			// Add the Test Suite to the root node.
			rootNode.appendChild(testSuiteNode);
		
			reportXML = com.workbrain.util.XMLHelper.convertToText(rootNode);
			
		} catch (Exception e) {
			
		}
		
		return reportXML;
	}
	
	private Node getNodeFromText(Document currentDoc, String xmlText) throws Exception {

		Document tempDoc = XMLHelper.createDocument(xmlText);
		return currentDoc.importNode(tempDoc.getDocumentElement(), true);
	}	
	
	/**
	 * Generate a ReportDisplayRulesSuite object from the XML String.
	 * 
	 * @return
	 */
	public ReportDisplayRulesSuite getReportDisplay() {
		
		ReportDisplayRulesSuite reportDisplay = new ReportDisplayRulesSuite();
		
		setReportSummary(reportDisplay);
		
		setCaseResults(reportDisplay);
		
		return reportDisplay;
	}
	
	
	/*
	 * Set the summary properties of a report.
	 * 
	 * @param reportDisplay
	 */
	private void setReportSummary(ReportDisplayRulesSuite reportDisplay) {
		
		NodeList testSuiteList = doc.getElementsByTagName(ELEMENT_TEST_SUITE);
		
		// There will be only 1 test suite in the list.
		Element testSuite = (Element) testSuiteList.item(0);
		
		reportDisplay.setSuiteName(testSuite.getAttribute(ATTRIBUTE_NAME));
		reportDisplay.setTestCount(Integer.parseInt(testSuite.getAttribute(ATTRIBUTE_TESTS)));
		reportDisplay.setFailCount(Integer.parseInt(testSuite.getAttribute(ATTRIBUTE_FAILURES)));
		reportDisplay.setErrorCount(Integer.parseInt(testSuite.getAttribute(ATTRIBUTE_ERRORS)));
		reportDisplay.setExecutionTimeSeconds(Long.parseLong(testSuite.getAttribute(ATTRIBUTE_TIME)));
		reportDisplay.setExecutedDate(DateHelper.parseDate(testSuite.getAttribute(ATTRIBUTE_EXECUTED_DATE), DATE_TIME_FORMAT));

		// Success rate is pass/total * 100 as a double.
		reportDisplay.setSuccessRate((int)(
				(double)(
				    (reportDisplay.getTestCount() - reportDisplay.getFailCount() - reportDisplay.getErrorCount())
					/
					(double) reportDisplay.getTestCount()
				)
				* 100d)
				);
		
	}
	
	
	/*
	 * Generates a list of test cases for the display object.
	 * 
	 * @param reportDisplay
	 */
	private void setCaseResults(ReportDisplayRulesSuite reportDisplay) {
		
		ReportDisplayRulesCase displayTestCase = null;
		
		NodeList testCaseList = doc.getElementsByTagName(ELEMENT_TEST_CASE);
		Element testCaseElement = null;
		NodeList testCaseResults = null;
		List caseResults = new ArrayList();
		
		// There will be multipe test cases.
		for (int i=0; i < testCaseList.getLength(); i++) {
			
			testCaseElement = (Element) testCaseList.item(i);
			
			// Create the display test case object.
			displayTestCase = new ReportDisplayRulesCase();
			
			displayTestCase.setCaseName(testCaseElement.getAttribute(ATTRIBUTE_NAME));
			displayTestCase.setExecutionTimeSeconds(Long.parseLong(testCaseElement.getAttribute(ATTRIBUTE_TIME)));
			displayTestCase.setEmpName(testCaseElement.getAttribute(ATTRIBUTE_EMP_NAME));
			
			// Create the display test case results object.
			testCaseResults = testCaseElement.getElementsByTagName(ELEMENT_TEST_CASE_RESULT);
			
			if (testCaseResults != null && testCaseResults.getLength() > 0) {
				setCaseDayResults(displayTestCase, testCaseResults);
			}
			
			caseResults.add(displayTestCase);
		}
		
		if (!caseResults.isEmpty()) {
			reportDisplay.setCaseResults(caseResults);
		}
	}

	/*
	 * Generates a list of test case results.
	 * 
	 * @param reportDisplay
	 * @param testCaseResults
	 */
	private void setCaseDayResults(ReportDisplayRulesCase displayCase, NodeList testCaseResults) {

		ReportDisplayRulesDay displayTestDay = null;
		List columnNames = null;
		List expectedData = null;
		List actualData = null;
		Date workSummaryDate = null;
		List dayResults = new ArrayList();
		boolean processed = false;
		
		Element resultNode = null;
		
		for (int i = 0; i < testCaseResults.getLength(); i++) {
			
			displayTestDay = new ReportDisplayRulesDay();
			
			resultNode = (Element) testCaseResults.item(i);
			workSummaryDate = DateHelper.parseDate(resultNode.getAttribute(ATTRIBUTE_WORK_SUMMARY_DATE), DATE_FORMAT);
			
			displayTestDay.setWorkSummaryDate(workSummaryDate);

			// Check for Failure.
			processed = parseFails(resultNode, displayTestDay);
				
			// Check for Exception.
			if (!processed) {
				processed = parseExceptions(resultNode, displayTestDay);
			}
			
			// Must have been a pass.
			if (!processed) {
				displayTestDay.setStatus(TestCaseCompareResult.STATUS_SUCCESS);
			}
			
			dayResults.add(displayTestDay);
		}
		
		if (!dayResults.isEmpty()) {
			displayCase.setCaseDayResults(dayResults);
		}
	}
	

	private boolean parseFails(Element resultNode, ReportDisplayRulesDay displayTestDay) {
		
		NodeList failList = resultNode.getElementsByTagName(ELEMENT_FAILURE);
		Element failureNode = null;
		Element calcResultNode = null;
		NodeList expectedDataList = null;
		NodeList actualDataList = null;
		
		if (failList == null) {
			return false;
		} else {
			// Get the details of the fail.
			failureNode = (Element) failList.item(0);
			
			displayTestDay.setStatus(TestCaseCompareResult.STATUS_FAILED);
			displayTestDay.setMessage(failureNode.getFirstChild().getNodeValue());
			
			// Parse the expected result XML into the result object.
			expectedDataList = resultNode.getElementsByTagName(ELEMENT_EXPECTED_DATA);
			if (expectedDataList != null && expectedDataList.getLength() > 0) {
				RulesResultsHelper.parseExpectedResult((Element)expectedDataList.item(0), displayTestDay);
			}
			
			// Parse the actual result XML into the result object.
			actualDataList = resultNode.getElementsByTagName(ELEMENT_ACTUAL_DATA);
			if (actualDataList != null && actualDataList.getLength() > 0) {
				RulesResultsHelper.parseActualResult((Element)actualDataList.item(0), displayTestDay);
			}
		}
		
		return true;
	}

	
	private boolean parseExceptions(Element resultNode, ReportDisplayRulesDay displayTestDay) {

		NodeList exceptionList = resultNode.getElementsByTagName(ELEMENT_ERROR);
		Element exceptionNode = null;
		
		if (exceptionList == null) {
			return false;
		} else {
			// Get the details of the exception.
			exceptionNode = (Element) exceptionList.item(0);
			
			displayTestDay.setStatus(TestCaseCompareResult.STATUS_FAILED);
			displayTestDay.setMessage(exceptionNode.getNodeValue());
		}
		
		return true;
	}
}
