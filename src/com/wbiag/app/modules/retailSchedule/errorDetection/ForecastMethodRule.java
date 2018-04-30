package com.wbiag.app.modules.retailSchedule.errorDetection;

import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import com.workbrain.app.modules.retailSchedule.exceptions.RetailException;
import com.workbrain.app.modules.retailSchedule.model.ScheduleGroupData;

public class ForecastMethodRule extends ErrorDetectionRule {

	private static Logger logger = Logger.getLogger(ForecastMethodRule.class);
	
	public Integer getRuleType(){
		return FORECAST_TYPE;
	}
	
	public ErrorDetectionScriptResult detect(ErrorDetectionContext context) {
		
		List corpTree = context.getCorpTree();
		
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Forecast Method");
		actionResult.setHelpTip("Checks forecast method consistency");
		actionResult.setHelpDesc("Forecast methods are typically uniform across sub-locations. Having different forecast methods may be valid, but the user should be warned if they are and not halt execution of the script.");
		actionResult.setErrorMsg("WARNING: The Forecast Method for the following location(s) is different from the other locations witin the hierarchy: ");
		
		String result = new String("");
		
		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.2.4...");
		
		try { 
			int forecastMtd = -1;
		    if(corpTree !=null)
		    {
		        for( Iterator iter = corpTree.iterator(); iter.hasNext(); )
		        {
		        	ScheduleGroupData tempSkdgrpData = ScheduleGroupData.getScheduleGroupData(new Integer((String)iter.next()));
	
		            if( forecastMtd == -1 )
		                forecastMtd = tempSkdgrpData.getSkdgrpFcastMtd();
	
		            if( tempSkdgrpData.getSkdgrpFcastMtd() != forecastMtd )
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
			logger.debug("Finished check 4.2.4");
			
		actionResult.setMessage(result);
		return actionResult;
	}

	protected String getLocalizedRuleName() {
		return "Forecast Method";
	}

}
