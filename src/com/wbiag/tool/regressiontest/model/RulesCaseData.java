package com.wbiag.tool.regressiontest.model;

import java.util.Date;

import com.workbrain.app.ta.model.RecordData;

/**
 * @author bviveiros
 *
 * Represents a row in the WBIAG_RULES_CASE table.
 */
public class RulesCaseData extends RecordData implements ITestCaseData  {

	private int caseId = 0;
	private int suiteId = 0;
	private int empId = 0;
	private String caseName = null;
	private String caseDesc = null;
	private Date caseStartDate = null;
	private Date caseEndDate = null;
	private String caseOutputAttrib = null;
	private String caseExpectResult = null;
	private Date caseCreatedDate = null;
	private Date caseUpdatedDate = null;
	private String employeeName = null;

	// Cannot use get/set for empName because RecordAccess will assume
	// that it's a column in the db table.
	public String empName() {
		return this.employeeName;
	}
	public void empName(String empName) {
		this.employeeName = empName;
	}
	
	
	public int getEmpId() {
		return empId;
	}
	public void setEmpId(int empId) {
		this.empId = empId;
	}
	public Date getCaseCreatedDate() {
		return caseCreatedDate;
	}
	public void setCaseCreatedDate(Date caseCreatedDate) {
		this.caseCreatedDate = caseCreatedDate;
	}
	public String getCaseDesc() {
		return caseDesc;
	}
	public void setCaseDesc(String caseDesc) {
		this.caseDesc = caseDesc;
	}
	public Date getCaseEndDate() {
		return caseEndDate;
	}
	public void setCaseEndDate(Date caseEndDate) {
		this.caseEndDate = caseEndDate;
	}
	public String getCaseExpectResult() {
		return caseExpectResult;
	}
	public void setCaseExpectResult(String caseExpectResult) {
		this.caseExpectResult = caseExpectResult;
	}
	public int getCaseId() {
		return caseId;
	}
	public void setCaseId(int caseId) {
		this.caseId = caseId;
	}
	public Date getCaseUpdatedDate() {
		return caseUpdatedDate;
	}
	public void setCaseUpdatedDate(Date caseUpdatedDate) {
		this.caseUpdatedDate = caseUpdatedDate;
	}
	public String getCaseName() {
		return caseName;
	}
	public void setCaseName(String caseName) {
		this.caseName = caseName;
	}
	public String getCaseOutputAttrib() {
		return caseOutputAttrib;
	}
	public void setCaseOutputAttrib(String caseOutputAttrib) {
		this.caseOutputAttrib = caseOutputAttrib;
	}
	public Date getCaseStartDate() {
		return caseStartDate;
	}
	public void setCaseStartDate(Date caseStartDate) {
		this.caseStartDate = caseStartDate;
	}
	public int getSuiteId() {
		return suiteId;
	}
	public void setSuiteId(int suiteId) {
		this.suiteId = suiteId;
	}

	/* (non-Javadoc)
	 * @see com.workbrain.app.ta.model.RecordData#newInstance()
	 */
	public RecordData newInstance() {
		return new RulesCaseData();
	}

}
