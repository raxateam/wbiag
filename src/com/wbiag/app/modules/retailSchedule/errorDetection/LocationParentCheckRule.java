package com.wbiag.app.modules.retailSchedule.errorDetection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import com.workbrain.sql.SQLHelper;
import com.workbrain.sql.DBConnection;

public class LocationParentCheckRule extends ErrorDetectionRule {

	private static Logger logger = Logger.getLogger(LocationParentCheckRule.class);
	
	public Integer getRuleType(){
		return FORECAST_TYPE;
	}
	
	public ErrorDetectionScriptResult detect(ErrorDetectionContext context) {
		DBConnection conn = context.getConnection();
		
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Location parent check");
		actionResult.setHelpTip("Checks to ensure a location's parent is not itself");
		actionResult.setHelpDesc("If a location's parent is set to itself, an endless loop could be entered causing a stack overflow. This check will ensure no location except the root location has itself as it's parent");
		actionResult.setErrorMsg("FATAL ERROR: The following locations have their parent location set to themself\n");
		
		String result = new String("");
		
		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.1.6...");
		
		PreparedStatement stmt = null;
		ResultSet rs = null;
		
		try {
			String strSelect = "SELECT SKDGRP_NAME ";
			strSelect += "FROM SO_SCHEDULE_GROUP ";
			strSelect += "WHERE SKDGRP_ID = SKDGRP_PARENT_ID ";
			strSelect += "AND SKDGRP_ID >1 ";
			
			stmt = conn.prepareStatement(strSelect);
			rs = stmt.executeQuery();
			
			while(rs.next())
				result += "-" + rs.getString("SKDGRP_NAME") + "\n";
		}
		catch(SQLException e) {
			logger.error(e);
			result = e.getMessage();
		}
		finally {
			SQLHelper.cleanUp(stmt, rs);
		}
		
		if(result.compareTo("") == 0)
			result = "OK";
		else
			actionResult.setFatalError(true);
		
        if(logger.isDebugEnabled())
			logger.debug("Finished check 4.1.6");
		
		actionResult.setMessage(result);
		return actionResult;
	}

	protected String getLocalizedRuleName() {
		return "Location parent check";
	}

}
