package com.wbiag.tool.regressiontest.report;

import java.util.Date;
import java.util.List;

/**
 * @author bviveiros
 *
 * 
 */
public abstract class ReportDisplaySuite {
	
	private int reportId = 0;
	private String suiteName = null;
	private int testCount = 0;
	private int failCount = 0;
	private int errorCount = 0;
	private int successRate = 0;
	private long executionTimeSeconds = 0;
	private Date executedDate = null;
	
	// List of ReportDisplayRulesCase.
	private List caseResults = null;
	
	
	
	/**
	 * Returns a List of ReportDisplayRulesCase
	 * 
	 * @return
	 */
	public List getCaseResults() {
		return caseResults;
	}
	public void setCaseResults(List caseResults) {
		this.caseResults = caseResults;
	}
	public int getErrorCount() {
		return errorCount;
	}
	public void setErrorCount(int errorCount) {
		this.errorCount = errorCount;
	}
	public Date getExecutedDate() {
		return executedDate;
	}
	public void setExecutedDate(Date executedDate) {
		this.executedDate = executedDate;
	}
	public long getExecutionTimeSeconds() {
		return executionTimeSeconds;
	}
	public void setExecutionTimeSeconds(long executionTimeSeconds) {
		this.executionTimeSeconds = executionTimeSeconds;
	}
	public int getFailCount() {
		return failCount;
	}
	public void setFailCount(int failCount) {
		this.failCount = failCount;
	}
	public int getSuccessRate() {
		return successRate;
	}
	public void setSuccessRate(int successRate) {
		this.successRate = successRate;
	}
	public String getSuiteName() {
		return suiteName;
	}
	public void setSuiteName(String suiteName) {
		this.suiteName = suiteName;
	}
	public int getTestCount() {
		return testCount;
	}
	public void setTestCount(int testCount) {
		this.testCount = testCount;
	}
	public void setReportId(int reportId) {
		this.reportId = reportId;
	}
	public int getReportId() {
		return reportId;
	}
}
