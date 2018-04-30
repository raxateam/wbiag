package com.wbiag.app.modules.retailSchedule.errorDetection;

import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import com.workbrain.app.modules.availability.db.AvOvrAccess;
import com.workbrain.app.modules.availability.db.AvailabilityAccess;
import com.workbrain.app.modules.retailSchedule.exceptions.RetailException;
import com.workbrain.app.modules.retailSchedule.model.Employee;
import com.workbrain.sql.DBConnection;
import com.workbrain.app.ta.db.CodeMapper;

public class EmployeeAvailabilityRule extends ErrorDetectionRule {

	private static Logger logger = Logger.getLogger(EmployeeAvailabilityRule.class);
	  
	public Integer getRuleType(){
		return SCHEDULE_TYPE;
	}

	public ErrorDetectionScriptResult detect(ErrorDetectionContext context) {
		
		List allEmployees = context.getAllEmployeesList();
		DBConnection conn = context.getConnection();
		CodeMapper cm = context.getCodeMapper();
		
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Employee Availability");
		actionResult.setHelpTip("Ensures that employees have availability data loaded");
		actionResult.setHelpDesc("In order to be scheduled, employees must have availability loaded. This check will check all of the employees that are tied to the locations and can be scheduled to work. It will report the employees that do not have availability loaded. This error will not halt execution of script and only report a warning message.");
		actionResult.setErrorMsg("FAILED: The following employees does not have any Availability Data loaded: ");
		
		String result = new String("");
		
		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.3.2...");
		
		for(Iterator empIter = allEmployees.iterator(); empIter.hasNext();)
		{
			try {
				Employee employee = ( Employee )empIter.next();
		        if (employee.getEmpId() != -3){
		        	AvailabilityAccess availAcc = new AvailabilityAccess( conn, cm );
				    List availData = availAcc.loadRecordData( "EMP_ID = " + employee.getEmpId() );
	
				    AvOvrAccess avOvrAcc = new AvOvrAccess( conn, cm );
				    List avOvrList = avOvrAcc.loadRecordData( "EMP_ID = " + employee.getEmpId() );
	
				    if( availData.size() == 0 && avOvrList.size() == 0)
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
			logger.debug("Finished check 4.3.2");
		
		actionResult.setMessage(result);
		return actionResult;
	}

	protected String getLocalizedRuleName() {
		return "Employee Availability";
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
