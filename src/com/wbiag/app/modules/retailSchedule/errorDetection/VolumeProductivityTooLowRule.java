package com.wbiag.app.modules.retailSchedule.errorDetection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;

import org.apache.log4j.Logger;

import com.workbrain.app.modules.retailSchedule.exceptions.RetailException;
import com.workbrain.app.modules.retailSchedule.model.Forecast;
import com.workbrain.app.modules.retailSchedule.model.ForecastDetail;
import com.workbrain.sql.SQLHelper;
import com.workbrain.sql.DBConnection;

public class VolumeProductivityTooLowRule extends ErrorDetectionRule {

	private static Logger logger = Logger.getLogger(VolumeProductivityTooLowRule.class);
	  
	public Integer getRuleType(){
		return SCHEDULE_TYPE;
	}
	
	public ErrorDetectionScriptResult detect(ErrorDetectionContext context) {
		
		String SKDGRP_ID = context.getSkdgrpId();
		DBConnection conn = context.getConnection();
		
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Volume Productivity Too Low");
		actionResult.setHelpTip("For each job associated with a store's forecast driver, the store's forecasted revenue divided by the volume productivity for each job should be below 250.");
		actionResult.setHelpDesc("If the required number of shifts (forecasted revenue divided by volume productivity) exceeds 250, a warning is issued to note a potential problem with volume productivity being too low.");
		actionResult.setErrorMsg("FAILED: The following warning(s) have been produced:\n");
		
		String result = new String("");
		
		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.3.14...");
		
		PreparedStatement stmt = null;
		ResultSet rs = null;
		
		try {
			//gets the schedule ID and forecast ID for any schedule of store type having a forecast for it
			String strSelect = "SELECT S.SKDGRP_ID, F.FCAST_ID, F.FCAST_NAME FNAME FROM (SELECT skdgrp_id FROM so_schedule_group, workbrain_team WHERE workbrain_team.wbt_id = so_schedule_group.wbt_id AND skdgrp_intrnl_type = 12 AND so_schedule_group.wbt_id IN (SELECT child_wbt_id FROM sec_wb_team_child_parent WHERE parent_wbt_id = (SELECT wbt_id FROM so_schedule_group WHERE skdgrp_id = ?)) ORDER BY wbt_level DESC) S JOIN SO_FCAST F ON (S.SKDGRP_ID = F.SKDGRP_ID)";
			stmt = conn.prepareStatement(strSelect);
            stmt.setInt(1, Integer.parseInt(SKDGRP_ID));
			
			rs = stmt.executeQuery();
			
			while(rs.next()) {
				int skdgrp_id = rs.getInt("SKDGRP_ID");
				int fcast_id = rs.getInt("FCAST_ID");
				Vector fd = (Vector)(new Forecast(new Integer(fcast_id))).getDetailList();
				
				//gets the volume productivity, VP, the job name, NAME, and the job id, ID, for each job underneath a given store.
				strSelect = "SELECT G.SKDGRP_NAME SGNAME, V.WRKLD_STDVOL_HOUR VP, R.JOB_ID ID, J.JOB_NAME NAME "; 
				strSelect += "FROM JOB J, SO_SCHEDULE_GROUP G, SO_CLIENT_STFDEF R, SO_VOLUME_WORKLOAD V ";
				strSelect += "WHERE R.CSD_ID = V.CSD_ID AND ";
				strSelect += "R.JOB_ID = J.JOB_ID AND ";
				strSelect += "V.SKDGRP_ID = G.SKDGRP_ID AND ";
				strSelect += "V.SKDGRP_ID = ? ";
				PreparedStatement stmt2 = null;
				ResultSet jobs = null;
				
				try {
					stmt2 = conn.prepareStatement(strSelect);
	                stmt2.setInt(1, skdgrp_id);
	                jobs = stmt2.executeQuery();
	            	
	            	while(jobs.next()) {
	            		if(jobs.getInt("VP") == 0)
	            			result += "-WARNING: Job \"" + jobs.getString("NAME") + "\" for location \"" + jobs.getString("SGNAME") + "\" has a volume productivity of 0! Skipping this location...\n";
	            		else {
	            			for(int i = 0; i < fd.size(); i++) {
	            				ForecastDetail currentDetail = ((ForecastDetail)fd.elementAt(i));
	            				
	            				if((currentDetail.getFcastCalls() + currentDetail.getFcastAdjval()) / (jobs.getInt("VP")*1.0) > 250)
	            					result += "-WARNING: Volume productivity for job \"" + jobs.getString("NAME") + "\" for location \"" + jobs.getString("SGNAME") + "\" is too low for forecast \"" + rs.getString("FNAME") + "\" on " + currentDetail.getFcastDate() + " (" + (currentDetail.getFcastCalls() + currentDetail.getFcastAdjval()) / jobs.getInt("VP") + " > 250).\n";
	            			}//end for i=0..fd.size
	            		}//end else
	            	}//end while jobs.next
				} finally {
					SQLHelper.cleanUp(jobs);
			        SQLHelper.cleanUp(stmt2);
				}
        	}//end while rs.next
		}
		catch(SQLException e) {
			result += e.getMessage() + "\n";
		}
		catch(RetailException e) {
			result += e.getMessage() + "\n";
		}
	    finally
	    {
	        SQLHelper.cleanUp(rs);
	        SQLHelper.cleanUp(stmt);
	    }
		
		if(result.compareTo("") == 0)
			result = "OK";
		
		if(logger.isDebugEnabled())
			logger.debug("Finished check 4.3.14");
		
		actionResult.setMessage(result);
		return actionResult;
	}

	protected String getLocalizedRuleName() {
		return "Volume Productivity Too Low";
	}

}
