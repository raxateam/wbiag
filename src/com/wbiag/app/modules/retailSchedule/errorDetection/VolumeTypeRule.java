package com.wbiag.app.modules.retailSchedule.errorDetection;

import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import com.workbrain.app.modules.retailSchedule.exceptions.RetailException;
import com.workbrain.app.modules.retailSchedule.model.ScheduleGroupData;

public class VolumeTypeRule extends ErrorDetectionRule {

	private static Logger logger = Logger.getLogger(VolumeTypeRule.class);
	
	public Integer getRuleType(){
		return SCHEDULE_TYPE;
	}
	
	public ErrorDetectionScriptResult detect(ErrorDetectionContext context) {
		List corpTree = context.getCorpTree();
		
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Volume Type");
		actionResult.setHelpTip("Checks volume types");
		actionResult.setHelpDesc("A bug has been found in the system that puts a null value in the database. The cause of this error has not yet been determined. This method will notify the user of this problem if it finds a null value in the Volume Type. This error will halt execution of the script.");
		actionResult.setErrorMsg("FATAL ERROR: Volume type is null for the following location(s): ");
		
		String result = new String("");
		
		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.1.1...");
		
		if(corpTree !=null)
	    {
	        for( Iterator iter = corpTree.iterator(); iter.hasNext(); )
	        {
	        	try {
		            ScheduleGroupData tempSkdgrpData = ScheduleGroupData.getScheduleGroupData(new Integer((String)iter.next()));
		            if( tempSkdgrpData.getVoltypId() == null )
		            {
					    if( result.length() == 0 )
					        result += tempSkdgrpData.getSkdgrpName();
					    else
					        result += ", " + tempSkdgrpData.getSkdgrpName();
		            }
	        	}
	        	catch(RetailException e) {
	        		result += e.getMessage();
	        	}
	        }
	    }
		
		if(result.compareTo("") == 0)
			result = "OK";
		else
			actionResult.setFatalError(true);
		
		if(logger.isDebugEnabled())
			logger.debug("Finished check 4.1.1");
		
		actionResult.setMessage(result);
		return actionResult;
	}

	protected String getLocalizedRuleName() {

		return "Volume Type";
	}

}
