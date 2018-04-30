package com.wbiag.app.ta.conditions;

import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.DBConnection;
import com.workbrain.util.*;

import java.util.*;

/**
 *  Title:          WorkedNMinutesBeforeAfterCondition
 *  Description:    A condition to check the number of minutes worked before or
 *                  after a given time of day.
 *  Copyright:      Copyright (c) 2003
 *  Company:        Workbrain Inc
 *
 *  @author         Kevin Tsoi
 *  @version        1.0
 */

public class WorkedNMinutesBeforeAfterCondition extends Condition
{
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(WorkedNMinutesBeforeAfterCondition.class);

    public final static String PARAM_WORK_MINUTES = "WorkMinutes";
    public final static String PARAM_OPERATOR = "Operator";
    public final static String PARAM_TCODE_NAME_LIST = "TcodeNameList";
    public final static String PARAM_TCODE_INCLUSIVE = "TcodeInclusive";
    public final static String PARAM_HTYPE_NAME_LIST = "HtypeNameList";
    public final static String PARAM_HTYPE_INCLUSIVE = "HtypeInclusive";
    public final static String PARAM_BEFORE_TIME = "BeforeTime";
    public final static String PARAM_VAL_SCHEDULE_START = "SCHEDULE_START";
    public final static String PARAM_VAL_SCHEDULE_END = "SCHEDULE_END";
    public final static String PARAM_AFTER_TIME = "AfterTime";
    public final static String PARAM_BEFORE_DAY = "BeforeDay";
    public final static String PARAM_AFTER_DAY = "AfterDay";

    public boolean evaluate(WBData wbData, Parameters parameters)
        throws Exception
    {
        int minutes;
        Calendar beforeTimeCal;
        Calendar afterTimeCal;
        Date beforeTime = null;
        Date afterTime = null;
        Date beforeDay = wbData.getWrksWorkDate() ;
        Date afterDay = wbData.getWrksWorkDate();

        int requiredMinutes = parameters.getIntegerParameter(PARAM_WORK_MINUTES);
        String operator = parameters.getParameter(PARAM_OPERATOR);
        String tcodeNameList = parameters.getParameter(PARAM_TCODE_NAME_LIST,null);
        boolean tcodeInclusive = Boolean.valueOf(parameters.getParameter(PARAM_TCODE_INCLUSIVE, Boolean.TRUE.toString() )).booleanValue();
        String htypeNameList = parameters.getParameter(PARAM_HTYPE_NAME_LIST,null);
        boolean htypeInclusive = Boolean.valueOf(parameters.getParameter(PARAM_HTYPE_INCLUSIVE, Boolean.TRUE.toString())).booleanValue();

        String beforeTimeStr = parameters.getParameter(PARAM_BEFORE_TIME);
        String afterTimeStr = parameters.getParameter(PARAM_AFTER_TIME);
        String beforeDayStr = parameters.getParameter(PARAM_BEFORE_DAY);
        String afterDayStr = parameters.getParameter(PARAM_AFTER_DAY);

        if (!StringHelper.isEmpty(beforeTimeStr)) {
            if (PARAM_VAL_SCHEDULE_START.equals(beforeTimeStr)) {
                beforeTime = wbData.getEmployeeScheduleData().getEmpskdActStartTime();
            }
            else if (PARAM_VAL_SCHEDULE_END.equals(beforeTimeStr)) {
                beforeTime = wbData.getEmployeeScheduleData().getEmpskdActEndTime();
            }
            else {
                //set before time on wrksWorkDate
                beforeTimeCal = DateHelper.toCalendar(wbData.getWrksWorkDate());
                if (setTime(beforeTimeStr, beforeTimeCal)) {
                    beforeTime = beforeTimeCal.getTime();
                }
            }
        }

        if (!StringHelper.isEmpty(afterTimeStr)) {
            if (PARAM_VAL_SCHEDULE_START.equals(beforeTimeStr)) {
                afterTime = wbData.getEmployeeScheduleData().getEmpskdActStartTime();
            }
            else if (PARAM_VAL_SCHEDULE_END.equals(beforeTimeStr)) {
                afterTime = wbData.getEmployeeScheduleData().getEmpskdActEndTime();
            }
            else {
                //set after time on wrksWorkDate
                afterTimeCal = DateHelper.toCalendar(wbData.getWrksWorkDate());
                if (setTime(afterTimeStr, afterTimeCal)) {
                    afterTime = afterTimeCal.getTime();
                }
            }
        }
        // *** if beforetime prior, it means the next day
        if(beforeTime != null && afterTime != null) {
            if (beforeTime.before(afterTime)) {
                beforeTime = DateHelper.addDays(beforeTime , 1);
            }
        }

        if (!StringHelper.isEmpty(beforeDayStr) && !StringHelper.isEmpty(afterDayStr)) {
            beforeDay = DateHelper.nextDay(
                DateHelper.addDays(wbData.getWrksWorkDate() , -7) , beforeDayStr);
            afterDay = DateHelper.nextDay(
                DateHelper.addDays(wbData.getWrksWorkDate() , -7) , afterDayStr);
            // *** if beforeDay prior, it means the next week
            if (beforeDay.before(afterDay)) {
                beforeDay = DateHelper.addDays(beforeDay , 7);
            }
            if (logger.isDebugEnabled()) logger.debug("afterDay : " + afterDay + "-beforeDay : " + beforeDay);
            if(beforeTime != null && afterTime != null) {
                beforeTime = DateHelper.setTimeValues(beforeDay , beforeTime);
                afterTime = DateHelper.setTimeValues(afterDay , afterTime);
            }
        }
        if (logger.isDebugEnabled()) logger.debug("afterTime : " + afterTime + "-beforeTime : " + beforeTime);

        minutes = wbData.getMinutesWorkDetailRange(afterDay , beforeDay ,
            afterTime, beforeTime, tcodeNameList, tcodeInclusive,
            htypeNameList, htypeInclusive);
        if (logger.isDebugEnabled()) logger.debug("Minutes total : " + minutes);
        return RuleHelper.evaluate(new Integer(minutes),
                                   new Integer(requiredMinutes), operator);
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
        result.add(new RuleParameterInfo(PARAM_AFTER_TIME, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_BEFORE_TIME, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_AFTER_DAY, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_BEFORE_DAY, RuleParameterInfo.STRING_TYPE, true));
        return result;
    }

    public String getComponentName()
    {
        return "WBIAG: Worked N Minutes Before After Condition";
    }

    private boolean setTime(String timeStr, Calendar time)
    {
        int hour;
        int minutes;
        try
        {
            hour = Integer.parseInt(timeStr.substring(0,2));
            minutes = Integer.parseInt(timeStr.substring(3,5));

            //validates timeStr
            if(hour < 0 || hour >=24 || minutes < 0 || minutes >= 60)
            {
                return false;
            }

            //sets time for calendar time object
            time.set(Calendar.HOUR_OF_DAY, hour);
            time.set(Calendar.MINUTE, minutes);
        }
        catch(Exception e)
        {
            throw new RuntimeException ("Could not parse time string : " +  timeStr);
        }
        return true;
    }
}
