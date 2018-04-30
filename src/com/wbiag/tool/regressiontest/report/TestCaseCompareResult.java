package com.wbiag.tool.regressiontest.report;

import java.io.Serializable;
import java.util.Date;

/**
 * @author bviveiros
 *
 * Used for the result of comparing an expected result to
 * an actual result.
 * 
 */
public class TestCaseCompareResult implements Serializable {

	public static final int STATUS_SUCCESS = 1;
	public static final int STATUS_FAILED = 2;
	public static final int STATUS_ERROR = 3;
	
	private String errorMessage = null;
	private int status = 0;
	private String expectedData = null;
	private String actualData = null;
	private Date workSummaryDate = null;
	
	public int getStatus() {
		return status;
	}
	public void setStatus(int status) {
		this.status = status;
	}
	public String getErrorMessage() {
		return errorMessage;
	}
	public void setErrorMessage(String message) {
		this.errorMessage = message;
	}
	public String getActualData() {
		return actualData;
	}
	public void setActualData(String actualData) {
		this.actualData = actualData;
	}
	public String getExpectedData() {
		return expectedData;
	}
	public void setExpectedData(String expectedData) {
		this.expectedData = expectedData;
	}
	public Date getWorkSummaryDate() {
		return workSummaryDate;
	}
	public void setWorkSummaryDate(Date workSummaryDate) {
		this.workSummaryDate = workSummaryDate;
	}
}
