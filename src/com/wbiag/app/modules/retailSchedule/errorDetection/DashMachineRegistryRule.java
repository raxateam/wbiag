package com.wbiag.app.modules.retailSchedule.errorDetection;

import org.apache.log4j.Logger;

import javax.naming.NamingException;

import com.workbrain.server.registry.Registry;

public class DashMachineRegistryRule extends ErrorDetectionRule {

	private static Logger logger = Logger.getLogger(DashMachineRegistryRule.class);
	  
	public Integer getRuleType(){
		return SYSTEM_TYPE;
	}
	
	public ErrorDetectionScriptResult detect(ErrorDetectionContext context) {
		
		
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Dash Machine Registry (Deprecated in WB5.0)");
		actionResult.setHelpTip("Registry not set up/setup incorrectly for remote Dash machine");
		actionResult.setHelpDesc("A check of the Workbrain Registry and validate the path specified. The script will check for this requirement and report of any error if finds. The script will not halt execution upon discovery of such an error.");
		actionResult.setErrorMsg("FAILED: ");
		
		String result = new String("");
		
		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.4.3...");
		
		try {
			String pdfURI =  (String)Registry.getVar("system/modules/scheduleOptimization/SO_PDF_URI");
			
			java.io.File f = new java.io.File(pdfURI);
			if(!f.exists()) {
				result = "Directory " + pdfURI + " does not exist";
			}
		}
		catch(NullPointerException e) {
			if(logger.isDebugEnabled())
				logger.debug(e.getStackTrace());
			
			result = "Missing registry entry for SO_PDF_URI (Deprecated in 5.0)";
		} catch (NamingException e){
			if(logger.isDebugEnabled())
				logger.debug(e.getStackTrace());
			
			result = "Missing registry entry for SO_PDF_URI (Deprecated in 5.0)";
		}
		
		if(result.compareTo("") == 0)
			result = "OK";
		
		if(logger.isDebugEnabled())
			logger.debug("Finished check 4.4.3");
		
		actionResult.setMessage(result);
		return actionResult;
	}

	protected String getLocalizedRuleName() {
		return "Dash Machine Registry";
	}

}
