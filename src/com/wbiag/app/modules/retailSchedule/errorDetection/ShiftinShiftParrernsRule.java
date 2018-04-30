package com.wbiag.app.modules.retailSchedule.errorDetection;

import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import com.workbrain.app.modules.retailSchedule.exceptions.RetailException;
import com.workbrain.app.modules.retailSchedule.model.Employee;
import com.workbrain.app.ta.model.EmployeeData;
import com.workbrain.app.ta.db.CodeMapper;
import com.workbrain.sql.DBConnection;

public class ShiftinShiftParrernsRule extends ErrorDetectionRule {
	

	private static Logger logger = Logger.getLogger(ShiftinShiftParrernsRule.class);
	  
	public Integer getRuleType(){
		return SCHEDULE_TYPE;
	}
	
	public ErrorDetectionScriptResult detect(ErrorDetectionContext context) {
		
		List allEmployees = context.getAllEmployeesList();
		DBConnection conn = context.getConnection();
		CodeMapper cm = context.getCodeMapper();
		
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Shifts in Shift Patterns");
		actionResult.setHelpTip("Shift patterns must have shifts defined");
		actionResult.setHelpDesc("Empty shift patterns create errors in Schedule generation. The method must ensure that there are no empty shift Patterns assigned in the system that are linked to any of the employees in the Staffing Group selected by the user.");
		actionResult.setErrorMsg("FAILED: The following employee(s) does not have a shift pattern assigned: ");
		
		String result = new String("");
		
		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.3.10...");
		
		for(Iterator empIter = allEmployees.iterator(); empIter.hasNext();)
		{
			try {
				Employee employee = ( Employee )empIter.next();
				if (employee.getEmpId() != -3) {
					com.workbrain.app.ta.db.EmployeeAccess empAcc = new com.workbrain.app.ta.db.EmployeeAccess( conn, cm );
					EmployeeData employeeData = empAcc.loadRawData( employee.getEmpId() );
	
					// Check if it's "NOT ASSIGNED" shift
					if( employeeData.getShftpatId() == -1 )
					{
						if( result.length() == 0 )
							result += getEmpInfo( conn, cm, employee.getEmpId() );
						else
							result += ", " + getEmpInfo( conn, cm, employee.getEmpId() );
					}
				} else {
	                if (logger.isDebugEnabled())
	                	logger.debug("Workbrain Admin should not be in scheduled group");
	                    
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
			logger.debug("Finished check 4.3.10");
		
		actionResult.setMessage(result);
		return actionResult;
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

	protected String getLocalizedRuleName() {
		return "Shifts in Shift Patterns";
	}

}
