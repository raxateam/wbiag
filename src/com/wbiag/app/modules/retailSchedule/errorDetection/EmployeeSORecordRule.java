package com.wbiag.app.modules.retailSchedule.errorDetection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import com.workbrain.sql.SQLHelper;
import com.workbrain.sql.DBConnection;

public class EmployeeSORecordRule extends ErrorDetectionRule {

	private static Logger logger = Logger.getLogger(EmployeeSORecordRule.class);
	
	public Integer getRuleType(){
		return BUG_TYPE;
	}
	
	public ErrorDetectionScriptResult detect(ErrorDetectionContext context) {
		
		DBConnection conn = context.getConnection();
		
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Employees with more than one SO_EMPLOYEE record");
		actionResult.setHelpTip("Employees should only have one SO_EMPLOYEE record");
		actionResult.setHelpDesc("Such employees might cause the schedule to come up with empty employee/time slots.");
		actionResult.setErrorMsg("FATAL ERROR: The following employees have duplicate records:\n");
		
		String result = new String("");
		
		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.1.4...");
		
		PreparedStatement stmt = null;
		ResultSet rs = null;
		
		try {
			String strSelect = "SELECT * FROM (";
			strSelect += "SELECT COUNT(SO_EMPLOYEE.EMP_ID) AS C, EMPLOYEE.EMP_FIRSTNAME, EMPLOYEE.EMP_LASTNAME ";
			strSelect += "FROM SO_EMPLOYEE JOIN EMPLOYEE ON SO_EMPLOYEE.EMP_ID = EMPLOYEE.EMP_ID ";
			strSelect += "GROUP BY SO_EMPLOYEE.EMP_ID, EMPLOYEE.EMP_FIRSTNAME, EMPLOYEE.EMP_LASTNAME";
			strSelect += ") WHERE C > 1";
			
			stmt = conn.prepareStatement(strSelect);
            rs = stmt.executeQuery();
			
			while (rs.next()) {
				result += rs.getInt("C") + " SO_EMPLOYEE records for " + rs.getString("EMP_FIRSTNAME") + " " + rs.getString("EMP_LASTNAME") + "\n";
			}
		}
		catch(SQLException e) {
			SQLHelper.cleanUp(rs);
			SQLHelper.cleanUp(stmt);
			result = "QUERY FAILED!\n" + e.getMessage();
		}
	    finally
	    {
	        SQLHelper.cleanUp(rs);
	        SQLHelper.cleanUp(stmt);
	    }
		
		if(result.compareTo("") == 0)
			result = "OK";
		else
			actionResult.setFatalError(true);
		
		if(logger.isDebugEnabled())
			logger.debug("Finished check 4.1.4");
		
		actionResult.setMessage(result);
		return actionResult;
	}

	protected String getLocalizedRuleName() {
		return "Employees with more than one SO_EMPLOYEE record";
	}

}
