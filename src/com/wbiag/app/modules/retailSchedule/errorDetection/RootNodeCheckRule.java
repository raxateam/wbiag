package com.wbiag.app.modules.retailSchedule.errorDetection;

import org.apache.log4j.Logger;

import com.workbrain.app.modules.mvse.validator.ValidationEngine;
import com.workbrain.app.modules.retailSchedule.exceptions.RetailException;
import com.workbrain.app.modules.retailSchedule.model.ScheduleGroupData;
import com.workbrain.app.modules.retailSchedule.type.InternalType;

public class RootNodeCheckRule extends ErrorDetectionRule {

	private static Logger logger = Logger.getLogger(RootNodeCheckRule.class);
	  
	public Integer getRuleType(){
		return SCHEDULE_TYPE;
	}
	
	public ErrorDetectionScriptResult detect(ErrorDetectionContext context) {
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Root location check");
		actionResult.setHelpTip("The root location must have an internal type of 'Store'");
		actionResult.setHelpDesc("The root location must have an internal type of 'Store'");
		actionResult.setErrorMsg("FATAL ERROR: ");
		
		String result = new String("");
		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.0.1...");
		
		try {
		    ScheduleGroupData loc = ScheduleGroupData.getScheduleGroupData( new Integer( context.getSkdgrpId() ) );
	
		    if( loc != null )
		    {
		    	int type = loc.getSkdgrpIntrnlType();
		        if(!InternalType.isStore(new Integer(type)))
		            result += "Root Location is not Store type";

		    }
		}
		catch(RetailException e) {
			result = e.getMessage() + "\n";
		}
		
		if(result.compareTo("") == 0)
			result = "OK";
		else
		 	actionResult.setFatalError(true);
		
		if(logger.isDebugEnabled())
			logger.debug("Finished check 4.0.1");
		
		actionResult.setMessage(result);
		return actionResult;
	}

	protected String getLocalizedRuleName() {
		return "Root Node Check";
	}

}
