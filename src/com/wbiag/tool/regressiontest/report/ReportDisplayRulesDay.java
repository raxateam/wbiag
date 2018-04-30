package com.wbiag.tool.regressiontest.report;

import java.util.Date;
import java.util.List;

/**
 * @author bviveiros
 *
 * A test case can span multiple days.  
 * This is the result of one of the days.
 */
public class ReportDisplayRulesDay {

	private int status = 0;
	private Date workSummaryDate = null;
	private String message = null;

	private List workSummaryColumns = null;
	private List workSummaryExpectedData = null;
	private List workSummaryActualData = null;
	
	private List workDetailColumns = null;
	private List workDetailExpectedData = null;
	private List workDetailActualData = null;
	
	private List workPremiumColumns = null;
	private List workPremiumExpectedData = null;
	private List workPremiumActualData = null;
	
	private List employeeBalanceColumns = null;
	private List employeeBalanceExpectedData = null;
	private List employeeBalanceActualData = null;

	
	
	public List getWorkSummaryActualData() {
		return workSummaryActualData;
	}
	public void setWorkSummaryActualData(List workSummaryActualData) {
		this.workSummaryActualData = workSummaryActualData;
	}
	public List getWorkSummaryColumns() {
		return workSummaryColumns;
	}
	public void setWorkSummaryColumns(List workSummaryColumns) {
		this.workSummaryColumns = workSummaryColumns;
	}
	public List getWorkSummaryExpectedDataExpectedData() {
		return workSummaryExpectedData;
	}
	public void setWorkSummaryExpectedData(List workSummaryExpectedData) {
		this.workSummaryExpectedData = workSummaryExpectedData;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public int getStatus() {
		return status;
	}
	public void setStatus(int status) {
		this.status = status;
	}
	public Date getWorkSummaryDate() {
		return workSummaryDate;
	}
	public void setWorkSummaryDate(Date workSummaryDate) {
		this.workSummaryDate = workSummaryDate;
	}
	public List getEmployeeBalanceActualData() {
		return employeeBalanceActualData;
	}
	public void setEmployeeBalanceActualData(List employeeBalanceActualData) {
		this.employeeBalanceActualData = employeeBalanceActualData;
	}
	public List getEmployeeBalanceColumns() {
		return employeeBalanceColumns;
	}
	public void setEmployeeBalanceColumns(List employeeBalanceColumns) {
		this.employeeBalanceColumns = employeeBalanceColumns;
	}
	public List getEmployeeBalanceExpectedData() {
		return employeeBalanceExpectedData;
	}
	public void setEmployeeBalanceExpectedData(List employeeBalanceExpectedData) {
		this.employeeBalanceExpectedData = employeeBalanceExpectedData;
	}
	public List getWorkDetailActualData() {
		return workDetailActualData;
	}
	public void setWorkDetailActualData(List workDetailActualData) {
		this.workDetailActualData = workDetailActualData;
	}
	public List getWorkDetailColumns() {
		return workDetailColumns;
	}
	public void setWorkDetailColumns(List workDetailColumns) {
		this.workDetailColumns = workDetailColumns;
	}
	public List getWorkDetailExpectedData() {
		return workDetailExpectedData;
	}
	public void setWorkDetailExpectedData(List workDetailExpectedData) {
		this.workDetailExpectedData = workDetailExpectedData;
	}
	public List getWorkPremiumActualData() {
		return workPremiumActualData;
	}
	public void setWorkPremiumActualData(List workPremiumActualData) {
		this.workPremiumActualData = workPremiumActualData;
	}
	public List getWorkPremiumColumns() {
		return workPremiumColumns;
	}
	public void setWorkPremiumColumns(List workPremiumColumns) {
		this.workPremiumColumns = workPremiumColumns;
	}
	public List getWorkPremiumExpectedData() {
		return workPremiumExpectedData;
	}
	public void setWorkPremiumExpectedData(List workPremiumExpectedData) {
		this.workPremiumExpectedData = workPremiumExpectedData;
	}
	public List getWorkSummaryExpectedData() {
		return workSummaryExpectedData;
	}
}
