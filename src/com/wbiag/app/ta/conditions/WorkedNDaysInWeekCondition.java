package com.wbiag.app.ta.conditions;

import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.DBConnection;
import com.workbrain.util.*;
import com.workbrain.server.registry.Registry;

import java.util.*;

/**
 *  Title:          WorkedNDaysInWeekCondition
 *  Description:    A condition to check the number of days worked since the
 *                  beginning of the work week.
 *  Copyright:      Copyright (c) 2003
 *  Company:        Workbrain Inc
 *
 *  @author         Kevin Tsoi
 *  @version        1.0
 */

public class WorkedNDaysInWeekCondition extends Condition
{
    public final static String PARAM_NUM_OF_DAYS = "NumberOfDays";
    public final static String PARAM_OPERATOR = "Operator";
    public final static String PARAM_WORK_MINUTES = "WorkMinutes";
    public final static String PARAM_TCODENAME_LIST = "TcodeNameList";
    public final static String PARAM_TCODE_INCLUSIVE = "TcodeInclusive";
    public final static String PARAM_HTYPENAME_LIST = "HtypeNameList";
    public final static String PARAM_HTYPE_INCLUSIVE = "HtypeInclusive";
    public final static String PARAM_DETAIL_PREMIUM = "DetailPremium";
    public final static String PARAM_INCLUDE_CURRENT_WORK_DATE = "IncludeCurrentWorkDate";

    public final static String RPARAM_DAY_WEEK_STARTS = "system/WORKBRAIN_PARAMETERS/DAY_WEEK_STARTS";

    public boolean evaluate(WBData wbData, Parameters parameters)
        throws Exception
    {
        Date wrksDay;

        int requiredDays = Integer.parseInt(parameters.getParameter(PARAM_NUM_OF_DAYS));
        String operator = parameters.getParameter(PARAM_OPERATOR);
        int requiredMinutes = parameters.getIntegerParameter(PARAM_WORK_MINUTES, 1);
        String tcodeNameList = parameters.getParameter(PARAM_TCODENAME_LIST,null);
        boolean tcodeInclusive = Boolean.valueOf(parameters.getParameter(PARAM_TCODE_INCLUSIVE,"true")).booleanValue();
        String htypeNameList = parameters.getParameter(PARAM_HTYPENAME_LIST,null);
        boolean htypeInclusive = Boolean.valueOf(parameters.getParameter(PARAM_HTYPE_INCLUSIVE,"true")).booleanValue();
        String detailPremium = parameters.getParameter(PARAM_DETAIL_PREMIUM,null);
        String startDay = Registry.getVarString(RPARAM_DAY_WEEK_STARTS);
        boolean inclCurrDay = Boolean.valueOf(parameters.getParameter(PARAM_INCLUDE_CURRENT_WORK_DATE,"true")).booleanValue();
        if (inclCurrDay) {
            wrksDay = wbData.getWrksWorkDate();
        }
        else {
            wrksDay = DateHelper.addDays(wbData.getWrksWorkDate() , -1);
        }
        Date startOfWeek = DateHelper.nextDay(DateHelper.addDays(wrksDay, -7), startDay);
        int days = wbData.getCountWorkSummaryRange(0,
                                               startOfWeek, wrksDay, null, null,
                                               tcodeNameList, tcodeInclusive,
                                               htypeNameList, htypeInclusive, detailPremium,
                                               requiredMinutes);
        return RuleHelper.evaluate(new Integer(days), new Integer(requiredDays), operator);
    }

    public List getParameterInfo( DBConnection conn )
    {
        List result = new ArrayList();
        result.add(new RuleParameterInfo(PARAM_NUM_OF_DAYS, RuleParameterInfo.INT_TYPE, false));
        RuleParameterInfo rpi = new RuleParameterInfo(PARAM_OPERATOR, RuleParameterInfo.CHOICE_TYPE, false);
        rpi.addChoice(RuleHelper.EQ);
        rpi.addChoice(RuleHelper.LESS);
        rpi.addChoice(RuleHelper.BIGGER);
        rpi.addChoice(RuleHelper.LESSEQ);
        rpi.addChoice(RuleHelper.BIGGEREQ);
        rpi.addChoice(RuleHelper.NOTEQ1);
        result.add(rpi);
        result.add(new RuleParameterInfo(PARAM_WORK_MINUTES, RuleParameterInfo.INT_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_TCODENAME_LIST, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_TCODE_INCLUSIVE, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_HTYPENAME_LIST, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_HTYPE_INCLUSIVE, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_DETAIL_PREMIUM, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_INCLUDE_CURRENT_WORK_DATE, RuleParameterInfo.STRING_TYPE, true));
        return result;
    }

    public String getComponentName()
    {
        return "WBIAG: Worked N Days In Week Condition";
    }
}
