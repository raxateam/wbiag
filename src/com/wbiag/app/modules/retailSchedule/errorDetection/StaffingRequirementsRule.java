package com.wbiag.app.modules.retailSchedule.errorDetection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import com.workbrain.sql.SQLHelper;
import com.workbrain.sql.DBConnection;

public class StaffingRequirementsRule extends ErrorDetectionRule {


	private static Logger logger = Logger.getLogger(RootNodeCheckRule.class);
	  
	public Integer getRuleType(){
		return SCHEDULE_TYPE;
	}
	
	public ErrorDetectionScriptResult detect(ErrorDetectionContext context) {
		
		DBConnection conn = context.getConnection();
		String SKDGRP_ID = context.getSkdgrpId();
		
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Staffing Requirements");
		actionResult.setHelpTip("Ensure there is same between schedule department and forecast drivers (jobs are listed)");
		actionResult.setHelpDesc("Jobs that are specified in the forecast driver’s Staffing Requirements section must be defined in one of the forecast driver’s Schedule department. Also, it should ensure that Schedule department jobs are defined in at least one of the forecast driver’s Staffing Requirements sections. Violations of this will result in an error displayed to the user but the script will not halt execution.");
		actionResult.setErrorMsg("WARNING:");
		
		String result = new String("");
		
		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.3.4...");
		
		PreparedStatement stmt = null;
		ResultSet rs = null;
		
		try {
			String strSelect = "SELECT A.CSD_EFF_END_DATE AS GAP_START, B.CSD_EFF_START_DATE AS GAP_END, J.JOB_NAME, S.SKDGRP_NAME ";
			strSelect += "FROM SO_CLIENT_STFDEF A, SO_CLIENT_STFDEF B, JOB J, SO_SCHEDULE_GROUP S ";
			strSelect += "WHERE A.CSD_EFF_END_DATE < B.CSD_EFF_START_DATE AND ";
			strSelect += "A.JOB_ID = B.JOB_ID AND ";
			strSelect += "A.SKDGRP_ID = B.SKDGRP_ID AND ";
			strSelect += "S.SKDGRP_ID = A.SKDGRP_ID AND ";
			strSelect += "J.JOB_ID = A.JOB_ID AND ";
			strSelect += "S.SKDGRP_ID IN (SELECT skdgrp_id FROM so_schedule_group, workbrain_team WHERE workbrain_team.wbt_id = so_schedule_group.wbt_id AND so_schedule_group.wbt_id IN (SELECT child_wbt_id FROM sec_wb_team_child_parent WHERE parent_wbt_id = (SELECT wbt_id FROM so_schedule_group WHERE skdgrp_id = ?))) ";

			
			stmt = conn.prepareStatement(strSelect);
			
			stmt.setInt(1, Integer.parseInt(SKDGRP_ID));
			
			rs = stmt.executeQuery();
			
			while(rs.next())
				result += "-There is a gap in staffing requirement " + rs.getString("JOB_NAME") + " at location " + rs.getString("SKDGRP_NAME") + " from " + rs.getDate("GAP_START") + " to " + rs.getDate("GAP_END") + "\n";
		}
		catch(SQLException e) {
			result = e.getMessage();
		}
		finally {
			SQLHelper.cleanUp(rs);
			SQLHelper.cleanUp(stmt);
		}
		
		if(result.compareTo("") == 0)
			result = "OK";
		
		if(logger.isDebugEnabled())
			logger.debug("Finished check 4.3.4");
		
		actionResult.setMessage(result);
		return actionResult;
	}

	protected String getLocalizedRuleName() {
		return "Staffing Requirements";
	}

}
