package com.wbiag.app.ta.conditions;

import com.workbrain.app.ta.conditions.*;
import com.workbrain.sql.DBConnection;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.util.*;
import java.util.*;


/**
 *  Title:        Is Day Condition Extended
 *  Description:  Check to see if Yesterday/Today/Tomorrow is Mon/Tues/Wed/Thurs/Fri/Sat/Sun ?
 *  			  For example: Is Yesterday Sunday? Is Today Monday? Is Tomorrow Friday?
 *  Copyright:    Copyright (c) 2006
 *  Company:      Workbrain Inc
 *
 *@deprecated As of 5.0.2.0, use core classes 
 *@author     Shelley Lee
 *@version    1.0
 */
public class IsDayConditionExtended extends IsDayCondition {
    
	private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(IsDayConditionExtended.class);

    public final static String PARAM_DAY = "Day";
    public static final String DAY_TO_CHECK = "Day to Check";
    
    public List getParameterInfo(DBConnection conn) {
        List result = new ArrayList();
        
        //Day can be Mon,Tue,Wed,Thu,Fri,Sat,Sun
        result.add(new RuleParameterInfo(PARAM_DAY, RuleParameterInfo.STRING_TYPE, false));
        
        RuleParameterInfo rpi = new RuleParameterInfo(DAY_TO_CHECK, RuleParameterInfo.CHOICE_TYPE, false);
        rpi.addChoice("Yesterday");
        rpi.addChoice("Today");
        rpi.addChoice("Tomorrow");
        result.add(rpi);
        
        return result;
    }

    public boolean evaluate(WBData wbData, Parameters parameters) throws Exception {
        String day = parameters.getParameter(PARAM_DAY);
        String dayToCheck = parameters.getParameter(DAY_TO_CHECK);
        int dayOfWeek;
        
        if (StringHelper.isEmpty(day)) {
        	 
            throw new RuntimeException ("Day cannot be null");
        }
        if (StringHelper.isEmpty(dayToCheck)) {
        	 
        	//throw new RuntimeException ("Day To Check cannot be null");
        	dayToCheck = "Today"; 
        }
        //get the current calendar day
        dayOfWeek = DateHelper.dayOfWeek(wbData.getWrksWorkDate());

        // if dayToCheck = TODAY, don't need to do anything
        
        if("Yesterday".equalsIgnoreCase(dayToCheck))    //Check if Yesterday is DAY
		{			
			if (dayOfWeek == 0) dayOfWeek = 6;
			else	dayOfWeek= dayOfWeek - 1;
		}
		else if("Tomorrow".equalsIgnoreCase(dayToCheck))  //Check if Tomorrow is DAY
		{			
			if (dayOfWeek == 6)dayOfWeek = 0;
			else	dayOfWeek= dayOfWeek + 1;
		}
        //See if DAY = DAY TO CHECK
        //getCalendar day has SUN = 1, MON = 2, etc.
        //dayofWeek has SUN = 0, MON = 1, etc
        boolean res = (DateHelper.getCalendarDay(day) - 1 == dayOfWeek);  
		
        return res;
    }

    public String getComponentName() {
        return "WBIAG:Is Day Condition Extended";
    }

    public String getDescription() {
        return "Determines if Yesterday/Today/Tomorrow is the calculation date";
    }
}



