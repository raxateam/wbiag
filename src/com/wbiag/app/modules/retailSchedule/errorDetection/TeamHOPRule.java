package com.wbiag.app.modules.retailSchedule.errorDetection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import com.workbrain.app.modules.retailSchedule.exceptions.RetailException;
import com.workbrain.app.modules.retailSchedule.model.ScheduleGroupData;
import com.workbrain.sql.SQLHelper;
import com.workbrain.sql.DBConnection;

public class TeamHOPRule extends ErrorDetectionRule {

	private static Logger logger = Logger.getLogger(TeamHOPRule.class);

	public Integer getRuleType(){
		return SCHEDULE_TYPE;
	}
	
	public ErrorDetectionScriptResult detect(ErrorDetectionContext context) {
		
		List corpTree = context.getCorpTree();
		DBConnection conn = context.getConnection();
		
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Team Hours of Op");
		actionResult.setHelpTip("All teams must have hours of operation defined to prevent crashing");
		actionResult.setHelpDesc("Schedules will not generate if teams do not have hours of operation defined. These are used to ensure that the scheduled hours for employees are set within the hours of operations that the team has.");
		actionResult.setErrorMsg("FAILED: The following teams have no Hours of Operation defined: ");
		
		String result = new String("");
		
		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.3.9...");
		
		if(corpTree !=null)
	    {
			PreparedStatement stmt = null;
			ResultSet rs = null;
			
	        for( Iterator iter = corpTree.iterator(); iter.hasNext(); )
	        {
	        	try {
	        		ScheduleGroupData tempSkdgrpData = ScheduleGroupData.getScheduleGroupData(new Integer((String)iter.next()));

	                String strSelect = "SELECT wbt_name FROM workbrain_team WHERE hrsop_id is null AND wbt_id = ?"  ;
	                stmt = conn.prepareStatement( strSelect );
	                stmt.setInt(1 , tempSkdgrpData.getWbtId()) ;
	                rs = stmt.executeQuery();
	                if( rs.next() )
	                {
	    			    if( result.length() == 0 )
	    			        result += tempSkdgrpData.getSkdgrpName() + " - (WB Team: " + rs.getString(1)+ ")";
	    			    else
	    			        result += ", " + tempSkdgrpData.getSkdgrpName() + " - (WB Team: " + rs.getString(1)+ ")";
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
			logger.debug("Finished check 4.3.9");
		
		actionResult.setMessage(result);
		return actionResult;
	}

	protected String getLocalizedRuleName() {
		return "Team Hours of Op";
	}

}
