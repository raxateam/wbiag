package com.wbiag.tool.regressiontest.report;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.wbiag.tool.regressiontest.model.RulesCaseData;


/**
 * @author bviveiros
 *
 * This class is used by PayRulesTestSuiteReportData to store the result
 * of an individual Test Case, and the PayRulesReportHelper to generate
 * the report of the results.
 */
public class TestCaseResult implements Serializable {

	private String caseName = null;
	private long executionTimeSeconds = 0;
	private int empId = 0;
	private Date startDate = null;
	private Date endDate = null;
	private boolean allPassed = true;
	
	// List of TestCaseCompareResult.
	private List dailyCompareResults = null;
	
	private String empName = null;

	/**
	 * Constructor from a RulesCaseData.
	 * 
	 * @param testCase
	 * @param executionTimeSeconds
	 */
	public TestCaseResult(RulesCaseData testCase, long executionTimeSeconds) {
		
		caseName = testCase.getCaseName();
		empId = testCase.getEmpId();
		empName = testCase.empName();
		startDate = testCase.getCaseStartDate();
		endDate = testCase.getCaseEndDate();
		this.executionTimeSeconds = executionTimeSeconds;
	}
	
	/**
	 * Adds a TestCaseCompareResult to the list.
	 * 
	 * @param status
	 * @param errorMessage
	 * @param actualData
	 * @param expectedData
	 */
	public void addResult(int status, String errorMessage, String actualData, String expectedData) {
		
		if (dailyCompareResults == null) {
			dailyCompareResults = new ArrayList();
		}
	
		TestCaseCompareResult compareResult = new TestCaseCompareResult();
		
		compareResult.setStatus(status);
		compareResult.setErrorMessage(errorMessage);
		compareResult.setActualData(actualData);
		compareResult.setExpectedData(expectedData);
		
		dailyCompareResults.add(compareResult);
	}
	
	
	//*****************************************************
	// Properties
	///

	
	public String getCaseName() {
		return caseName;
	}
	public void setCaseName(String caseName) {
		this.caseName = caseName;
	}
	public int getEmpId() {
		return empId;
	}
	public void setEmpId(int empId) {
		this.empId = empId;
	}
	public Date getEndDate() {
		return endDate;
	}
	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}
	public long getExecutionTimeSeconds() {
		return executionTimeSeconds;
	}
	public void setExecutionTimeSeconds(long executionTimeSeconds) {
		this.executionTimeSeconds = executionTimeSeconds;
	}
	public Date getStartDate() {
		return startDate;
	}
	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}
	public String getEmpName() {
		return empName;
	}
	public void setEmpName(String empName) {
		this.empName = empName;
	}
	
	/**
	 * Returns a list of TestCaseCompareResult.
	 * @return
	 */
	public List getDailyCompareResults() {
		return dailyCompareResults;
	}
	public void setDailyCompareResults(List dailyCompareResults) {
		this.dailyCompareResults = dailyCompareResults;
	}
	public boolean isAllPassed() {
		return allPassed;
	}
	public void setAllPassed(boolean allPassed) {
		this.allPassed = allPassed;
	}
}
