package com.wbiag.tool.regressiontest.report;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.wbiag.tool.regressiontest.model.RulesCaseResultActual;
import com.wbiag.tool.regressiontest.model.RulesCaseResultExpected;
import com.wbiag.tool.regressiontest.model.TestReportData;
import com.wbiag.tool.regressiontest.model.TestSuiteData;
import com.wbiag.tool.regressiontest.xml.RulesReportHelper;
import com.wbiag.tool.regressiontest.xml.RulesResultsHelper;
import com.workbrain.util.NestedRuntimeException;

/**
 * @author bviveiros
 * 
 * This class
 * does the comparason of the expected results with the actual results
 * and generates a report.
 * 
 * The compare of the results is done in the PayRulesResultsHelper.  It
 * is the only class that has knowledge of the actual/expected data format.
 * 
 * The generate of the report is done in the PayRulesReportHelper.  It
 * is the only class that has knowledge of the report format.
 */
public class RulesReportXMLGenerator implements IReportXMLGenerator {

	private int testCount = 0;
	private int failureCount = 0;
	private int errorCount = 0;
	private long executionTimeSeconds = 0;
	private List testCaseResults = null;

	private	String xmlReport = null;
		
	private RulesReportHelper reportHelper = null;
	private RulesResultsHelper resultsHelper = null;
	
	private TestSuiteData testSuite = null;
	private Map actualResultList = null;
	private Map expectedResultList = null;

	
	/**
	 * Constructor.
	 * 
	 * @param testSuite
	 * @param actualResultList
	 * @param expectedResultList
	 * @param executionTimeSeconds
	 * @throws Exception
	 */
	public RulesReportXMLGenerator(TestSuiteData testSuite, 
										Map actualResultList, 
										Map expectedResultList,
										long executionTimeSeconds) throws Exception {

		// Throw an exception if Actual Result or Expected Results are null.
		if (actualResultList == null) {
			throw new NestedRuntimeException("Actual Result List cannot be null.");
		}
		if (expectedResultList == null) {
			throw new NestedRuntimeException("Expected Result List cannot be null.");
		}

		this.testSuite = testSuite;
		this.actualResultList = actualResultList;
		this.expectedResultList = expectedResultList;
		this.executionTimeSeconds = executionTimeSeconds;
		
		testCaseResults = new ArrayList();
		
		// Helper class to perform the comparason of expected to
		// actual results.
		resultsHelper = new RulesResultsHelper();
		
		// Helper class to generate a report of the results.
		reportHelper = new RulesReportHelper();
	}
	

	/* (non-Javadoc)
	 * @see com.wbiag.tool.regressiontest.testengine.IReportXMLGenerator#getReportData()
	 */
	public TestReportData getReportData() throws Exception {
		
		TestReportData reportData = new TestReportData();
		
		reportData.setSuiteId(testSuite.getSuiteId());
		reportData.setReportDate(testSuite.getSuiteExecuteDate());

		if (xmlReport == null) {
			generateXML();
		}
		
		reportData.setReportOutput(xmlReport);
		
		return reportData;
	}
	
	
	/* (non-Javadoc)
	 * @see com.wbiag.tool.regressiontest.testengine.IReportGenerator#generateXML()
	 */
	public String generateXML() throws Exception {

		RulesCaseResultActual actualResult = null;
		RulesCaseResultExpected expectedResult = null;
		Integer testCaseId = null;
		TestCaseResult caseResult = null;
		TestCaseCompareResult compareRusult = null;
		long executionTime = 0;
		
		Iterator i = expectedResultList.keySet().iterator();
		
		while (i.hasNext()) {
			
			testCount++;
			
			testCaseId = (Integer) i.next();
			
			// Expected result for the Test Case.
			expectedResult = (RulesCaseResultExpected) expectedResultList.get(testCaseId);
			
			// Actual result for the Test Case.
			actualResult = (RulesCaseResultActual) actualResultList.get(testCaseId);
			
			// Get the execution time of the test case.
			executionTime = actualResult == null ? 0 : actualResult.getExecutionTime();
			
			// Create a new result object to hold the results of each day of the
			// test case.
			caseResult = new TestCaseResult(expectedResult.getTestCase(), executionTime);

			// If there was no actual result, then the test case failed.
			if (actualResult == null) {
				// Fail.
				failureCount++;
				caseResult.addResult(TestCaseCompareResult.STATUS_FAILED,
										"Actual Result is null.",
										null, null);
				
				
			} else {
				// If there was an exception during the test case, 
				// then the test case was in error.
				if (actualResult.getExceptionThrown() != null) {
					// Error
					errorCount++;
					caseResult.addResult(TestCaseCompareResult.STATUS_ERROR,
											actualResult.getExceptionThrown().getMessage(),
											null, null);
					
				} else {
					// Compare the expected and actual results.
					resultsHelper.compare(caseResult, expectedResult, actualResult);
					
					// If a message is returned, then the test case failed.
					if (!caseResult.isAllPassed()) {
						// Fail.
						failureCount++;
											
					// The test case succeeded.
					} else {
						// Success.
						caseResult.addResult(TestCaseCompareResult.STATUS_SUCCESS, null, null, null);
					}
				}
			}
			
			// Add the result of this test case to the list.
			testCaseResults.add(caseResult);
		}
		
		// Generate a report in XML format for the test suite, and 
		// the list of results.
		xmlReport = reportHelper.getReportXML(testSuite,
													testCount,
													failureCount,
													errorCount,
													executionTimeSeconds,
													testSuite.getSuiteExecuteDate(),
													testCaseResults);
		
		// Return the report.
		return xmlReport;
	}

}
