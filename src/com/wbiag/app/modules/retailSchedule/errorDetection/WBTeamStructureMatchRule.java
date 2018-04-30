package com.wbiag.app.modules.retailSchedule.errorDetection;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import com.workbrain.app.modules.retailSchedule.exceptions.RetailException;
import com.workbrain.app.modules.retailSchedule.model.ScheduleGroupData;
import com.workbrain.app.ta.db.WorkbrainTeamAccess;
import com.workbrain.app.ta.model.WorkbrainTeamData;
import com.workbrain.sql.DBConnection;

public class WBTeamStructureMatchRule extends ErrorDetectionRule {

	private static Logger logger = Logger.getLogger(WBTeamStructureMatchRule.class);

	public Integer getRuleType(){
		return SCHEDULE_TYPE;
	}
	
	public ErrorDetectionScriptResult detect(ErrorDetectionContext context) {
		
		List corpTree = context.getCorpTree();
		DBConnection conn = context.getConnection();
		
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Workbrain Team Structure Match");
		actionResult.setHelpTip("Workbrain Team and location structures should always match all the way down ideally");
		actionResult.setHelpDesc("Workbrain Team and location structures should always match all the way down ideally");
		actionResult.setErrorMsg("FAILED: The following location(s) have to be revisited: ");
		
		String result = new String("");
		
		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.3.16...");
		
		if(corpTree !=null)
	    {
			WorkbrainTeamAccess teamAccess = new WorkbrainTeamAccess(conn);
	        
			for( Iterator iter = corpTree.iterator(); iter.hasNext(); )
	        {
	        	try {
	        		ScheduleGroupData loc = ScheduleGroupData.getScheduleGroupData(new Integer((String)iter.next()));
		            
		            if(loc.getSkdgrpParentId() != null) {
						ScheduleGroupData parentLoc = ScheduleGroupData.getScheduleGroupData(loc.getSkdgrpParentId());
						
						int locTeamID = loc.getWbtId();
						int parentLocTeamID = parentLoc.getWbtId();
						WorkbrainTeamData locTeam = teamAccess.load(locTeamID);
						int parentTeamTeamID = locTeam.getWbtParentId();
						
						if(!(parentLocTeamID == parentTeamTeamID))
							result += "-WARNING: Location/Team structure does not match at location \"" + loc.getSkdgrpName() + "\" with team \"" + locTeam.getWbtName() + "\"\n";
			        }//end if get parent is not null
		        }//end try
	        	catch(SQLException e) {
	        		result += e.getMessage() + "\n";
	        	}
	        	catch(RetailException e) {
	        		result += e.getMessage() + "\n";
	        	}
	        }//end for iter
	    }//end if corptree not null
		
		if(result.compareTo("") == 0)
			result = "OK";
		
		if(logger.isDebugEnabled())
			logger.debug("Finished check 4.3.16");
		
		actionResult.setMessage(result);
		return actionResult;
	}

	protected String getLocalizedRuleName() {
		return "Workbrain Team Structure Match";
	}

}
