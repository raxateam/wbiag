package com.wbiag.app.modules.retailSchedule.errorDetection;

import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import com.workbrain.app.modules.retailSchedule.exceptions.RetailException;
import com.workbrain.app.modules.retailSchedule.model.ScheduleGroupData;

public class StartDayRule extends ErrorDetectionRule {

	private static Logger logger = Logger.getLogger(StartDayRule.class);
	
	public Integer getRuleType(){
		return FORECAST_TYPE;
	}
	
	public ErrorDetectionScriptResult detect(ErrorDetectionContext context) {
		
		List corpTree = context.getCorpTree();
		
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Start Day");
		actionResult.setHelpTip("Checks to ensure all locations start on the same start day of week");
		actionResult.setHelpDesc("Typically, forecast data collected by POS systems will be collected at the same time interval across all departments for a store. The script should check to ensure that this is so in the database. If there are different intervals found the report should notify the user but not halt execution of the script.");
		actionResult.setErrorMsg("FAILED: The Start Day for the following location(s) is different from the other locations witin the hierarchy: ");
		
		String result = new String("");
		
		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.2.3...");
		
		try {
			int startDOW = -1;
		    if(corpTree !=null)
		    {
		        for( Iterator iter = corpTree.iterator(); iter.hasNext(); )
		        {
		        	ScheduleGroupData tempSkdgrpData = ScheduleGroupData.getScheduleGroupData(new Integer((String)iter.next()));
		        	
		            if( startDOW == -1 )
		                startDOW = tempSkdgrpData.getSkdgrpStartdow();
	
		            if( tempSkdgrpData.getSkdgrpStartdow() != startDOW )
		            {
					    if( result.length() == 0 )
					        result += tempSkdgrpData.getSkdgrpName();
					    else
					        result += ", " + tempSkdgrpData.getSkdgrpName();
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
			logger.debug("Finished check 4.2.3");
		
		actionResult.setMessage(result);
		return actionResult;
	}

	protected String getLocalizedRuleName() {
		
		return "Start Day";
	}

}
