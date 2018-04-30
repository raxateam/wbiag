package com.wbiag.app.modules.retailSchedule.errorDetection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import com.workbrain.app.modules.retailSchedule.exceptions.RetailException;
import com.workbrain.app.modules.retailSchedule.model.ScheduleGroupData;
import com.workbrain.app.modules.retailSchedule.type.InternalType;
import com.workbrain.sql.SQLHelper;
import com.workbrain.sql.DBConnection;
import com.workbrain.app.ta.db.CodeMapper;

public class EmployeeNoStaffGroupRule extends ErrorDetectionRule {

	private static Logger logger = Logger.getLogger(EmployeeNoStaffGroupRule.class);
	
	public Integer getRuleType(){
		return BUG_TYPE;
	}
	
	public ErrorDetectionScriptResult detect(ErrorDetectionContext context) {
		
		List corpTree =  context.getCorpTree();
		DBConnection conn = context.getConnection();
		CodeMapper cm = context.getCodeMapper();
		
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Employees with no Staff Group");
		actionResult.setHelpTip("Employees should be assigned to a staff group");
		actionResult.setHelpDesc("This simple test will allow for users of the LFSO system to see the employees that have not been added to a Staffing group, and therefore cannot and will not be shown on the schedule.");
		actionResult.setErrorMsg("FAILED: The following employee(s) have no Staff Group: ");
		
		String result = new String("");
		
		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.1.3...");
		
		if(corpTree !=null){
			PreparedStatement stmt = null;
			ResultSet rs = null;
			
    		for(Iterator iter = corpTree.iterator(); iter.hasNext(); )
    		{
    			try {
    				ScheduleGroupData tempSkdgrpData = ScheduleGroupData.getScheduleGroupData(new Integer((String)iter.next()));
    				int type = tempSkdgrpData.getSkdgrpIntrnlType();
    				
    				if( InternalType.isDriver(new Integer(type)) &&
    						tempSkdgrpData.getSkdgrpVolsub() != -1 )
    				{
    				
    					String strSelect = "select emp_id from so_employee where empgrp_id not in (select empgrp_id from so_employee_group) ";
    					strSelect += "and skdgrp_id = ? ";
    					
    					stmt = conn.prepareStatement(strSelect);
                        stmt.setInt(1, tempSkdgrpData.getSkdgrpId().intValue());
    					rs = stmt.executeQuery();
    					
    					while (rs.next())
    					{
    						int tempEmpId = rs.getInt( "emp_id" );

            			    if( result.length() == 0 )
            			        result += getEmpInfo( conn, cm, tempEmpId );
            			    else
            			        result += ", " + getEmpInfo( conn, cm, tempEmpId );
    					}
    				}
    			}
    			catch(SQLException e) {
    				result = e.getMessage();
    			}
    			catch(RetailException e) {
    				result = e.getMessage();
    			}

    		    finally
    		    {
    		        SQLHelper.cleanUp(rs);
    		        SQLHelper.cleanUp(stmt);
    		    }
        	}
    	}
		
		if(result.compareTo("") == 0)
			result = "OK";
		
		if(logger.isDebugEnabled())
			logger.debug("Finished check 4.1.3");
		
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
		
		return "Employees with no Staff Group";
	}

}
