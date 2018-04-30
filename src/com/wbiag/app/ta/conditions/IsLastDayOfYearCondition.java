package com.wbiag.app.ta.conditions;

import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.DBConnection;
import com.workbrain.util.DateHelper;

import java.util.*;

/**
 *  Title:          IsLastDayOfYearCondition
 *  Description:    Checks to see if day worked is the last day of the year
 *  Copyright:      Copyright (c) 2003
 *  Company:        Workbrain Inc
 *
 *  @author         Kevin Tsoi
 *  @version        1.0
 */

public class IsLastDayOfYearCondition extends Condition 
{
    public final static int LAST_MONTH = 11;
    public final static int LAST_DAY = 31;

    public boolean evaluate(WBData wbData, Parameters parameters) 
        throws Exception 
    {
        Calendar wrksWorkDate = DateHelper.toCalendar(wbData.getWrksWorkDate());        
        return (wrksWorkDate.get(Calendar.MONTH) == LAST_MONTH && wrksWorkDate.get(Calendar.DAY_OF_MONTH) == LAST_DAY); 
    }
    
    public List getParameterInfo( DBConnection conn ) 
    {
        ArrayList result = new ArrayList();

        return result;
    }

    public String getComponentName() 
    {
        return "WBIAG: Is Last Day Of Year Condition";
    }   
}
