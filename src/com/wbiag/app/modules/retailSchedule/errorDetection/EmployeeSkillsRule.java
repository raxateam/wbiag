package com.wbiag.app.modules.retailSchedule.errorDetection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import com.workbrain.app.modules.retailSchedule.exceptions.RetailException;
import com.workbrain.app.modules.retailSchedule.model.Employee;
import com.workbrain.sql.SQLHelper;
import com.workbrain.sql.DBConnection;
import com.workbrain.app.ta.db.CodeMapper;

public class EmployeeSkillsRule extends ErrorDetectionRule {

	private static Logger logger = Logger.getLogger(EmployeeSkillsRule.class);
	
	public Integer getRuleType(){
		return BUG_TYPE;
	}
	
	public ErrorDetectionScriptResult detect(ErrorDetectionContext context) {
		
		List allEmployees = context.getAllEmployeesList();
		DBConnection conn = context.getConnection();
		CodeMapper cm = context.getCodeMapper();
		
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Employee Skills");
		actionResult.setHelpTip("Ensures that employees have at least one skill selected, and no duplicate skills");
		actionResult.setHelpDesc("Employees must have at least one skill selected to be considered for positions in the Scheduling process. The current fix that is used in the system is that there is a no_skill skill created and assigned to employees. If there is no skill assigned to employees, an error will be generated but the script will not halt execution. Finally, no employee should have a skill selected more than once.");
		actionResult.setErrorMsg("WARNING: ");
		
		String result = new String("");
		
		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.1.2...");
		
		if( allEmployees.size() == 0 )
	        result = "No Employees found at this location\n";

		PreparedStatement stmt = null;
		ResultSet rs = null;
		
		for( Iterator iter = allEmployees.iterator(); iter.hasNext(); )
		{
			try {
				Employee employee = ( Employee )iter.next();
				if (employee.getEmpId() != -3){
	        
					String strSelect = "select * from ST_EMP_SKILL where EMP_ID = ? ";
					
					stmt = conn.prepareStatement( strSelect );
					stmt.setInt(1 , employee.getEmpId() ) ;
					rs = stmt.executeQuery();
	            
					if( !rs.next() )
						result += "-No skills selected for " + getEmpInfo( conn, cm, employee.getEmpId() ) + "!\n";
					
					strSelect = "SELECT * ";
					strSelect += "FROM (SELECT COUNT(ES.STSKL_ID) AS C, ES.EMP_ID, S.STSKL_NAME ";
					strSelect += "FROM ST_EMP_SKILL ES, ST_SKILL S ";
					strSelect += "WHERE ES.EMP_ID = ? AND ES.STSKL_ID = S.STSKL_ID ";
					strSelect += "GROUP BY S.STSKL_NAME, ES.EMP_ID) ";
					strSelect += "WHERE C >= 2 ";
					
					stmt = conn.prepareStatement( strSelect );
					stmt.setInt(1 , employee.getEmpId() ) ;
					rs = stmt.executeQuery();
					
					while(rs.next())
						result += "-Employee " + getEmpInfo( conn, cm, employee.getEmpId() ) + " has skill " + rs.getString("STSKL_NAME") + " defined " + rs.getInt("C") + " times!\n";
						
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
			catch(SQLException e) {
				result = e.getMessage();
			}

		    finally
		    {
		        SQLHelper.cleanUp(rs);
		        SQLHelper.cleanUp(stmt);
		    }
		}
		
		if(result.compareTo("") == 0)
			result = "OK";
		
		if(logger.isDebugEnabled())
			logger.debug("Finished check 4.1.2");
		
		actionResult.setMessage(result);
		return actionResult;
	}

	protected String getLocalizedRuleName() {

		return "Employee Skills";
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
