package com.wbiag.app.modules.retailSchedule.errorDetection;

import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import com.workbrain.app.modules.retailSchedule.exceptions.RetailException;
import com.workbrain.app.modules.retailSchedule.model.Employee;
import com.workbrain.app.ta.db.CodeMapper;
import com.workbrain.sql.DBConnection;

public class EmployeeShiftPatternsRule extends ErrorDetectionRule {

	private static Logger logger = Logger.getLogger(EmployeeShiftPatternsRule.class);

	public Integer getRuleType(){
		return SCHEDULE_TYPE;
	}
	
	public ErrorDetectionScriptResult detect(ErrorDetectionContext context) {
		
		List allEmployees = context.getAllEmployeesList();
		DBConnection conn = context.getConnection();
		CodeMapper cm = context.getCodeMapper();
		
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Employee Shift Patterns");
		actionResult.setHelpTip("Ensure that employees on fixed shift patterns have shifts specified");
		actionResult.setHelpDesc("Employees that are specified to work ‘fixed shifts’ must have fixed shifts defined to be properly accepted and scheduled by the system. The script will check for this requirement and report of any error if finds. The script will not halt execution upon discovery of such an error.");
		actionResult.setErrorMsg("FAILED: The following employee(s) have no assigned shifts for the 'Fixed Shift' option: ");
		
		String result = new String("");
		
		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.3.6...");
		
		final int NOT_ASSIGNED = -1;
		final int NULL_ALL_OFFS1 = 0;
		final int NULL_ALL_OFFS2 = 10;

		for(Iterator empIter = allEmployees.iterator(); empIter.hasNext();)
		{
			try {
				Employee employee = ( Employee )empIter.next();
				if (employee.getEmpId() != -3) {
				     // Check if Employee is on fixed schedule
				     if( employee.getSempOnfixedSkd() == 1 )
				     {
				         com.workbrain.app.ta.db.EmployeeAccess empAcc = new com.workbrain.app.ta.db.EmployeeAccess( conn, cm );
				         com.workbrain.app.ta.model.EmployeeData empData = empAcc.loadRawData( employee.getEmpId() );
	
				         // Check if this employee have a valid fixed shift (not NULL OFFS)
				         if( empData.getShftpatId() == NOT_ASSIGNED ||
				             empData.getShftpatId() == NULL_ALL_OFFS1 ||
				             empData.getShftpatId() == NULL_ALL_OFFS2 )
				         {
					         if( result.length() == 0 )
					             result += getEmpInfo( conn, cm, employee.getEmpId() );
					         else
					             result += ", " + getEmpInfo( conn, cm, employee.getEmpId() );
					     }
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
			logger.debug("Finished check 4.3.6");
		
		actionResult.setMessage(result);
		return actionResult;
	}

	protected String getLocalizedRuleName() {
		return "Employee Shift Patterns";
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
