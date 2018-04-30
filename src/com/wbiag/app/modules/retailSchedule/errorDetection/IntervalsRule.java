package com.wbiag.app.modules.retailSchedule.errorDetection;

import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import com.workbrain.app.modules.retailSchedule.exceptions.RetailException;
import com.workbrain.app.modules.retailSchedule.model.ScheduleGroupData;

public class IntervalsRule extends ErrorDetectionRule {

	private static Logger logger = Logger.getLogger(IntervalsRule.class);
	
	public Integer getRuleType(){
		return FORECAST_TYPE;
	}
	
	public ErrorDetectionScriptResult detect(ErrorDetectionContext context) {
		
		List corpTree = context.getCorpTree();
		
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Intervals");
		actionResult.setHelpTip("Checks to ensure all forecast intervals are same");
		actionResult.setHelpDesc("Typically, forecast data collected by POS systems will be collected at the same time interval across all departments for a store. The script should check to ensure that this is so in the database. If there are different intervals found the report should notify the user but not halt execution of the script.");
		actionResult.setErrorMsg("WARNING: The forecast interval for the following location(s) is different from the other locations witin the hierarchy: ");
		
		String result = new String("");
		
		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.2.2...");
		
		try {
			int forecastInt = -1;
		    if(corpTree !=null)
		    {
		        for( Iterator iter = corpTree.iterator(); iter.hasNext(); )
		        {
		            ScheduleGroupData tempSkdgrpData = ScheduleGroupData.getScheduleGroupData(new Integer((String)iter.next()));
	
		            if( forecastInt == -1 )
		                forecastInt = tempSkdgrpData.getSkdgrpFcastInt();
	
		            if( tempSkdgrpData.getSkdgrpFcastInt() != forecastInt )
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
			logger.debug("Finished check 4.2.2");
		
		actionResult.setMessage(result);
		return actionResult;
	}

	protected String getLocalizedRuleName() {
		return "Intervals";
	}

}
