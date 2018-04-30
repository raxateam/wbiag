package com.wbiag.app.modules.retailSchedule.errorDetection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import com.workbrain.app.modules.retailSchedule.exceptions.RetailException;
import com.workbrain.app.modules.retailSchedule.model.ScheduleGroupData;
import com.workbrain.app.ta.model.JobData;
import com.workbrain.app.ta.db.CodeMapper;
import com.workbrain.sql.SQLHelper;
import com.workbrain.sql.DBConnection;;

public class NonVolumeStartEndTimesRule extends ErrorDetectionRule {

	private static Logger logger = Logger.getLogger(NonVolumeStartEndTimesRule.class);

	public Integer getRuleType(){
		return SCHEDULE_TYPE;
	}
	
	public ErrorDetectionScriptResult detect(ErrorDetectionContext context) {
		
		DBConnection conn = context.getConnection();
		List corpTree = context.getCorpTree();
		CodeMapper cm = context.getCodeMapper();
		
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Non Volume Start/End Times");
		actionResult.setHelpTip("Non Volume job requirements must have Start and End times entered");
		actionResult.setHelpDesc("Locations must specify in the requirements section the Start and End times for the Volume driven Jobs listed.");
		actionResult.setErrorMsg("FAILED: The following jobs does not have start/end time set for their respective location: ");
		
		String result = new String("");
		
		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.3.12...");
		
		if(corpTree !=null){
			PreparedStatement stmt = null;
			ResultSet rs = null;
			
    		for(Iterator iter = corpTree.iterator(); iter.hasNext(); )
    		{
    			try {
    				ScheduleGroupData tempSkdgrpData = ScheduleGroupData.getScheduleGroupData(new Integer((String)iter.next()));

					String strSelect = "select skdgrp_id, job_id, csd_start_time, csd_end_time from so_client_stfdef where csd_nonvlm_flag = 1 and (csd_start_time is null or csd_end_time is null) ";
					strSelect += "and skdgrp_id = ?";
					
					stmt = conn.prepareStatement(strSelect);
                    stmt.setInt(1, tempSkdgrpData.getSkdgrpId().intValue());
					rs = stmt.executeQuery();
					
					while (rs.next())
					{
						int jobId = rs.getInt( "JOB_ID" );
					    JobData tempJobData = cm.getJobById( jobId );
        			    
					    if( result.length() == 0 )
        			        result += tempJobData.getJobName() + "(" + tempSkdgrpData.getSkdgrpName() + ")";
        			    else
        			        result += ", " + tempJobData.getJobName() + "(" + tempSkdgrpData.getSkdgrpName() + ")";
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
		
		if(result.compareTo("") == 0)
			result = "OK";
		
		if(logger.isDebugEnabled())
			logger.debug("Finished check 4.3.12");
		
		actionResult.setMessage(result);
		return actionResult;
	}

	protected String getLocalizedRuleName() {
		return "Non Volume Start/End Times";
	}

}
