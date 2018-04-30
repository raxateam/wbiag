package com.wbiag.app.modules.retailSchedule.errorDetection;

import org.apache.log4j.Logger;



public class VolumeTypeVariableStaffingRule extends ErrorDetectionRule {
	
	private static Logger logger = Logger.getLogger(VolumeTypeVariableStaffingRule.class);

	public ErrorDetectionScriptResult detect(ErrorDetectionContext context) {
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Volume Type and Variable Staffing Calculation");
		actionResult.setHelpTip("Correlation between Volume Type and Variable Staffing Calculation");
		actionResult.setHelpDesc("Typically, users will choose a Standard Volume per Hour for their Variable Staffing Calculation when their Volume Type is based on Revenue. Since this is not the only solution, violation of this correlation will not halt execution of the script and only display a warning to the user of a possible mismatch.");
		actionResult.setErrorMsg("");
		
		String result = new String("");
		
		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.2.5...");
		
		result = "No longer necessary";
		
		if(result.compareTo("") == 0)
			result = "OK";
		
		if(logger.isDebugEnabled())
			logger.debug("Finished check 4.2.5");
		
		actionResult.setMessage(result);
		return actionResult;
	}

	protected String getLocalizedRuleName() {
		
		return "Volume Type and Variable Staffing Calculation";
	}
	
	public Integer getRuleType() {
		return ErrorDetectionRule.FORECAST_TYPE;
	}
	
}