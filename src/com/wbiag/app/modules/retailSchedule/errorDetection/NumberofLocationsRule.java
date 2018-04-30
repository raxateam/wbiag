package com.wbiag.app.modules.retailSchedule.errorDetection;

import java.util.List;
import org.apache.log4j.Logger;



public class NumberofLocationsRule extends ErrorDetectionRule {

	private static Logger logger = Logger.getLogger(NumberofLocationsRule.class);
	
	public Integer getRuleType(){
		return SCHEDULE_TYPE;
	}
	
	public ErrorDetectionScriptResult detect(ErrorDetectionContext context) {
		
		List corpTree = context.getCorpTree();
		
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Number of Locations");
		actionResult.setHelpTip("There must be at least three locations");
		actionResult.setHelpDesc("There must be at least three locations for the script to properly execute. One should be a store, one should be a schedule department, and one should be a forecast driver");
		actionResult.setErrorMsg("FATAL ERROR: ");
		
		String result = new String("");
		
		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.0.2...");
		
	    if(corpTree !=null)
	    {
	        if(corpTree.size() <= 2 )
	        	result = "Number of Locations found: " + corpTree.size();

	    }
	    
	    if(result.compareTo("") == 0)
			result = "OK";
		else
		 	actionResult.setFatalError(true);
	    
	    if(logger.isDebugEnabled())
			logger.debug("Finished check 4.0.2");
		
		actionResult.setMessage(result);
		return actionResult;
	}

	protected String getLocalizedRuleName() {
		
		return "Number of Locations";
	}

}
