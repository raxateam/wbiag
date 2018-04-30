package com.wbiag.app.jsp.action.biweeklytimesheet;

import java.util.Date;

import com.workbrain.app.jsp.action.timesheet.EmployeeModel;

public class CSMEmployeeModel extends EmployeeModel implements Cloneable, java.io.Serializable {
    private String employeePct;	//emp_val2
    private String workgroup;	//emp_val3
    private String schedule;	//global registry value
    private String deptNo;		//emp_val6
    private String jobClass;	//emp_job's name (workbrain Job, not PIPs Job)
    private String jobClassName;//emp_job's description (workbrain Job, not PIPs Job)
    private String type;		//emp_val4
    private String dayStartTime;
    private Date payGroupStartDate;
    private Date payGroupEndDate;

    public Date getPayGroupEndDate() {
		return payGroupEndDate;
	}

	public void setPayGroupEndDate(Date payGroupEndDate) {
		this.payGroupEndDate = payGroupEndDate;
	}

	public Date getPayGroupStartDate() {
		return payGroupStartDate;
	}

	public void setPayGroupStartDate(Date payGroupStartDate) {
		this.payGroupStartDate = payGroupStartDate;
	}

	public String getDeptNo() {
		return deptNo;
	}

	public void setDeptNo(String deptNo) {
		this.deptNo = deptNo;
	}

	public String getEmployeePct() {
		return employeePct;
	}

	public void setEmployeePct(String employeePct) {
		this.employeePct = employeePct;
	}

	public String getJobClass() {
		return jobClass;
	}

	public void setJobClass(String jobClass) {
		this.jobClass = jobClass;
	}

	public String getSchedule() {
		return schedule;
	}

	public void setSchedule(String schedule) {
		this.schedule = schedule;
	}

	public String getWorkgroup() {
		return workgroup;
	}

	public void setWorkgroup(String workgroup) {
		this.workgroup = workgroup;
	}

	public Object clone() throws CloneNotSupportedException {
    	CSMEmployeeModel em = (CSMEmployeeModel)super.clone();
        return em;
    }

	public String getJobClassName() {
		return jobClassName;
	}

	public void setJobClassName(String jobClassName) {
		this.jobClassName = jobClassName;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getDayStartTime() {
		return dayStartTime;
	}

	public void setDayStartTime(String dayStartTime) {
		this.dayStartTime = dayStartTime;
	}


}
