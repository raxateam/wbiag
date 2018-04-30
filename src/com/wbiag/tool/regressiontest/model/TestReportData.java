package com.wbiag.tool.regressiontest.model;

import java.util.Date;

import com.workbrain.app.ta.model.RecordData;

/**
 * @author bviveiros
 *
 * A Test Suite Report is the result of the Test Suite run.  Contains
 * the number of test cases, timestamp of the run, XML representation
 * of the results, etc.
 * 
 * Also represents a row in the WBIAG_TST_REPORT table.
 */
public class TestReportData extends RecordData {

	// Database columns.
	protected int reportId = 0;
	protected Date reportDate = null;
	protected String reportOutput = null;
	protected int suiteId = 0;
	
	public TestReportData() {
	}
	
	public int getReportId() {
		return reportId;
	}
	public void setReportId(int id) {
		this.reportId = id;
	}
	public int getSuiteId() {
		return suiteId; 
	}
	public void setSuiteId(int suiteId) {
		this.suiteId = suiteId; 
	}
	public Date getReportDate() {
		return reportDate;
	}
	public void setReportDate(Date reportDate) {
		this.reportDate = reportDate;
	}
	public String getReportOutput() {
		return reportOutput;
	}
	public void setReportOutput(String reportOutput) {
		this.reportOutput = reportOutput;
	}


	/* (non-Javadoc)
	 * @see com.workbrain.app.ta.model.RecordData#newInstance()
	 */
	public RecordData newInstance() {
		return new TestReportData();
	}

}
