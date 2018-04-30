package com.wbiag.tool.regressiontest.report;

import java.util.List;

/**
 * @author bviveiros
 *
 * The result of one test case.
 * 
 */
public class ReportDisplayRulesCase {

	// List of ReportDisplayRulesDay
	private List caseDayResults = null;

	private String caseName = null;
	private long executionTimeSeconds = 0;
	private String empName = null;
	
	
	public long getExecutionTimeSeconds() {
		return executionTimeSeconds;
	}
	public void setExecutionTimeSeconds(long executionTimeSeconds) {
		this.executionTimeSeconds = executionTimeSeconds;
	}
	public String getCaseName() {
		return caseName;
	}
	public void setCaseName(String caseName) {
		this.caseName = caseName;
	}
	public String getEmpName() {
		return empName;
	}
	public void setEmpName(String empName) {
		this.empName = empName;
	}
	
	/**
	 * Returns a list of ReportDisplayRulesDa.
	 * 
	 * @return
	 */
	public List getCaseDayResults() {
		return caseDayResults;
	}
	public void setCaseDayResults(List caseDayResults) {
		this.caseDayResults = caseDayResults;
	}
}
