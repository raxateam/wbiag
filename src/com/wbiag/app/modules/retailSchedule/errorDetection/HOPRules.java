package com.wbiag.app.modules.retailSchedule.errorDetection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import com.workbrain.app.modules.retailSchedule.exceptions.RetailException;
import com.workbrain.app.modules.retailSchedule.model.CorporateEntity;
import com.workbrain.app.modules.retailSchedule.model.ScheduleGroupData;
import com.workbrain.app.modules.retailSchedule.type.HoursOfOperationType;
import com.workbrain.sql.DBConnection;

public class HOPRules extends ErrorDetectionRule {

	private static Logger logger = Logger.getLogger(RootNodeCheckRule.class);

	public Integer getRuleType(){
		return SCHEDULE_TYPE;
	}
	
	public ErrorDetectionScriptResult detect(ErrorDetectionContext context) {
		
		List corpTree = context.getCorpTree();
		DBConnection conn = context.getConnection();
		
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Hours Of Operation");
		actionResult.setHelpTip("Check to ensure hours of operation are defined");
		actionResult.setHelpDesc("Hours of Operation have to be properly defined by each section. There are three possible scenarios that a user may select: Default Hours, Defined and Same as Parent.  If Default Hours is selected, then the script must ensure that the team has Hours of Operations specified.  If Defined is selected, then the script must ensure that hours were actually defined by the user.  If Same as Parent is selected, then the script must check to see that Parent (or higher if Parent has ‘Same as Parent’ selected) had hours defined.");
		actionResult.setErrorMsg("");
		
		String result = new String("");
		
		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.3.5...");
		
		try {
			if(corpTree !=null){
				for(Iterator iter = corpTree.iterator(); iter.hasNext(); )
				{
					ScheduleGroupData tempSkdgrpData = ScheduleGroupData.getScheduleGroupData(new Integer((String)iter.next()));
	
		    		result = checkHOP( tempSkdgrpData, conn );
				}
		    }
		}
		catch(RetailException e) {
			result = e.getMessage();
		}
		
		if(result.compareTo("") == 0)
			result = "OK";
		
		if(logger.isDebugEnabled())
			logger.debug("Finished check 4.3.5");
		
		actionResult.setMessage(result);
		return actionResult;
	}

	protected String getLocalizedRuleName() {
		return "Hours Of Operation";
	}

//  Recursive method for 4.3.5
    private String checkHOP( ScheduleGroupData skdgrpData, DBConnection conn )
    {
        String tempResult = "";

	    try
	    {
    		if( skdgrpData.getSkdgrpHrsOpSrc() == HoursOfOperationType.HRSOP_DEFAULT.intValue() )
    		{
                String strSelect = "SELECT wbt_name FROM workbrain_team WHERE hrsop_id is null AND wbt_id = ?" ;
                PreparedStatement stmt = conn.prepareStatement( strSelect );
                stmt.setInt(1, skdgrpData.getWbtId());
                ResultSet rs = stmt.executeQuery();
                if( rs.next() )
                {
    				tempResult = "FAILED: " + skdgrpData.getSkdgrpName() + " with Workbrain team " + rs.getString(1) + " (default set) has no Hours of Operation defined.\n";
                }
    		}
    		else if( skdgrpData.getSkdgrpHrsOpSrc() == HoursOfOperationType.HRSOP_SETS.intValue() )
    		{
    		    String strSelect = "SELECT * FROM SO_CORP_ENT_HOUR WHERE SKDGRP_ID = ? " ;
                PreparedStatement stmt = conn.prepareStatement( strSelect  );
                stmt.setInt(1, skdgrpData.getWbtId());
                ResultSet rs = stmt.executeQuery();
                if( !rs.next() )
                {
    				tempResult = "FAILED: " + skdgrpData.getSkdgrpName() + " (defined set) has no Hours of Operation defined.\n";
                }
    		}
    		else if( skdgrpData.getSkdgrpHrsOpSrc() == HoursOfOperationType.HRSOP_INHERIT.intValue() )
    		{
    		    Integer parentID = skdgrpData.getSkdgrpParentId();
    		    if( parentID == null )
    		    {
    		        tempResult = "FAILED: " + skdgrpData.getSkdgrpName() + " has no parent while Hours of Operation is set to 'same as parent'.\n";
    		    }
    		    else
    		    {
                    CorporateEntity cE = CorporateEntity.getCorporateEntity( parentID );
                    ScheduleGroupData parentSkdgrpData = cE.getSkdgrpData();
                    tempResult = checkHOP( parentSkdgrpData, conn );
                }
    		}
        }
        catch( Exception e )
        {
			if(logger.isDebugEnabled())
				logger.debug(e.getStackTrace());
			
            tempResult = e.getMessage();
        }
		return tempResult;
    }

}
