package com.wbiag.app.modules.retailSchedule.errorDetection;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import com.workbrain.app.modules.retailSchedule.exceptions.RetailException;
import com.workbrain.app.modules.retailSchedule.model.Employee;
import com.workbrain.app.ta.model.EmployeeJobData;
import com.workbrain.app.ta.db.CodeMapper;
import com.workbrain.util.StringHelper;
import com.workbrain.sql.DBConnection;

public class EmployeePreferredJobRule extends ErrorDetectionRule {


	private static Logger logger = Logger.getLogger(EmployeePreferredJobRule.class);
	  
	public Integer getRuleType(){
		return SCHEDULE_TYPE;
	}
	
	public ErrorDetectionScriptResult detect(ErrorDetectionContext context) {
		
		List allEmployees = context.getAllEmployeesList();
		DBConnection conn = context.getConnection();
		CodeMapper cm = context.getCodeMapper();
		
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Employee Preferred Job");
		actionResult.setHelpTip("Employee must have one and only one preferred job");
		actionResult.setHelpDesc("The system requires for there to be a selection of a preferred job for each employee. This must be checked to ensure that one and only one selection is made. The script will check for this requirement and report of any error if finds. The script will not halt execution upon discovery of such an error.");
		actionResult.setErrorMsg("WARNING: The following employee(s) should only have 1 and only 1 preferred job defined OR they have employee jobs with employee_job.empjob_preffered column as NULL (i.e should be set to 'N'): ");
		
		String result = new String("");
		
		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.3.7...");
		
		int iPrefPos = 0;
		Date preferredJobEndDate = null;
		
		for(Iterator iter = allEmployees.iterator(); iter.hasNext();)
		{
			try {
				iPrefPos = 0;
				Employee employee = (Employee)iter.next();
				if (employee.getEmpId() != -3) {
					for(Iterator posIter = employee.getEmployeeJobList().iterator(); posIter.hasNext();)
					{
						EmployeeJobData empJob = (EmployeeJobData)posIter.next();
						// *** null EmpjobPreferred causes problems
						if (StringHelper.isEmpty(empJob.getEmpjobPreferred())) {
							iPrefPos = 0;
						}
						else if(empJob.getEmpjobPreferred().equalsIgnoreCase("Y")) {
							if(preferredJobEndDate == null)
								iPrefPos++;
							else if(preferredJobEndDate.after(empJob.getEmpjobStartDate()))
								iPrefPos++;
							else //preferred jobs do not overlap
							{
//								do nothing
							}
								
							preferredJobEndDate = empJob.getEmpjobEndDate();
						}
					}
					if( iPrefPos != 1 )
					{
						if( result.length() == 0 )
							result += getEmpInfo( conn, cm, employee.getEmpId() );
						else
							result += ", " + getEmpInfo( conn, cm, employee.getEmpId() );
					}
				} else {
					if (logger.isDebugEnabled()){
						logger.debug("Workbrain Admin should not be in scheduled group");
                    }
                    result += ", Workbrain Admin Should not be in scheduled group";
             	}
			}
			catch(RetailException e) {
				result = e.getMessage();
			}
		}
		
		if(result.compareTo("") == 0)
			result = "OK";
		
		if(logger.isDebugEnabled())
			logger.debug("Finished check 4.3.7");
		
		actionResult.setMessage(result);
		return actionResult;
	}

	protected String getLocalizedRuleName() {

		return "Employee Preferred Job";
	}
	
	 private String getEmpInfo( DBConnection conn, CodeMapper cm, int empId )
	    {
	        // Standardize the Employee info to this format: Lastname, Firstname (EmpID)
		    if (logger.isDebugEnabled()){
				logger.debug("getEmpInfo() - Checking Emp ID: " + empId);
		    }
		    com.workbrain.app.ta.db.EmployeeAccess empAcc = new com.workbrain.app.ta.db.EmployeeAccess( conn, cm );
		    com.workbrain.app.ta.model.EmployeeData empData = empAcc.loadRawData( empId );
	          String result = null;
		    if (empData == null) {
				result = "Employee Not Found (id: " +  empId+ ")";
		    }
	          else {
			     result = empData.getEmpLastname() + ", " + empData.getEmpFirstname() + " (" + empId + ")";
	          }
		    if (logger.isDebugEnabled()){
				logger.debug("getEmpInfo() - Found Emp Info: " + result);
		    }
			return result;
	    }

}
