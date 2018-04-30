package com.wbiag.app.modules.retailSchedule.errorDetection;

import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import com.workbrain.app.modules.retailSchedule.exceptions.RetailException;
import com.workbrain.app.modules.retailSchedule.model.ScheduleGroupData;
import com.workbrain.app.modules.retailSchedule.type.InternalType;

public class LevelsRule extends ErrorDetectionRule {

	private static Logger logger = Logger.getLogger(LevelsRule.class);
	
	public Integer getRuleType(){
		return FORECAST_TYPE;
	}
	
	public ErrorDetectionScriptResult detect(ErrorDetectionContext context) {
		
		List corpTree = context.getCorpTree();
		
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Levels");
		actionResult.setHelpTip("Checks to ensure level requirements are met");
		actionResult.setHelpDesc("A user may mistakenly select a forecast driver's parent to be a schedule department or forecast driver level. This will not allow forecasts and/or schedules to generate properly (or at all).  This error will halt the execution of the script to avoid producing incorrect errors that may occur as a result.");
		actionResult.setErrorMsg("FAILED: The following locations have an invalid parent: ");
		
		String result = new String("");
		
		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.2.1...");
		
		try {
			if(corpTree !=null)
		    {
		        for( Iterator iter = corpTree.iterator(); iter.hasNext(); )
		        {
		            ScheduleGroupData tempSkdgrpData = ScheduleGroupData.getScheduleGroupData(new Integer((String)iter.next()));
	
		            // Ensure we're not at the very top level
		            if( tempSkdgrpData.getSkdgrpParentId() != null )
		            {
		                ScheduleGroupData parentSkdgrpData = ScheduleGroupData.getScheduleGroupData(tempSkdgrpData.getSkdgrpParentId());
		                if(!InternalType.isStore(new Integer(parentSkdgrpData.getSkdgrpIntrnlType())))
		                {
						    if( result.length() == 0 )
						        result += tempSkdgrpData.getSkdgrpName();
						    else
						        result += ", " + tempSkdgrpData.getSkdgrpName();
		                }
		            }
		        }
		    }
		}
		catch(RetailException e) {
			result = e.getMessage();
		}
		
		if(result.compareTo("") == 0)
			result = "OK";
		
		if(logger.isDebugEnabled())
			logger.debug("Finished check 4.2.1");
		
		actionResult.setMessage(result);
		return actionResult;
	}

	protected String getLocalizedRuleName() {
		return "Levels";
	}

}
