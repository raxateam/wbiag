package com.wbiag.app.ta.conditions;

import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.DBConnection;
import com.workbrain.util.*;

import java.util.*;

/**
 *  Title:          WorkedNMinutesInMonthCondition
 *  Description:    A condition to check the number of minutes worked so far in the current month.
 *  Copyright:      Copyright (c) 2003
 *  Company:        Workbrain Inc
 *
 *  @author         Kevin Tsoi
 *  @version        1.0
 */

public class WorkedNMinutesInMonthCondition extends Condition
{
    public final static String PARAM_WORK_MINUTES = "WorkMinutes";
    public final static String PARAM_OPERATOR = "Operator";
    public final static String PARAM_TCODE_NAME_LIST = "TcodeNameList";
    public final static String PARAM_TCODE_INCLUSIVE = "TcodeInclusive";
    public final static String PARAM_HTYPE_NAME_LIST = "HtypeNameList";
    public final static String PARAM_HTYPE_INCLUSIVE = "HtypeInclusive";
    public final static String PARAM_INCLUDE_CURRENT_WORK_DATE = "IncludeCurrentWorkDate";

    public boolean evaluate(WBData wbData, Parameters parameters)
        throws Exception
    {
        int minutes;
        int requiredMinutes;
        String operator;
        String tcodeNameList;
        boolean tcodeInclusive;
        String htypeNameList;
        boolean htypeInclusive;
        Date currentDay;
        Calendar startOfMonth;

        requiredMinutes = Integer.parseInt(parameters.getParameter(PARAM_WORK_MINUTES));
        operator = parameters.getParameter(PARAM_OPERATOR);
        tcodeNameList = parameters.getParameter(PARAM_TCODE_NAME_LIST, null);
        tcodeInclusive = Boolean.valueOf(parameters.getParameter(PARAM_TCODE_INCLUSIVE,"true")).booleanValue();
        htypeNameList = parameters.getParameter(PARAM_HTYPE_NAME_LIST, null);
        htypeInclusive = Boolean.valueOf(parameters.getParameter(PARAM_HTYPE_INCLUSIVE,"true")).booleanValue();

        boolean inclCurrDay = Boolean.valueOf(parameters.getParameter(PARAM_INCLUDE_CURRENT_WORK_DATE,"true")).booleanValue();
        if (inclCurrDay) {
            currentDay = wbData.getWrksWorkDate();
        }
        else {
            currentDay = DateHelper.addDays(wbData.getWrksWorkDate() , -1);
        }

        startOfMonth = DateHelper.toCalendar(currentDay);
        startOfMonth.set(Calendar.DAY_OF_MONTH, 1);

        minutes = wbData.getMinutesWorkDetailRange(startOfMonth.getTime(), currentDay, null, null, tcodeNameList, tcodeInclusive, htypeNameList, htypeInclusive);

        return RuleHelper.evaluate(new Integer(minutes), new Integer(requiredMinutes), operator);
    }

    public List getParameterInfo( DBConnection conn )
    {
        List result = new ArrayList();
        result.add(new RuleParameterInfo(PARAM_WORK_MINUTES, RuleParameterInfo.INT_TYPE, false));
        RuleParameterInfo rpi = new RuleParameterInfo(PARAM_OPERATOR, RuleParameterInfo.CHOICE_TYPE, false);
        rpi.addChoice(RuleHelper.EQ);
        rpi.addChoice(RuleHelper.LESS);
        rpi.addChoice(RuleHelper.BIGGER);
        rpi.addChoice(RuleHelper.LESSEQ);
        rpi.addChoice(RuleHelper.BIGGEREQ);
        rpi.addChoice(RuleHelper.NOTEQ1);
        result.add(rpi);
        result.add(new RuleParameterInfo(PARAM_TCODE_NAME_LIST, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_TCODE_INCLUSIVE, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_HTYPE_NAME_LIST, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_HTYPE_INCLUSIVE, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_INCLUDE_CURRENT_WORK_DATE, RuleParameterInfo.STRING_TYPE, true));
        return result;
    }

    public String getComponentName()
    {
        return "WBIAG: Worked N Minutes In Month Condition";
    }
}
