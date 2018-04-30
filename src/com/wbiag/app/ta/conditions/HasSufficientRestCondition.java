package com.wbiag.app.ta.conditions;

import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.DBConnection;
import com.workbrain.util.*;

import java.util.*;

/**
 *  Title:          HasSufficientRest
 *  Description:    A condition to check if emp has sufficient rest based on parameters.
 */
public class HasSufficientRestCondition extends Condition
{
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(HasSufficientRestCondition.class);

    public final static String PARAM_REST_MINUTES = "RestMinutes";
    public final static String PARAM_YESTERDAY_MUST_WORK_MINUTES = "YesterdayMustWorkMinutes";
    public final static String PARAM_WORK_MINUTES_CONSECUTIVE = "WorkMinutesConsecutive";
    public final static String PARAM_TCODENAME_LIST = "TcodeNameList";
    public final static String PARAM_TCODE_INCLUSIVE = "TcodeInclusive";
    public final static String PARAM_HTYPENAME_LIST = "HtypeNameList";
    public final static String PARAM_HTYPE_INCLUSIVE = "HtypeInclusive";
    public final static String PARAM_OPERATOR = "Operator";
    
    public boolean evaluate(WBData wbData, Parameters parameters)
        throws Exception
    {
        int minRestMinutes = parameters.getIntegerParameter(PARAM_REST_MINUTES,0);
        int yesterdayMustWorkMinutes = parameters.getIntegerParameter(PARAM_YESTERDAY_MUST_WORK_MINUTES,0);
        boolean workMinutesConsecutive = Boolean.valueOf(parameters.getParameter(PARAM_WORK_MINUTES_CONSECUTIVE, "false")).booleanValue();
        String tcodeNameList = parameters.getParameter(PARAM_TCODENAME_LIST,null);
        boolean tcodeInclusive = Boolean.valueOf(parameters.getParameter(PARAM_TCODE_INCLUSIVE,"true")).booleanValue();
        String htypeNameList = parameters.getParameter(PARAM_HTYPENAME_LIST,null);
        boolean htypeInclusive = Boolean.valueOf(parameters.getParameter(PARAM_HTYPE_INCLUSIVE,"true")).booleanValue();
        String operator = parameters.getParameter(PARAM_OPERATOR);
        
        Date wrksWorkDate = wbData.getRuleData().getWorkSummary().getWrksWorkDate();
        Date yesterdayWrksWorkDate = DateHelper.addDays(wrksWorkDate,-1);
        int actualRestMinutes;

        // **** check if worked enough yesterday
        if (yesterdayMustWorkMinutes > 0) {
            int yesterdayMins = 0;
            if (!workMinutesConsecutive) {
                yesterdayMins = wbData.getMinutesWorkDetailPremiumRange(
                    yesterdayWrksWorkDate,yesterdayWrksWorkDate,null,null,
                    tcodeNameList,tcodeInclusive,htypeNameList,htypeInclusive,"D");
            } else {
                yesterdayMins = wbData.getMinutesWorkDetailRangeConsecutive(
                    yesterdayWrksWorkDate,yesterdayWrksWorkDate,null,null,
                    tcodeNameList,tcodeInclusive,htypeNameList,htypeInclusive);
            }
            if (yesterdayMins < yesterdayMustWorkMinutes) {
                if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) { logger.debug("yesterdayMustWorkMinutes : " + yesterdayMustWorkMinutes + " , yesterday Actual Minutes " +  yesterdayMins + " , no need to execute the condition" );}
                return false;
            }
        }

        // **** check if rested enough between yesterday max end time and today min start time
        actualRestMinutes = wbData.getMinutesDiffYesterdayMaxTodayMin
              (tcodeNameList,tcodeInclusive,htypeNameList,htypeInclusive);
        if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG))  {logger.debug("evaluating actualRestMinutes=" + actualRestMinutes + " (" + operator + ") " + "minRestMinutes=" + minRestMinutes); }
        return RuleHelper.evaluate(new Integer(actualRestMinutes), 
        		new Integer(minRestMinutes), operator) ;
    }

    public List getParameterInfo( DBConnection conn )
    {
        List result = new ArrayList();
        result.add(new RuleParameterInfo(PARAM_REST_MINUTES, RuleParameterInfo.INT_TYPE, false));
        RuleParameterInfo rpi = new RuleParameterInfo(PARAM_OPERATOR, RuleParameterInfo.CHOICE_TYPE, false);
        rpi.addChoice(RuleHelper.EQ);
        rpi.addChoice(RuleHelper.LESS);
        rpi.addChoice(RuleHelper.BIGGER);
        rpi.addChoice(RuleHelper.LESSEQ);
        rpi.addChoice(RuleHelper.BIGGEREQ);
        rpi.addChoice(RuleHelper.NOTEQ1);
        result.add(rpi);        
        result.add(new RuleParameterInfo(PARAM_YESTERDAY_MUST_WORK_MINUTES, RuleParameterInfo.INT_TYPE));
        result.add(new RuleParameterInfo(PARAM_WORK_MINUTES_CONSECUTIVE, RuleParameterInfo.STRING_TYPE));
        result.add(new RuleParameterInfo(PARAM_TCODENAME_LIST, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_TCODE_INCLUSIVE, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_HTYPENAME_LIST, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_HTYPE_INCLUSIVE, RuleParameterInfo.STRING_TYPE, true));
        return result;
    }

    public String getComponentName()
    {
        return "WBIAG: Has Sufficient Rest Condition";
    }

 }
