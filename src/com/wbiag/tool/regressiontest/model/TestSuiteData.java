package com.wbiag.tool.regressiontest.model;

import java.util.Date;

import com.workbrain.app.ta.model.RecordData;

/**
 * @author bviveiros
 *
 * A Test Suite has a Name, Description, Last Executed Date etc.  A Test
 * Suite is made up of a List of Test Cases.  The Test Case List is 
 * not stored within this object since these need to be queried 
 * seperately anyway.  A Test Suite can only contain one type of Test Case, 
 * which is identified in getTestSuiteType() and the class TestSuiteTypes.
 *
 * Also represents a row in the WBIAG_TST_SUITE table.
 */
public class TestSuiteData extends RecordData {

	private int suiteId = 0;
	private String suiteName = null;
	private String suiteDesc = null;
	private Date suiteExecuteDate = null;
	private String suiteType = null;
	private Date suiteCreatedDate = null;
	private Date suiteUpdatedDate = null;
	private String suiteCreatorName = null;
	

	
	public Date getSuiteCreatedDate() {
		return suiteCreatedDate;
	}
	public void setSuiteCreatedDate(Date suiteCreatedDate) {
		this.suiteCreatedDate = suiteCreatedDate;
	}
	public String getSuiteCreatorName() {
		return suiteCreatorName;
	}
	public void setSuiteCreatorName(String suiteCreatorName) {
		this.suiteCreatorName = suiteCreatorName;
	}
	public String getSuiteDesc() {
		return suiteDesc;
	}
	public void setSuiteDesc(String suiteDesc) {
		this.suiteDesc = suiteDesc;
	}
	public int getSuiteId() {
		return suiteId;
	}
	public void setSuiteId(int suiteId) {
		this.suiteId = suiteId;
	}
	public Date getSuiteExecuteDate() {
		return suiteExecuteDate;
	}
	public void setSuiteExecuteDate(Date suiteExecuteDate) {
		this.suiteExecuteDate = suiteExecuteDate;
	}
	public Date getSuiteUpdatedDate() {
		return suiteUpdatedDate;
	}
	public void setSuiteUpdatedDate(Date suiteUpdatedDate) {
		this.suiteUpdatedDate = suiteUpdatedDate;
	}
	public String getSuiteName() {
		return suiteName;
	}
	public void setSuiteName(String suiteName) {
		this.suiteName = suiteName;
	}
	public String getSuiteType() {
		return suiteType;
	}
	public void setSuiteType(String suiteType) {
		this.suiteType = suiteType;
	}
	/* (non-Javadoc)
	 * @see com.workbrain.app.ta.model.RecordData#newInstance()
	 */
	public RecordData newInstance() {
		return new TestSuiteData();
	}

}