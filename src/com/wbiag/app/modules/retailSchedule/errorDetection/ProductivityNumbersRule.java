package com.wbiag.app.modules.retailSchedule.errorDetection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import com.workbrain.app.modules.retailSchedule.exceptions.RetailException;
import com.workbrain.app.modules.retailSchedule.model.ScheduleGroupData;
import com.workbrain.app.modules.retailSchedule.type.InternalType;
import com.workbrain.sql.SQLHelper;
import com.workbrain.sql.DBConnection;

public class ProductivityNumbersRule extends ErrorDetectionRule {


	private static Logger logger = Logger.getLogger(RootNodeCheckRule.class);
	  
	public Integer getRuleType(){
		return SCHEDULE_TYPE;
	}
	
	public ErrorDetectionScriptResult detect(ErrorDetectionContext context) {
		
		DBConnection conn = context.getConnection();
		String SKDGRP_ID = context.getSkdgrpId();
		
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Productivity Numbers");
		actionResult.setHelpTip("Productivity numbers must be entered for Volume driven job requirements");
		actionResult.setHelpDesc("Productivity numbers have to exist for all jobs that are scheduled at the forecast driver Level that are Volume driven. These numbers are used to calculate the workload staffing requirements and cannot be 0.");
		actionResult.setErrorMsg("FAILED: The following jobs does not have volume productivity set for their respective location: ");
		
		String result = new String("");
		
		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.3.11...");
		
		PreparedStatement skdgrpStmt = null;
		ResultSet skdgrpRs = null;
		
		try {
			String skdgrpQueryString = "SELECT SKDGRP_ID, SKDGRP_NAME ";
			skdgrpQueryString += "FROM SO_SCHEDULE_GROUP, WORKBRAIN_TEAM ";
			skdgrpQueryString += "WHERE WORKBRAIN_TEAM.WBT_ID = SO_SCHEDULE_GROUP.WBT_ID AND ";
			skdgrpQueryString += "    SKDGRP_INTRNL_TYPE = 12 AND ";
			skdgrpQueryString += "   SO_SCHEDULE_GROUP.WBT_ID IN ";
			skdgrpQueryString += "        (SELECT CHILD_WBT_ID ";
			skdgrpQueryString += "        FROM SEC_WB_TEAM_CHILD_PARENT ";
			skdgrpQueryString += "        WHERE PARENT_WBT_ID = ";
			skdgrpQueryString += "                (SELECT WBT_ID ";
			skdgrpQueryString += "                FROM SO_SCHEDULE_GROUP ";
			skdgrpQueryString += "                WHERE SKDGRP_ID = ?)) ";
			skdgrpQueryString += "ORDER BY WBT_LEVEL DESC";
	
			skdgrpStmt = conn.prepareStatement(skdgrpQueryString);
			skdgrpStmt.setInt(1, Integer.parseInt(SKDGRP_ID));
			
			skdgrpRs = skdgrpStmt.executeQuery();
			
			PreparedStatement stmt = null;
			ResultSet rs = null;
			
			while(skdgrpRs.next())
			{
				try {
					ScheduleGroupData tempSkdgrpData = ScheduleGroupData.getScheduleGroupData(new Integer(skdgrpRs.getInt("SKDGRP_ID")));
					int type = tempSkdgrpData.getSkdgrpIntrnlType();
	
					if( InternalType.isDriver(new Integer(type)) &&
							tempSkdgrpData.getSkdgrpVolsub() != -1 )
					{
	//    					gets the volume productivity, VP, the job name, NAME, and the job id, ID, for each job underneath a given store.
						String strSelect = "SELECT G.SKDGRP_NAME SGNAME, V.WRKLD_STDVOL_HOUR VP, R.JOB_ID ID, J.JOB_NAME NAME "; 
						strSelect += "FROM JOB J, SO_SCHEDULE_GROUP G, SO_CLIENT_STFDEF R, SO_VOLUME_WORKLOAD V ";
						strSelect += "WHERE R.CSD_ID = V.CSD_ID AND ";
						strSelect += "R.JOB_ID = J.JOB_ID AND ";
						strSelect += "V.SKDGRP_ID = G.SKDGRP_ID AND ";
						strSelect += "V.SKDGRP_ID = ? ";
						
						stmt = conn.prepareStatement(strSelect);
	                    stmt.setInt(1, tempSkdgrpData.getSkdgrpId().intValue());
						rs = stmt.executeQuery();
						
						while (rs.next())
						{
							int volProd = rs.getInt( "VP" );
	
							//check empjob_rate_index equals jobrate_index
							if( volProd == 0 )
							{
							    if( result.length() == 0 )
	            			        result += rs.getString("NAME") + "(" + rs.getString("SGNAME") + ")";
	            			    else
	            			        result += ", " + rs.getString("NAME") + "(" + rs.getString("SGNAME") + ")";
							}
						}
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
		}
		catch(SQLException e) {
			result = e.getMessage();
		}
	    finally
	    {
	        SQLHelper.cleanUp(skdgrpRs);
	        SQLHelper.cleanUp(skdgrpStmt);
	    }
		
		if(result.compareTo("") == 0)
			result = "OK";
		
		if(logger.isDebugEnabled())
			logger.debug("Finished check 4.3.11");
		
		actionResult.setMessage(result);
		return actionResult;
	}

	protected String getLocalizedRuleName() {
		
		return "Productivity Numbers";
	}

}
