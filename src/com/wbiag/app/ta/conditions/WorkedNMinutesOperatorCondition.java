package com.wbiag.app.ta.conditions;

import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.app.ta.model.WorkDetailList;
import com.workbrain.app.ta.model.WorkDetailData;
import com.workbrain.sql.DBConnection;
import com.workbrain.util.*;
import com.workbrain.server.registry.Registry;

import java.util.*;

/**
 *  Title:          WorkedNMinutesOperatorCondition
 *  Description:    A condition to check the number of minutes worked in a day.
 *                  This is an extension of the core WorkedNMinutes Condition with
 *                  the addition of the Operator parameter.
 *  Copyright:      Copyright (c) 2003
 *  Company:        Workbrain Inc
 *
 *  @author         Kevin Tsoi
 *  @version        1.0
 */

public class WorkedNMinutesOperatorCondition extends Condition
{

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(WorkedNMinutesOperatorCondition.class);
    public final static String PARAM_WORK_MINUTES = "WorkMinutes";
    public final static String PARAM_OPERATOR = "Operator";
    public final static String PARAM_TCODENAME_LIST = "TcodeNameList";
    public final static String PARAM_TCODE_INCLUSIVE = "TcodeInclusive";
    public final static String PARAM_HTYPENAME_LIST = "HtypeNameList";
    public final static String PARAM_HTYPE_INCLUSIVE = "HtypeInclusive";
    public final static String PARAM_EXPRESSION_STRING = "ExpressionString";
    public final static String PARAM_DETAIL_PREMIUM = "DetailPremium";
    public final static String PARAM_FROM_DAY = "FromDay";
    public final static String PARAM_ANY_OCCURENCE = "AnyOccurence";


    public boolean evaluate(WBData wbData, Parameters parameters)
        throws Exception
    {


        ParametersResolved pars = getParametersResolved(parameters);
        if (!pars.isAnyOccurence) {
            if (logger.isDebugEnabled()) logger.debug("Running WorkedNMinutesOperatorCondition with AnyOccurence=false");
            return processAnyOccurenceFalse(wbData, pars);
        }
        else {
            if (logger.isDebugEnabled()) logger.debug("Running WorkedNMinutesOperatorCondition with AnyOccurence=true");
            return processAnyOccurenceTrue(wbData, pars);
        }

    }

    /**
     * Checks if worked minutes is satisfied based on summation of all work details/premiums
     * @param wbData WBData
     * @param pars ParametersResolved
     * @return boolean
     * @throws Exception
     */
    private boolean processAnyOccurenceFalse(WBData wbData,
        ParametersResolved pars) throws Exception{
        int mins = 0;
        // *** if startDayOfTheWeek supplied, sum for the week
        if (StringHelper.isEmpty(pars.startDayOfTheWeek))
        {
            if (pars.detailPremium == null || pars.detailPremium.equals(WorkDetailData.DETAIL_TYPE))
            {
                mins += wbData.getMinutesWorkDetail(null,null,pars.tcodeNameList,pars.tcodeInclusive,
                            pars.htypeNameList,pars.htypeInclusive,pars.extraCondition);
            }
            if (pars.detailPremium == null || pars.detailPremium.equals(WorkDetailData.PREMIUM_TYPE))
            {
                mins += wbData.getMinutesWorkPremium(pars.tcodeNameList,pars.tcodeInclusive,
                            pars.htypeNameList,pars.htypeInclusive,pars.extraCondition);
            }
        }
        else
        {
            Date dateWeekStarts = DateHelper.nextDay(
                new Date(wbData.getRuleData().getWorkSummary().getWrksWorkDate().getTime() - 7 * DateHelper.DAY_MILLISECODS),
                         pars.startDayOfTheWeek);
            mins = wbData.getMinutesWorkDetailPremiumRange( dateWeekStarts ,
                    wbData.getRuleData().getWorkSummary().getWrksWorkDate(),
                    null, null,
                    pars.tcodeNameList , pars.tcodeInclusive,
                    pars.htypeNameList , pars.htypeInclusive,
                    pars.detailPremium);
        }
        return RuleHelper.evaluate(new Integer(mins),
                                   new Integer(pars.compareMins), pars.operator);

    }

    /**
     * Checks if any of the details satisfy given condition
     * @param wbData WBData
     * @param pars ParametersResolved
     * @return boolean
     * @throws Exception
     */
    private boolean processAnyOccurenceTrue(WBData wbData,
        ParametersResolved pars) throws Exception{
        boolean ret = false;
        // *** if startDayOfTheWeek supplied, sum for the week
        if (StringHelper.isEmpty(pars.startDayOfTheWeek))
        {
            if (pars.detailPremium == null || pars.detailPremium.equals(WorkDetailData.DETAIL_TYPE))
            {
                ret = processMinutesWorkDetail(wbData , pars, wbData.getRuleData().getWorkDetails());
                if (ret) {
                    return ret;
                }
            }
            if (pars.detailPremium == null || pars.detailPremium.equals(WorkDetailData.PREMIUM_TYPE))
            {
                ret = processMinutesWorkDetail(wbData , pars, wbData.getRuleData().getWorkPremiums());
                if (ret) {
                    return ret;
                }
            }
        }
        else
        {
            Date dateWeekStarts = DateHelper.nextDay(
                new Date(wbData.getRuleData().getWorkSummary().getWrksWorkDate().getTime() - 7 * DateHelper.DAY_MILLISECODS),
                         pars.startDayOfTheWeek);
            for (Date date = dateWeekStarts; date.compareTo(wbData.getRuleData().getWorkSummary().getWrksWorkDate()) <= 0;
                         date = DateHelper.addDays(date, 1)) {
                    if (pars.detailPremium == null || pars.detailPremium.equals(WorkDetailData.DETAIL_TYPE))
                    {
                        ret = processMinutesWorkDetail(wbData , pars,
                            wbData.getWorkDetailsOrPremiumsForDate(date, true)  );
                        if (ret) {
                            return ret;
                        }
                    }
                    if (pars.detailPremium == null || pars.detailPremium.equals(WorkDetailData.PREMIUM_TYPE))
                    {
                        ret = processMinutesWorkDetail(wbData , pars,
                             wbData.getWorkDetailsOrPremiumsForDate(date, false));
                        if (ret) {
                            return ret;
                        }
                    }

            }
        }
        return ret;
    }

    /**
     * Check if any work details satisfies given parameters
     * @param wbData wbData
     * @param pars ParametersResolved
     * @param wdl WorkDetailList
     * @return boolean
     * @throws Exception
     */
    private boolean processMinutesWorkDetail(WBData wbData,
                                             ParametersResolved pars,
                                             WorkDetailList wdl) throws Exception{
        boolean eval = false;
        for (int i = 0, j = wdl.size() ; i < j; i++) {
            String sHtypeName = wdl.getWorkDetail(i).getWrkdHtypeName();
            String sTcodeName = wdl.getWorkDetail(i).getWrkdTcodeName();
            if ((RuleHelper.isCodeInList(pars.htypeNameList, sHtypeName)== pars.htypeInclusive) &&
                    (RuleHelper.isCodeInList(pars.tcodeNameList, sTcodeName) == pars.tcodeInclusive) &&
                        wdl.getWorkDetail(i).evaluateExpression(pars.extraCondition)) {
                        eval = RuleHelper.evaluate(new Integer(wdl.getWorkDetail(i).getWrkdMinutes()),
                                   new Integer(pars.compareMins), pars.operator);
                        if (eval) {
                            break;
                        }
            }
        }

        return eval;

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
        result.add(new RuleParameterInfo(PARAM_TCODENAME_LIST, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_TCODE_INCLUSIVE, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_HTYPENAME_LIST, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_HTYPE_INCLUSIVE, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_EXPRESSION_STRING, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_DETAIL_PREMIUM, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_FROM_DAY, RuleParameterInfo.STRING_TYPE, true));
        return result;
    }



    private ParametersResolved getParametersResolved(Parameters parameters) {
        ParametersResolved pars = new ParametersResolved();
        pars.compareMins = parameters.getIntegerParameter(PARAM_WORK_MINUTES);
        pars.operator = parameters.getParameter(PARAM_OPERATOR);
        pars.tcodeNameList = parameters.getParameter(PARAM_TCODENAME_LIST,null);
        pars.tcodeInclusive = Boolean.valueOf(parameters.getParameter(PARAM_TCODE_INCLUSIVE,"true")).booleanValue();
        pars.htypeNameList = parameters.getParameter(PARAM_HTYPENAME_LIST,null);
        pars.htypeInclusive = Boolean.valueOf(parameters.getParameter(PARAM_HTYPE_INCLUSIVE,"true")).booleanValue();
        pars.extraCondition = parameters.getParameter(PARAM_EXPRESSION_STRING,null);
        pars.detailPremium = parameters.getParameter(PARAM_DETAIL_PREMIUM,null);
        pars.startDayOfTheWeek = parameters.getParameter(PARAM_FROM_DAY,null);
        pars.isAnyOccurence = Boolean.valueOf(parameters.getParameter(PARAM_ANY_OCCURENCE, "false" )).booleanValue();
        return pars;
    }

    public class ParametersResolved {
        public int compareMins;
        public String operator;
        public String tcodeNameList;
        public boolean tcodeInclusive;
        public String htypeNameList;
        public boolean htypeInclusive;
        public String extraCondition;
        public String detailPremium;
        public String startDayOfTheWeek;
        public boolean isAnyOccurence = false;
    }

    public String getComponentName()
    {
        return "WBIAG: Worked N Minutes Operator Condition";
    }
}
