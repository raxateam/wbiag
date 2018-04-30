package com.wbiag.app.modules.retailSchedule.errorDetection;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import com.workbrain.app.modules.retailSchedule.exceptions.RetailException;
import com.workbrain.app.modules.retailSchedule.model.CorporateEntity;
import com.workbrain.app.modules.retailSchedule.model.HoursOfOperation;

public class HOPDetailsRule extends ErrorDetectionRule {

	private static Logger logger = Logger.getLogger(HOPDetailsRule.class);
	
	public Integer getRuleType(){
		return SCHEDULE_TYPE;
	}
	
	public ErrorDetectionScriptResult detect(ErrorDetectionContext context) {
		
		List corpTree = context.getCorpTree();
		
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Store Hours of Operation with Missing Details");
		actionResult.setHelpTip("Lists stores missing hour of operation details for one or more days");
		actionResult.setHelpDesc("When trying to generate a forecast for a store, if you are linking a forecast driver to an hours of operation that is missing the details for a day or all days, then a null pointer stack trace error occurs");
		actionResult.setErrorMsg("ERROR:\n");
		
		String result = new String("");
		
		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.1.5...");
		
		if(corpTree !=null){
    		for(Iterator iter = corpTree.iterator(); iter.hasNext(); )
    		{
    			try {
    				CorporateEntity cE = CorporateEntity.getCorporateEntity(new Integer((String)iter.next()));
    				
    				if(cE.getHoursOfOp() != null) {
    					HoursOfOperation locHop = cE.getHoursOfOp();
    					HashMap hopDetails = locHop.getHrSopDays();
    					
    					for(int i = 1; i <= 7; i++) {
    						if(!hopDetails.containsKey(new Integer(i)))
    							result += "-Hours of operation set " + locHop.getHrsopName() + " does not contain an entry for day " + i + "!\n";
    					}
    				}
    			}
    			catch(RetailException e) {
    				result = e.getMessage();
    			}
        	}
    	}
		
		if(result.compareTo("") == 0)
			result = "OK";
		
		if(logger.isDebugEnabled())
			logger.debug("Finished check 4.1.5");
		
		actionResult.setMessage(result);
		return actionResult;
	}

	protected String getLocalizedRuleName() {
		
		return "Store Hours of Operation with Missing Details";
	}

}
