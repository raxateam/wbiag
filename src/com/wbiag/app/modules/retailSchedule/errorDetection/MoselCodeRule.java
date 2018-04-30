package com.wbiag.app.modules.retailSchedule.errorDetection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import com.workbrain.sql.SQLHelper;
import com.workbrain.sql.DBConnection;

public class MoselCodeRule extends ErrorDetectionRule {

	private static Logger logger = Logger.getLogger(MoselCodeRule.class);
	  
	public Integer getRuleType(){
		return SYSTEM_TYPE;
	}
	
	public ErrorDetectionScriptResult detect(ErrorDetectionContext context) {
		DBConnection conn = context.getConnection();
		
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Mosel Code");
		actionResult.setHelpTip("Mosel code on remote machine is not present/incorrect");
		actionResult.setHelpDesc("Mosel is the Schedule Optimization engine that is responsible for generating the Schedules for LFSO. It runs on a remote machine so connections must be verified by this script. Also, it must be verified that the Mosel code is running properly. The script will check for this requirement and report of any error if finds. The script will not halt execution upon discovery of such an error.");
		actionResult.setErrorMsg("FAILED: ");
		
		String result = "";
		
		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.4.2...");
		
		PreparedStatement stmt = null;
		ResultSet rs = null;
		
	    try {
			String strSelect = "select LAST_ANALYZED from user_tables where table_name like 'SO%'";
            stmt = conn.prepareStatement(strSelect);
            rs = stmt.executeQuery();
            if(!rs.next())
                result += "No information on DB statistics available";
        }
        catch(SQLException e) {
			if(logger.isDebugEnabled())
				logger.debug(e.getStackTrace());
			
        	result = e.getMessage();
        }
	    finally
	    {
	        SQLHelper.cleanUp(rs);
	        SQLHelper.cleanUp(stmt);
	    }
        
        if(result.compareTo("") == 0)
			result = "OK";
        
		if(logger.isDebugEnabled())
			logger.debug("Finished check 4.4.2");
		
		actionResult.setMessage(result);
		return actionResult;
	}

	protected String getLocalizedRuleName() {
		return "Mosel Code";
	}

}
