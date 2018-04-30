package com.wbiag.app.modules.retailSchedule.errorDetection;

import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import com.workbrain.app.modules.retailSchedule.exceptions.RetailException;
import com.workbrain.app.modules.retailSchedule.model.ScheduleGroupData;
import com.workbrain.app.modules.retailSchedule.type.InternalType;

public class LocationPropertiesRule extends ErrorDetectionRule {

	private static Logger logger = Logger.getLogger(LocationPropertiesRule.class);
	
	public Integer getRuleType(){
		return SCHEDULE_TYPE;
	}
	
	public ErrorDetectionScriptResult detect(ErrorDetectionContext context) {
		
		List corpTree = context.getCorpTree();
		
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Location Properties");
		actionResult.setHelpTip("Check the checkboxes associated to each location");
		actionResult.setHelpDesc("There is a set of 5 checkboxes on the Location Properties page. These must be set according to other items in the Location settings. For example, if the Volume Type is set to Revenue, the Sales Based Distribution checkbox should be activated.");
		actionResult.setErrorMsg("FAILED:\n");
		
		String result = new String("");
		
		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.2.6...");
		
		try {
			if(corpTree !=null)
		    {
		        for( Iterator iter = corpTree.iterator(); iter.hasNext(); )
		        {
		            ScheduleGroupData tempSkdgrpData = ScheduleGroupData.getScheduleGroupData(new Integer((String)iter.next()));
					Integer type = new Integer(tempSkdgrpData.getSkdgrpIntrnlType());

		            if( InternalType.isStore(type) )
		            {
		                if(!( tempSkdgrpData.getSkdgrpVolsub() == 0 &&
		                    tempSkdgrpData.getSkdgrpHistdatSub() == 1 &&
		                    tempSkdgrpData.getSkdgrpOptchkskd() == 1 &&
		                    tempSkdgrpData.getSkdgrpSalesBased() == 1))
		                {
		                	result += "The following need to be checked/unchecked for store '" + tempSkdgrpData.getSkdgrpName() + "'\n";
		                	
		                    if(tempSkdgrpData.getSkdgrpVolsub() != 0)
		                    	result += "-'Volume is subset of parent volume' should be unchecked\n";
		                    if(tempSkdgrpData.getSkdgrpHistdatSub() != 1)
		                    	result += "-'Historical Data' should be checked\n";
		                    if(tempSkdgrpData.getSkdgrpOptchkskd() != 1)
		                    	result += "-'Check schedule assignments' should be checked\n";
		                    if(tempSkdgrpData.getSkdgrpSalesBased() != 1)
		                    	result += "-'Sales based distribution' should be checked\n";
		                }
		            }
		            else if( InternalType.isScheduleArea(type) )
		            {
		                if(!( tempSkdgrpData.getSkdgrpVolsub() == 1 &&
		                	tempSkdgrpData.getSkdgrpOptchkskd() == 0 &&
		                	tempSkdgrpData.getSkdgrpSalesBased() == 0 ))
		                {
                			result += "The following need to be checked/unchecked for schedule department '" + tempSkdgrpData.getSkdgrpName() + "'\n";
		                	
		                    if(tempSkdgrpData.getSkdgrpVolsub() != 1)
		                    	result += "-'Volume is subset of parent volume' should be checked\n";
		                    if(tempSkdgrpData.getSkdgrpOptchkskd() != 0)
		                    	result += "-'Check schedule assignments' should be unchecked\n";
		                    if(tempSkdgrpData.getSkdgrpSalesBased() != 0)
		                    	result += "-'Sales based distribution' should be unchecked\n";
		                }
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
			logger.debug("Finished check 4.2.6");
		
		actionResult.setMessage(result);
		return actionResult;
	}

	protected String getLocalizedRuleName() {
		return "Location Properties";
	}

}
