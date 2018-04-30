package com.wbiag.app.modules.retailSchedule.errorDetection;

import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import com.workbrain.app.modules.retailSchedule.exceptions.RetailException;
import com.workbrain.app.modules.retailSchedule.model.CorporateEntity;
import com.workbrain.app.modules.retailSchedule.model.ScheduleGroupData;
import com.workbrain.app.modules.retailSchedule.utils.SODate;

public class FiscalYearMatchRule extends ErrorDetectionRule {


	private static Logger logger = Logger.getLogger(FiscalYearMatchRule.class);
	  
	public Integer getRuleType(){
		return FORECAST_TYPE;
	}
	
	public ErrorDetectionScriptResult detect(ErrorDetectionContext context) {
		
		List corpTree = context.getCorpTree();
		
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Fiscal Year Match");
		actionResult.setHelpTip("Fiscal Year date must be set so that the day of week matches the location start day of week");
		actionResult.setHelpDesc("If the Fiscal Year Date day of week is not the same as the day of week that is the start of locations, then there will be an offset problem when the forecast is being calculated for previous weeks. These have to be the same.");
		actionResult.setErrorMsg("FAILED:\n");
		
		String result = new String("");
		
		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.2.8...");
		
		Integer thisYear = new Integer(Calendar.getInstance().getTime().getYear() + 1900);
		
		if(corpTree !=null){
			for(Iterator iter = corpTree.iterator(); iter.hasNext(); )
    		{
    			try {
    				CorporateEntity cE = CorporateEntity.getCorporateEntity(new Integer((String)iter.next()));
    				ScheduleGroupData tempSkdgrpData = cE.getSkdgrpData();
    				
    				//days of the week range from 1..7 in WB and in java
    				//1=sun and 7=sat in both systems
    				//so, we can do a direct comparison
					SODate startOfYear = cE.getFiscalYearStartDate(thisYear);
					int startOfWeek = tempSkdgrpData.getSkdgrpStartdow();
					
					if(startOfYear != null) {
						if(startOfYear.getDayOfWeek() != startOfWeek) {
							result += "-Start Day of Week does not match Fiscal Year Start Day of Week for location " + tempSkdgrpData.getSkdgrpName() + "\n";
						}
					}
					else {
						if(cE.getParent() != null)
							result += checkSOW(cE.getParent(), tempSkdgrpData);
						else
							result += "-No Fiscal Year Start Day of Week defined for location " + tempSkdgrpData.getSkdgrpName() + " and no parent defined for a parent check\n";
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
			logger.debug("Finished check 4.2.8");
		
		actionResult.setMessage(result);
		return actionResult;
	}

	protected String getLocalizedRuleName() {
		return "Fiscal Year Match";
	}
	
	private String checkSOW(CorporateEntity cE, ScheduleGroupData originalLocation) {
    	String result = "";
    	Integer thisYear = new Integer(Calendar.getInstance().getTime().getYear() + 1900);
    	
    	try {
			ScheduleGroupData tempSkdgrpData = cE.getSkdgrpData();
			
			SODate startOfYear = cE.getFiscalYearStartDate(thisYear);
			int startOfWeek = tempSkdgrpData.getSkdgrpStartdow();
			
			if(startOfYear != null) {
				if(startOfYear.getDayOfWeek() != startOfWeek) {
					result += "-No Fiscal Year Start Day of Week defined for location " + originalLocation.getSkdgrpName() + " and Start Day of Week does not match Fiscal Year Start Day of Week for parent location " + tempSkdgrpData.getSkdgrpName() + "\n";
				}
			}
			else {
				if(cE.getParent() != null)
					result += checkSOW(cE.getParent(), originalLocation);
				else
					result += "-No Fiscal Year Start Day of Week defined for location " + originalLocation.getSkdgrpName() + " or for any parent location\n";
			}
		}
		catch(RetailException e) {
			if(logger.isDebugEnabled())
				logger.debug(e.getStackTrace());
			
			result = e.getMessage();
		}
		
		return result;
    }

}
