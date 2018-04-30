package com.wbiag.app.ta.conditions;

import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.DBConnection;

import java.util.*;

/**
 *  Title:          WorkedNMinutesPremiumCondition
 *  Description:    A condition to check the number of minutes paid as a premium.
 *  Copyright:      Copyright (c) 2003
 *  Company:        Workbrain Inc
 *
 *  @author         Kevin Tsoi
 *  @version        1.0
 */

public class WorkedNMinutesPremiumCondition extends Condition
{
    public final static String PARAM_PREMIUM_MINUTES = "PremiumMinutes";
    public final static String PARAM_OPERATOR = "Operator";
    public final static String PARAM_TCODE_NAME_LIST = "TcodeNameList";
    public final static String PARAM_TCODE_INCLUSIVE = "TcodeInclusive";
    public final static String PARAM_HTYPE_NAME_LIST = "HtypeNameList";
    public final static String PARAM_HTYPE_INCLUSIVE = "HtypeInclusive";
    public final static String PARAM_EXPRESSION_STRING = "ExpressionString";

    public boolean evaluate(WBData wbData, Parameters parameters)
        throws Exception
    {
        int premiumMinutes;
        int minutes;
        String operator;
        String tcodeNameList;
        boolean tcodeInclusive;
        String htypeNameList;
        boolean htypeInclusive;
        String expressionString;

        premiumMinutes = parameters.getIntegerParameter(PARAM_PREMIUM_MINUTES);
        operator = parameters.getParameter(PARAM_OPERATOR);
        tcodeNameList = parameters.getParameter(PARAM_TCODE_NAME_LIST,null);
        tcodeInclusive = Boolean.valueOf(parameters.getParameter(PARAM_TCODE_INCLUSIVE,"true")).booleanValue();
        htypeNameList = parameters.getParameter(PARAM_HTYPE_NAME_LIST,null);
        htypeInclusive = Boolean.valueOf(parameters.getParameter(PARAM_HTYPE_INCLUSIVE,"true")).booleanValue();
        expressionString = parameters.getParameter(PARAM_EXPRESSION_STRING,null);

        minutes = wbData.getMinutesWorkPremium(tcodeNameList, tcodeInclusive, htypeNameList, htypeInclusive, expressionString);

        return RuleHelper.evaluate(new Integer(minutes), new Integer(premiumMinutes), operator);
    }

    public List getParameterInfo( DBConnection conn )
    {
        List result = new ArrayList();
        result.add(new RuleParameterInfo(PARAM_PREMIUM_MINUTES, RuleParameterInfo.INT_TYPE, false));
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
        result.add(new RuleParameterInfo(PARAM_EXPRESSION_STRING, RuleParameterInfo.STRING_TYPE, true));
        return result;
    }

    public String getComponentName()
    {
        return "WBIAG: Worked N Minutes Premium Condition";
    }
}
