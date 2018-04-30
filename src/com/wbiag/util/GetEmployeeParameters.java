package com.wbiag.util;

import java.util.Date;

/**
 * @author bviveiros
 * 
 * Parameters for querying employees.
 */
public class GetEmployeeParameters {

    public final static int
            ORDER_BY_EMP_NAME = 0,
            ORDER_BY_LAST_NAME = 1,
            ORDER_BY_FIRST_NAME = 2;

    private int wbuId = -1;
	private String empIds = null; 
	private String teamIds = null;
	private String payGroupIds = null; 
	private String calcGroupIds = null;
	private String shiftIds = null;	
	private Date startDate = null;
	private Date endDate = null;
	private boolean includeSubTeams = false;
	private int orderBy = ORDER_BY_EMP_NAME;
	
	public boolean getIncludeSubTeams() {
		return includeSubTeams;
	}
	public void setIncludeSubTeams(boolean includeSubTeams) {
		this.includeSubTeams = includeSubTeams;
	}
	public String getCalcGroupIds() {
		return calcGroupIds;
	}
	public void setCalcGroupIds(String calcGroupIds) {
		this.calcGroupIds = calcGroupIds;
	}
	public String getEmpIds() {
		return empIds;
	}
	public void setEmpIds(String empIds) {
		this.empIds = empIds;
	}
	public Date getEndDate() {
		return endDate;
	}
	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}
	public String getPayGroupIds() {
		return payGroupIds;
	}
	public void setPayGroupIds(String payGroupIds) {
		this.payGroupIds = payGroupIds;
	}
	public String getShiftIds() {
		return shiftIds;
	}
	public void setShiftIds(String shiftIds) {
		this.shiftIds = shiftIds;
	}
	public Date getStartDate() {
		return startDate;
	}
	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}
	public String getTeamIds() {
		return teamIds;
	}
	public void setTeamIds(String teamIds) {
		this.teamIds = teamIds;
	}
	public int getWbuId() {
		return wbuId;
	}
	public void setWbuId(int wbuId) {
		this.wbuId = wbuId;
	}
	public int getOrderBy() {
		return orderBy;
	}
	public void setOrderBy(int orderBy) {
		this.orderBy = orderBy;
	}
}
