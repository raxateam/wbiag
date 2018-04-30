
package com.wbiag.app.ta.conditions;

import com.workbrain.app.ta.conditions.*;
import com.workbrain.sql.DBConnection;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.util.DateHelper;
import com.workbrain.util.StringHelper;

import java.util.*;

/**
 *  Title:        Is Regularly Scheduled Day Condition Extended
 *  Description:  Check to see if Yesterday/Today/Tomorrow is a regular scheduled day
 *  Copyright:    Copyright (c) 2006
 *  Company:      Workbrain Inc
 *  TT: 1292
 *
 *@author     Shelley Lee
 *@version    1.0
 */

public class IsRegularlyScheduledDayConditionExtended extends IsRegularlyScheduledDayCondition {
    
	private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(IsRegularlyScheduledDayConditionExtended.class);
    public static final String DAY_TO_CHECK = "Day to Check";
    
    public List getParameterInfo( DBConnection conn ) {
    	List result = new ArrayList();
        
    	RuleParameterInfo rpi = new RuleParameterInfo(DAY_TO_CHECK, RuleParameterInfo.CHOICE_TYPE, false);
        rpi.addChoice("Yesterday");
        rpi.addChoice("Today");
        rpi.addChoice("Tomorrow");
        result.add(rpi);
    	return result;
    }

    public boolean evaluate(WBData wbData, Parameters parameters) throws Exception {
    	
    	String dayToCheck = parameters.getParameter(DAY_TO_CHECK);
    	
    	if (StringHelper.isEmpty(dayToCheck)) {       	 
        	//throw new RuntimeException ("Day To Check cannot be null");
        	dayToCheck = "Today"; 
        }
    	
    	boolean isRegularDay;
    	Date d;
    	
    	if("Yesterday".equalsIgnoreCase(dayToCheck))    //Check if Yesterday is DAY
		{			
			d = DateHelper.addDays(wbData.getRuleData().getWrksWorkDate(),-1);
		}
		else if("Tomorrow".equalsIgnoreCase(dayToCheck))  //Check if Tomorrow is DAY
		{			
			d = DateHelper.addDays(wbData.getRuleData().getWrksWorkDate(),1);
		}
		else{
			d = wbData.getRuleData().getWrksWorkDate();
		}
		isRegularDay = wbData.getEmployeeScheduleData(d).isEmployeeScheduledActual();    	
    	return isRegularDay;
    }

    public String getComponentName() {
        return "WBIAG: Is Regularly Scheduled Day Condition Extended";
    }
    
	public String getDescription() {
		return "Applies if employee is scheduled to work Yesterday/Today/Tomorrow";
	}

}

