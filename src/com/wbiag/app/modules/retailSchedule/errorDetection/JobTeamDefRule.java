package com.wbiag.app.modules.retailSchedule.errorDetection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import com.workbrain.sql.SQLHelper;
import com.workbrain.sql.DBConnection;

public class JobTeamDefRule extends ErrorDetectionRule {

	private static Logger logger = Logger.getLogger(JobTeamDefRule.class);
	  
	public Integer getRuleType(){
		return SCHEDULE_TYPE;
	}
	
	public ErrorDetectionScriptResult detect(ErrorDetectionContext context) {
		
		DBConnection conn = context.getConnection();
		
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Job Team Definitions");
		actionResult.setHelpTip("Each job used for scheduling must have a job team associated with it that defines such things as the overtime thresholds. Missing definitions will cause stack trace errors.");
		actionResult.setHelpDesc("Each job used for scheduling must have a job team associated with it that defines such things as the overtime thresholds. Missing definitions will cause stack trace errors.");
		actionResult.setErrorMsg("FAILED: The following warning(s) have been produced:\n");
		
		String result = new String("");
		
		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.3.15...");
		
		PreparedStatement stmt = null;
		ResultSet rs = null;
		
		try {
			String strSelect = "SELECT JOB_ID, JOB_NAME FROM JOB WHERE JOB_ID NOT IN (SELECT JOB.JOB_ID FROM JOB JOIN JOB_TEAM ON JOB.JOB_ID=JOB_TEAM.JOB_ID) AND JOB_ID >= 10000 ORDER BY JOB_ID";
			stmt = conn.prepareStatement(strSelect);
            rs = stmt.executeQuery();
        	
        	while(rs.next()) {
        		result += "WARNING: Job \"" + rs.getString("JOB_NAME") + "\" does not have a job team record.\n";
        	}
		}
		catch(SQLException e) {
			logger.error(e.getMessage());
		}
	    finally
	    {
	        SQLHelper.cleanUp(rs);
	        SQLHelper.cleanUp(stmt);
	    }
		
		if(result.compareTo("") == 0)
			result = "OK";
		
		if(logger.isDebugEnabled())
			logger.debug("Finished check 4.3.15");
		
		actionResult.setMessage(result);
		return actionResult;
	}

	protected String getLocalizedRuleName() {
		return "Job Team Definitions";
	}

}
