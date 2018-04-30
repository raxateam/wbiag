package com.wbiag.app.modules.retailSchedule.errorDetection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import com.workbrain.sql.SQLHelper;
import com.workbrain.sql.DBConnection;;

public class DuplicatePOSRule extends ErrorDetectionRule {
	
	private static Logger logger = Logger.getLogger(DuplicatePOSRule.class);
	
	public Integer getRuleType(){
		return FORECAST_TYPE;
	}
	
	public ErrorDetectionScriptResult detect(ErrorDetectionContext context) {
		
		DBConnection conn = context.getConnection();
		String SKDGRP_ID = context.getSkdgrpId();
		
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Duplicate POS");
		actionResult.setHelpTip("Check to ensure there is no duplicate entries in POS data");
		actionResult.setHelpDesc("Duplicate POS data will cause forecast numbers to be increased incorrectly. The engine aggregates all POS data for a particular time interval when it is doing its calculation, so duplicate entries will be added together. This method should look for duplicate entries for the same time period and location. If the numbers in the tables are the same (volume) for both entries, then it should report this error.");
		actionResult.setErrorMsg("FAILED:");
		
		String result = new String("");
		
		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.2.7...");
		
		PreparedStatement stmt = null;
		ResultSet rs = null;
		
		try {
	        String strSelect = "SELECT * ";
	        strSelect += "FROM  ";
	        strSelect += "    (SELECT COUNT(RESDET_VOLUME) AS C, RESDET_DATE, RESDET_TIME, SKDGRP_NAME, INVTYP_ID ";
	        strSelect += "    FROM SO_RESULTS_DETAIL RD, ";
	        strSelect += "        (SELECT SKDGRP_ID, SKDGRP_NAME  ";
	        strSelect += "        FROM SO_SCHEDULE_GROUP, WORKBRAIN_TEAM  ";
	        strSelect += "        WHERE WORKBRAIN_TEAM.WBT_ID = SO_SCHEDULE_GROUP.WBT_ID AND  ";
	        strSelect += "            SKDGRP_INTRNL_TYPE = 12 AND  ";
	        strSelect += "           SO_SCHEDULE_GROUP.WBT_ID IN  ";
	        strSelect += "                (SELECT CHILD_WBT_ID  ";
	        strSelect += "                FROM SEC_WB_TEAM_CHILD_PARENT  ";
	        strSelect += "                WHERE PARENT_WBT_ID =  ";
	        strSelect += "                        (SELECT WBT_ID  ";
	        strSelect += "                        FROM SO_SCHEDULE_GROUP  ";
	        strSelect += "                        WHERE SKDGRP_ID = ?))  ";
	        strSelect += "        ORDER BY WBT_LEVEL DESC) S ";
	        strSelect += "    WHERE S.SKDGRP_ID = RD.SKDGRP_ID AND ";
	        strSelect += "        RESDET_VOLUME > 0 ";
	        strSelect += "    GROUP BY RESDET_DATE, RESDET_TIME, SKDGRP_NAME, INVTYP_ID) ";
	        strSelect += "WHERE C > 1 ";
	        
	        stmt = conn.prepareStatement( strSelect );
	        
	        stmt.setInt(1, Integer.parseInt(SKDGRP_ID));

	        rs = stmt.executeQuery();
	        
	        while( rs.next() )
			    result += "-Location " + rs.getString("SKDGRP_NAME") + " has " + rs.getInt("C") + " POS entries for " + rs.getDate("RESDET_DATE") + " at " + rs.getTime("RESDET_TIME") + "\n";
	    }
		catch(SQLException e) {
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
			logger.debug("Finished check 4.2.7");
		
		actionResult.setMessage(result);
		return actionResult;
	}

	protected String getLocalizedRuleName() {
		
		return "Duplicate POS";
	}

}
