package com.wbiag.tool.regressiontest.jsp;

import java.io.Serializable;

/**
 * @author bviveiros
 *
 * A bean that represents the Pay Rules Test Case parameters
 * on the JSP.
 */
public class RulesCaseParamsBean implements Serializable {

	private String rulesCaseId = null;
	private String testSuiteId = null;
	private String name = null;
	private String description = null;
	private String startDate = null;
	private String endDate = null;
	private String outputAttrib = null;

	private String wbuId = null;
	private String empIds = null;
	private String empNames = null;
	private String teamIds = null;
	private String payGroupIds = null;
	private String calcGroupIds = null;
	private String shiftIds = null;
	private String includeSubTeams = null;
	
	public String getCalcGroupIds() {
		return calcGroupIds;
	}
	public void setCalcGroupIds(String calcGroupIds) {
		this.calcGroupIds = calcGroupIds;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getEmpIds() {
		return empIds;
	}
	public void setEmpIds(String empIds) {
		this.empIds = empIds;
	}
	public String getEmpNames() {
		return empNames;
	}
	public void setEmpNames(String empNames) {
		this.empNames = empNames;
	}
	public String getEndDate() {
		return endDate;
	}
	public void setEndDate(String endDate) {
		this.endDate = endDate;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getOutputAttrib() {
		return outputAttrib;
	}
	public void setOutputAttrib(String outputAttrib) {
		this.outputAttrib = outputAttrib;
	}
	public String getPayGroupIds() {
		return payGroupIds;
	}
	public void setPayGroupIds(String payGroupIds) {
		this.payGroupIds = payGroupIds;
	}
	public String getRulesCaseId() {
		return rulesCaseId;
	}
	public void setRulesCaseId(String rulesCaseId) {
		this.rulesCaseId = rulesCaseId;
	}
	public String getShiftIds() {
		return shiftIds;
	}
	public void setShiftIds(String shiftIds) {
		this.shiftIds = shiftIds;
	}
	public String getStartDate() {
		return startDate;
	}
	public void setStartDate(String startDate) {
		this.startDate = startDate;
	}
	public String getTeamIds() {
		return teamIds;
	}
	public void setTeamIds(String teamIds) {
		this.teamIds = teamIds;
	}
	public String getTestSuiteId() {
		return testSuiteId;
	}
	public void setTestSuiteId(String testSuiteId) {
		this.testSuiteId = testSuiteId;
	}
	public String getWbuId() {
		return wbuId;
	}
	public void setWbuId(String wbuId) {
		this.wbuId = wbuId;
	}
	public String getIncludeSubTeams() {
		return includeSubTeams;
	}
	public void setIncludeSubTeams(String includeSubTeams) {
		this.includeSubTeams = includeSubTeams;
	}
}
