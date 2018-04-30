package com.wbiag.app.modules.retailSchedule.errorDetection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import com.workbrain.app.modules.retailSchedule.exceptions.RetailException;
import com.workbrain.app.modules.retailSchedule.model.ScheduleGroupData;
import com.workbrain.app.modules.retailSchedule.type.InternalType;
import com.workbrain.sql.SQLHelper;
import com.workbrain.sql.DBConnection;

public class EmptyStaffGroupRule extends ErrorDetectionRule {

	private static Logger logger = Logger.getLogger(EmptyStaffGroupRule.class);
	  
	public Integer getRuleType(){
		return SCHEDULE_TYPE;
	}
	
	public ErrorDetectionScriptResult detect(ErrorDetectionContext context) {
		
		List corpTree =context.getCorpTree();
		DBConnection conn = context.getConnection();
		
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Empty Staff Group");
		actionResult.setHelpTip("Staff groups should not be empty");
		actionResult.setHelpDesc("If a staff group is selected and the scheduling check task is run, an error must be displayed to tell the user that the staff group they have selected is empty. Even if one of the groups is empty, this error must be displayed.");
		actionResult.setErrorMsg("FAILED: The following Staff Group(s) is empty: ");
		
		String result = new String("");
		
		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.3.13...");
		
		if(corpTree !=null){
			PreparedStatement stmt = null;
			ResultSet rs = null;
			
    		for(Iterator iter = corpTree.iterator(); iter.hasNext(); )
    		{
    			try {
    				ScheduleGroupData tempSkdgrpData = ScheduleGroupData.getScheduleGroupData(new Integer((String)iter.next()));
    				int type = tempSkdgrpData.getSkdgrpIntrnlType();

    				if( InternalType.isDriver(new Integer(type)) &&
    						tempSkdgrpData.getSkdgrpVolsub() != -1 )
    				{
	
						String strSelect = "select empgrp_id, empgrp_name from so_employee_group where empgrp_id not in (select distinct(empgrp_id) from so_employee) ";
						strSelect += "and skdgrp_id = ?";
						
						stmt = conn.prepareStatement(strSelect);
	                    stmt.setInt(1, tempSkdgrpData.getSkdgrpId().intValue());
						rs = stmt.executeQuery();
						
						while (rs.next())
						{
							String empgrpName = rs.getString( "empgrp_name" );
	
	        			    if( result.length() == 0 )
	        			        result += empgrpName;
	        			    else
	        			        result += ", " + empgrpName;
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
		
		if(result.compareTo("") == 0)
			result = "OK";
		
		if(logger.isDebugEnabled())
			logger.debug("Finished check 4.3.13");
		
		actionResult.setMessage(result);
		return actionResult;
	}

	protected String getLocalizedRuleName() {
		return "Empty Staff Group";
	}

}
