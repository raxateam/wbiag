package com.wbiag.app.ta.conditions;

import com.workbrain.app.ta.conditions.*;
import com.workbrain.sql.DBConnection;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.util.DateHelper;
import java.util.*;

/**
 *  Title:        Is Weekend Day Extended Condition
 *  Description:
 *  Copyright:    Copyright (c) 2006
 *  Company:      Workbrain Inc
 *
 *@deprecated As of 5.0.2.0, use core classes 
 *@author     Manisha Luthra
 *@version    1.0
 */

public class IsWeekendDayExtended extends IsWeekendDayCondition {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(IsWeekendDayExtended.class);
    public static final String DAY_TO_CHECK = "Day to Check";

    public boolean evaluate(WBData wbData, Parameters parameters) throws Exception {
        
		String op = parameters.getParameter(DAY_TO_CHECK);
		int dayOfWeek;
		
		/*Check if Yesterday is either Saturday or Sunday*/
		if("Yesterday".equalsIgnoreCase(op))
		{
			dayOfWeek = DateHelper.dayOfWeek(wbData.getRuleData().getWorkSummary().getWrksWorkDate());
			if (dayOfWeek == 0)
				dayOfWeek = 6;
			else
				dayOfWeek= dayOfWeek - 1;
		}
		else if("Tomorrow".equalsIgnoreCase(op))  /*Check if Tomorrow is either Saturday or Sunday*/
		{
			dayOfWeek = (DateHelper.dayOfWeek(wbData.getRuleData().getWorkSummary().getWrksWorkDate()));

			if (dayOfWeek == 6)
				dayOfWeek = 0;
			else
				dayOfWeek= dayOfWeek + 1;
		}
		else /*If value not selected Default to Today and check if today is either Saturday or Sunday*/
		{
			dayOfWeek = DateHelper.dayOfWeek(wbData.getRuleData().getWorkSummary().getWrksWorkDate());
		}

        return dayOfWeek == 0 || dayOfWeek == 6;
    }

    public List getParameterInfo( DBConnection conn ) {
        List result = new ArrayList();
        RuleParameterInfo rpi = new RuleParameterInfo(DAY_TO_CHECK, RuleParameterInfo.CHOICE_TYPE, false);
        rpi.addChoice("Yesterday");
        rpi.addChoice("Today");
        rpi.addChoice("Tomorrow");
        result.add(rpi);
        return result;

	}

    public String getComponentName() {
        return "WBIAG: Is Weekend Day Extended Condition";
    }
    
	public String getDescription() {
		return "Applies if it is a weekend day on that day";
	}
}
