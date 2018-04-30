package com.wbiag.app.modules.retailSchedule.errorDetection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import com.workbrain.sql.SQLHelper;
import com.workbrain.sql.DBConnection;;

public class TeamParrentCheckRule extends ErrorDetectionRule {

	private static Logger logger = Logger.getLogger(RootNodeCheckRule.class);
	
	public Integer getRuleType(){
		return SCHEDULE_TYPE;
	}
	
	public ErrorDetectionScriptResult detect(ErrorDetectionContext context) {
		DBConnection conn = context.getConnection();
		
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Team parent check");
		actionResult.setHelpTip("Checks to ensure a team's parent is not itself");
		actionResult.setHelpDesc("If a team's parent is set to itself, an endless loop could be entered causing a stack overflow. This check will ensure no team except the root team has itself as it's parent");
		actionResult.setErrorMsg("FATAL ERROR: The following teams have their parent team set to themself\n");
		
		String result = new String("");
		
		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.0.4..");
		
		PreparedStatement stmt = null;
		ResultSet rs = null;
		
		try {
			String strSelect = "SELECT WBT_NAME ";
			strSelect += "FROM WORKBRAIN_TEAM ";
			strSelect += "WHERE WBT_ID = WBT_PARENT_ID ";
			strSelect += "AND WBT_ID > 1 ";
			
			stmt = conn.prepareStatement(strSelect);
			rs = stmt.executeQuery();
			
			while(rs.next())
				result += "-" + rs.getString("WBT_NAME") + "\n";
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
			logger.debug("Finished check 4.0.4");
		
		actionResult.setMessage(result);
		return actionResult;
	}

	protected String getLocalizedRuleName() {

		return "Team parent check";
	}

}
