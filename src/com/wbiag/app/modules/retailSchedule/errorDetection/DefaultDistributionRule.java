package com.wbiag.app.modules.retailSchedule.errorDetection;

import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import com.workbrain.app.modules.retailSchedule.exceptions.RetailException;
import com.workbrain.app.modules.retailSchedule.model.Distribution;
import com.workbrain.app.modules.retailSchedule.model.ScheduleGroupData;
import com.workbrain.app.modules.retailSchedule.type.InternalType;

public class DefaultDistributionRule extends ErrorDetectionRule {


	private static Logger logger = Logger.getLogger(RootNodeCheckRule.class);
	  
	public Integer getRuleType(){
		return SCHEDULE_TYPE;
	}
	
	public ErrorDetectionScriptResult detect(ErrorDetectionContext context) {
		
		List corpTree = context.getCorpTree();
		
		ErrorDetectionScriptResult actionResult = new ErrorDetectionScriptResult();
		actionResult.setHelpTitle("Default Distribution");
		actionResult.setHelpTip("If forecast driver, check for default distribution (flag if flat)");
		actionResult.setHelpDesc("Typically a LFSO user will create distributions for their departments to use. There are times however when a Flat distribution is acceptable. The user will be notified which forecast drivers have Default Distributions set to Flat. This error will not halt execution of script and only report a warning message.");
		actionResult.setErrorMsg("WARNING: The following departments have the default distributation set to FLAT: ");
		
		String result = new String("");
		
		if(logger.isDebugEnabled())
			logger.debug("Starting check 4.3.3...");
		
		if(corpTree !=null)
	    {
	        for( Iterator iter = corpTree.iterator(); iter.hasNext(); )
	        {
	        	try {
		            ScheduleGroupData tempSkdgrpData = ScheduleGroupData.getScheduleGroupData(new Integer((String)iter.next()));
					int type = tempSkdgrpData.getSkdgrpIntrnlType();
	
		            if( InternalType.isDriver(new Integer(type)) )
		            {
		                if( tempSkdgrpData.getSkdgrpSunDistId() == Distribution.FLAT_DIST_ID )
		                {
		    			    if( result.length() == 0 )
		    			        result += tempSkdgrpData.getSkdgrpName();
		    			    else
		    			        result += ", " + tempSkdgrpData.getSkdgrpName() + "(Sun)";
		                }
		                if( tempSkdgrpData.getSkdgrpMonDistId() == Distribution.FLAT_DIST_ID )
		                {
		    			    if( result.length() == 0 )
		    			        result += tempSkdgrpData.getSkdgrpName();
		    			    else
		    			        result += ", " + tempSkdgrpData.getSkdgrpName() + "(Mon)";
		                }
		                if( tempSkdgrpData.getSkdgrpTueDistId() == Distribution.FLAT_DIST_ID )
		                {
		    			    if( result.length() == 0 )
		    			        result += tempSkdgrpData.getSkdgrpName();
		    			    else
		    			        result += ", " + tempSkdgrpData.getSkdgrpName() + "(Tue)";
		                }
		                if( tempSkdgrpData.getSkdgrpWedDistId() == Distribution.FLAT_DIST_ID )
		                {
		    			    if( result.length() == 0 )
		    			        result += tempSkdgrpData.getSkdgrpName();
		    			    else
		    			        result += ", " + tempSkdgrpData.getSkdgrpName() + "(Wed)";
		                }
		                if( tempSkdgrpData.getSkdgrpThuDistId() == Distribution.FLAT_DIST_ID )
		                {
		    			    if( result.length() == 0 )
		    			        result += tempSkdgrpData.getSkdgrpName();
		    			    else
		    			        result += ", " + tempSkdgrpData.getSkdgrpName() + "(Thu)";
		                }
		                if( tempSkdgrpData.getSkdgrpFriDistId() == Distribution.FLAT_DIST_ID )
		                {
		    			    if( result.length() == 0 )
		    			        result += tempSkdgrpData.getSkdgrpName();
		    			    else
		    			        result += ", " + tempSkdgrpData.getSkdgrpName() + "(Fri)";
		                }
		                if( tempSkdgrpData.getSkdgrpSatDistId() == Distribution.FLAT_DIST_ID )
		                {
		    			    if( result.length() == 0 )
		    			        result += tempSkdgrpData.getSkdgrpName();
		    			    else
		    			        result += ", " + tempSkdgrpData.getSkdgrpName() + "(Sat)";
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
			logger.debug("Finished check 4.3.3");
		
		actionResult.setMessage(result);
		return actionResult;
	}

	protected String getLocalizedRuleName() {
		return "Default Distribution";
	}

}
