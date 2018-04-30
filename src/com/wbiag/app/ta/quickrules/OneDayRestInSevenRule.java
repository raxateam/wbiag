/*
 * Created on Mar 16, 2006
 *
 * State Pay Rules Project
 * One Day Rest In Seven Rule
 * Description:  Employee should be schedule for one day's rest every seven days.
 *
 */
package com.wbiag.app.ta.quickrules;

import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;
import com.workbrain.server.registry.Registry;


import java.util.*;

import javax.naming.*;

/**
 *  Title:        One Day Rest In Seven Rule
 *  Description:  Employee should be schedule for one day's rest every seven days.
 *  Copyright:    Copyright (c) 2006
 *  Company:      Workbrain Inc
 *  TT: 1074
 *
 *@deprecated As of 5.0.2.0, use core classes 
 *@author     Manisha Luthra
 *@version    1.0
 */
public class OneDayRestInSevenRule extends Rule{

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(OneDayRestInSevenRule.class);

	/*Constants*/
	public final static String DEFAULT_LOOK_BACK_DAYS = "7";
	public final static String DEFAULT_BREAK_DURATION = "24";
	public final static String DEFAULT_DAYWEEKSTART = "MON";
	public final static String DEFAULT_SPLIT_TIME = "19000101 000000";

    /*Parameters for the Rule*/
	public final static String PARAM_LIMIT_TO_CALENDAR_WEEK = "LimitToCalendarWeek";
    public final static String PARAM_DAY_WEEK_START = "DayWeekStart";
    public final static String PARAM_DAYS_TO_LOOK_BACK = "DaysToLookBack";
    public final static String PARAM_MIN_WORKED_HOURS = "MinimumWorkedHours";
    public final static String PARAM_VALID_WORK_TIME_CODE = "ValidWorkTimeCode";
    public final static String PARAM_VALID_WORK_HOUR_TYPE = "ValidWorkHourType";
    public final static String PARAM_VALID_NONWORK_NONREST_TIME_CODE = "ValidNonWorkNonRestTimeCode";
    public final static String PARAM_VALID_NONWORK_NONREST_HOUR_TYPE = "ValidNonWorkNonRestHourType";
    public final static String PARAM_SPLIT_TIME = "SplitTime";
	public final static String PARAM_MIN_REST_PERIOD_DURATION = "MinimumRestPeriodDuration";
	public final static String PARAM_REST_PERIOD_START_TIME = "RestPeriodStartTime";
	public final static String PARAM_REST_PERIOD_END_TIME = "RestPeriodEndTime";



    public OneDayRestInSevenRule(){
    }

	public String getComponentName(){
		return "WBIAG: One Day Rest in Seven Rule";
	}

	/*Set up the parameter for the rule builder*/
    public List getParameterInfo(DBConnection conn)	{
        List result = new ArrayList();

		/*Get Registry Day_Week_Start setting, so that we can set DayWeekStart parameter to its defaullt value*/
		String defaultDayWeekStarts = null;
		try
		{
			defaultDayWeekStarts = (String) Registry.getVar("system/WORKBRAIN_PARAMETERS/DAY_WEEK_STARTS");
		}
		catch(NamingException e)
		{
			defaultDayWeekStarts = DEFAULT_DAYWEEKSTART;		//default DAYWEEKSSTART value if no registry value exist
			if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) logger.error("The registry parameter DAY_WEEK_START has not been initialized. A default value will be used for the time being.");
		}

		/*Limit to Calendar Week Setting*/
        RuleParameterInfo limitToCalendarChoice = new RuleParameterInfo(PARAM_LIMIT_TO_CALENDAR_WEEK, RuleParameterInfo.CHOICE_TYPE);
		limitToCalendarChoice.addChoice("True");
		limitToCalendarChoice.addChoice("False");
		limitToCalendarChoice.setDefaultValue("False");					//Default Value set to False
		result.add(limitToCalendarChoice);

		/*Day Week Start Parameter*/
		RuleParameterInfo dayWeekStartChoice = new RuleParameterInfo(PARAM_DAY_WEEK_START, RuleParameterInfo.CHOICE_TYPE);
        dayWeekStartChoice.addChoice("SUN");
        dayWeekStartChoice.addChoice("MON");
		dayWeekStartChoice.addChoice("TUE");
		dayWeekStartChoice.addChoice("WED");
		dayWeekStartChoice.addChoice("THU");
		dayWeekStartChoice.addChoice("FRI");
		dayWeekStartChoice.addChoice("SAT");
		dayWeekStartChoice.setDefaultValue(defaultDayWeekStarts);		//Default Value set to Registry Setting for DayWeeksStart
        result.add(dayWeekStartChoice);

		/*Days to Look Back Parameter*/
        RuleParameterInfo daysToLookBack = new RuleParameterInfo(PARAM_DAYS_TO_LOOK_BACK, RuleParameterInfo.INT_TYPE);
		daysToLookBack.setDefaultValue(DEFAULT_LOOK_BACK_DAYS);							//Default Value set to 7 (DEFAULT_LOOK_BACK_DAYS)
        result.add(daysToLookBack);

		/*Minimum Worked Hours Parameter*/
		result.add(new RuleParameterInfo(PARAM_MIN_WORKED_HOURS, RuleParameterInfo.INT_TYPE));

		/*Valid Work Time Code Parameters*/
        result.add(new RuleParameterInfo(PARAM_VALID_WORK_TIME_CODE, RuleParameterInfo.STRING_TYPE));

		/*Valid Work Hour Type Parameters*/
		result.add(new RuleParameterInfo(PARAM_VALID_WORK_HOUR_TYPE, RuleParameterInfo.STRING_TYPE));

		/*Valid Non-Work Non-Rest Time Code Parameters*/
        result.add(new RuleParameterInfo(PARAM_VALID_NONWORK_NONREST_TIME_CODE, RuleParameterInfo.STRING_TYPE));

		/*Valid Non-Work Non-Rest Hour Type Parameters*/
		result.add(new RuleParameterInfo(PARAM_VALID_NONWORK_NONREST_HOUR_TYPE, RuleParameterInfo.STRING_TYPE));

		/*Split Time Parameter*/
		RuleParameterInfo splitTimeParam = new RuleParameterInfo(PARAM_SPLIT_TIME, RuleParameterInfo.STRING_TYPE);
		splitTimeParam.setDefaultValue(DEFAULT_SPLIT_TIME);				//Default value set to 19000101 000000 (DEFAULT_SPLIT_TIME)
		result.add(splitTimeParam);

		/*Rest Period Duration Parameters*/
		RuleParameterInfo minRestPeriodDuration = new RuleParameterInfo(PARAM_MIN_REST_PERIOD_DURATION, RuleParameterInfo.INT_TYPE);
		minRestPeriodDuration.setDefaultValue(DEFAULT_BREAK_DURATION);				//Default value set to 24 (DEFAULT_BREAK_DURATION)
		result.add(minRestPeriodDuration);

		/*Rest Period Start Time Parameter*/
		result.add(new RuleParameterInfo(PARAM_REST_PERIOD_START_TIME, RuleParameterInfo.STRING_TYPE));

		/*Rest Peroid End Time Parameter*/
		result.add(new RuleParameterInfo(PARAM_REST_PERIOD_END_TIME, RuleParameterInfo.STRING_TYPE));

        return result;
    }

    public void execute(WBData wbData, Parameters parameters) throws java.lang.Exception{
		Date today = wbData.getRuleData().getWrksWorkDate();
		Date intervalStart = null;
		Date intervalEnd = null;
		int minutesWrked = 0;
		int longestRestPeriod = 0;
		WorkDetailList wdl = new WorkDetailList();

		/*Call to getParameters method to get parameters values and store them*/
		ParametersResolved pars = getParameters(parameters);

		if("True".equalsIgnoreCase(pars.limitToCalendarWeek))
		{
			intervalStart = (Date) DateHelper.getWeeksFirstDate(today,
                DateHelper.getCalendarDay(pars.dayWeekStart));
			intervalEnd = (Date) DateHelper.addDays(intervalStart, 6);
		}
		else
		{
			intervalStart = (Date) DateHelper.addDays(today, 1 - pars.daysToLookBack);
			intervalEnd = today;
		}

		wdl = getWorkDetails(wbData, intervalStart, intervalEnd, pars);

		if (logger.isDebugEnabled()) logger.debug("----------------- OneDayRestInSevenRule START ---------------------");
		if (logger.isDebugEnabled()) logger.debug(wdl.toString());
		if (logger.isDebugEnabled()) logger.debug("wdl size: "+wdl.size());

		minutesWrked = getMinutes(wdl, intervalStart, intervalEnd, pars);

		if (logger.isDebugEnabled()) logger.debug("minWrked:"+minutesWrked+" >= "+"minWorkedHours*60:"+pars.minWorkedHours*60);

		if(minutesWrked >= (pars.minWorkedHours*60))
		{
			longestRestPeriod = getMaxRestPeriod(wdl, intervalStart, intervalEnd, pars);

			if (logger.isDebugEnabled()) logger.debug("maxRestPeriod:"+longestRestPeriod+" < minRestPeriodDuration:"+ pars.minRestPeriodDuration*60);

			if(longestRestPeriod < pars.minRestPeriodDuration*60){
				throw new RuntimeException ("OneDayRestInSeven: Longest Rest Period Length is: " + (longestRestPeriod/60) +
											  " that is less than " + pars.minRestPeriodDuration);
			}
		}
		if (logger.isDebugEnabled()) logger.debug("----------------- OneDayRestInSevenRule END ---------------------");
    }

    // Helper Methods


	/*
	 * Retrieve the parameter settings from the rule builder
	 */
    public ParametersResolved getParameters(Parameters parameters){
        ParametersResolved ret = new ParametersResolved();
		ret.limitToCalendarWeek = parameters.getParameter(PARAM_LIMIT_TO_CALENDAR_WEEK);
		ret.dayWeekStart = parameters.getParameter(PARAM_DAY_WEEK_START);
		ret.daysToLookBack = parameters.getIntegerParameter(PARAM_DAYS_TO_LOOK_BACK);
		ret.minWorkedHours = parameters.getIntegerParameter(PARAM_MIN_WORKED_HOURS);
		ret.validWorkTimeCode = parameters.getParameter(PARAM_VALID_WORK_TIME_CODE);
		ret.validWorkHourType = parameters.getParameter(PARAM_VALID_WORK_HOUR_TYPE);
		ret.validNonWorkNonRestTimeCode = parameters.getParameter(PARAM_VALID_NONWORK_NONREST_TIME_CODE);
		ret.validNonWorkNonRestHourType = parameters.getParameter(PARAM_VALID_NONWORK_NONREST_HOUR_TYPE);
		ret.splitTime = parameters.getParameter(PARAM_SPLIT_TIME);
		ret.minRestPeriodDuration = parameters.getIntegerParameter(PARAM_MIN_REST_PERIOD_DURATION);
		ret.requiredRestPeriodStartTime = parameters.getParameter(PARAM_REST_PERIOD_START_TIME);
		ret.requiredRestPeriodEndTime = parameters.getParameter(PARAM_REST_PERIOD_END_TIME);

        return ret;
	}

    /*
     * Return the number of minutes in the list of work details that have validWorkTimeCode and validWorkHourType
     */
    private int getMinutes(WorkDetailList wdl, Date start, Date end,
                           ParametersResolved pars){
    	boolean checkPastMidnight = true;

    	Date intervalStart = getDate(start, pars.splitTime, !checkPastMidnight);
    	Date intervalEnd = getDate(end,pars.splitTime, checkPastMidnight);

    	if (logger.isDebugEnabled()) logger.debug("--- getMinutes");
    	if (logger.isDebugEnabled()) logger.debug("getWorkDetails -> start: "+intervalStart+" end: "+intervalEnd);

    	return wdl.getMinutes(intervalStart, intervalEnd, pars.validWorkTimeCode,
                              true, pars.validWorkHourType, true);
    }


    /*
     * Get all Workdetails between specified start date + splitTime and end date + splitTime
     */
    private WorkDetailList getWorkDetails(WBData wbData, Date start, Date end
                                          ,ParametersResolved pars) throws Exception{
    	WorkDetailList resultList = new WorkDetailList();
    	boolean splitDetail = true;
    	boolean checkPastMidnight = true;

    	if ((start == null) || (end == null)) return resultList;

    	// For the first date, there may be work details that are charged to 'yesterday' but overlap to today
    	Date curDate = DateHelper.addDays(start, -1);

    	Date intervalStart = getDate(start, pars.splitTime, !checkPastMidnight);
    	Date intervalEnd = getDate(end,pars.splitTime, checkPastMidnight);

    	resultList.setCodeMapper(wbData.getCodeMapper());

    	while (curDate.getTime() <= end.getTime()){
    		WorkDetailList curList = wbData.getWorkDetailsForDate(curDate);

    		curList.removeWorkDetails(null, intervalStart, splitDetail);
    		curList.removeWorkDetails(intervalEnd, null, splitDetail);

    		resultList.addAll(curList);

    		curDate = DateHelper.addDays(curDate, 1);
    	}

    	return resultList;
    }

    /*
     * Return a new date that combines the specified split time with the date portion (yyyy-MM-dd) of start
     */
    private Date getDate(Date start, String splitTime, boolean checkPastMidnight){
    	Date result = start;

    	//String dateSegment = DateHelper.convertDateString(start.toString(), "yyyy-MM-dd HH:mm:ss.S", "yyyy-MM-dd");
    	String dateSegment = DateHelper.convertDateString(start, "yyyy-MM-dd");
    	//String timeSegment = DateHelper.convertDateString(splitTime, "yyyyMMdd HHmmss", "HH:mm");
    	String timeSegment = DateHelper.convertDateString(splitTime, "yyyyMMdd HHmmss", "HH:mm");
    	String newDate = dateSegment + " " + timeSegment;

    	result = DateHelper.parseDate(newDate, "yyyy-MM-dd HH:mm");

    	if (checkPastMidnight && (result.compareTo(start) <=0)){
    		result = DateHelper.addDays(result, 1);
    	}

    	return result;
    }

    /*
     * Return the maximum rest period for the specified list of work details between start and end
     */
    private int getMaxRestPeriod(WorkDetailList wdl, Date start,
                                 Date end,
                                 ParametersResolved pars){
    	int maxRest = 0;
    	int curMaxRest = 0;
       	boolean checkPastMidnight = true;
    	Date intervalStart = getDate(start, pars.splitTime, !checkPastMidnight);
    	Date intervalEnd = getDate(end,pars.splitTime, checkPastMidnight);
    	WorkDetailData firstWD = new WorkDetailData();
    	String tcodeList = pars.validWorkTimeCode;
    	String htypeList = pars.validWorkHourType;

    	if (!StringHelper.isEmpty(pars.validNonWorkNonRestTimeCode))
    		tcodeList = tcodeList + "," + pars.validNonWorkNonRestTimeCode;

    	if (!StringHelper.isEmpty(pars.validNonWorkNonRestHourType))
    		htypeList = htypeList + "," + pars.validNonWorkNonRestHourType;

    	if (logger.isDebugEnabled()) logger.debug("--- getMaxRestPeriod");
    	if (logger.isDebugEnabled()) logger.debug("wdl.size():"+wdl.size());

    	while (maxRest < pars.minRestPeriodDuration*60){

        	if (logger.isDebugEnabled()) logger.debug("maxRest:"+maxRest+" < minRestPeriodDuration:"+pars.minRestPeriodDuration*60);

        	int firstWDindex = wdl.getFirstRecordIndex(intervalStart,
  				  null,
  				  tcodeList,
  				  true,
  				  htypeList,
  				  true,
				  true);

        	if (logger.isDebugEnabled()) logger.debug("firstWDindex:"+firstWDindex);

        	if (firstWDindex >= 0){
        		firstWD = wdl.getWorkDetail(firstWDindex);

        		if (logger.isDebugEnabled()) logger.debug("intervalStart:"+intervalStart+" firstWD.getWrkdStartTime():"+firstWD.getWrkdStartTime());

        		if (!intervalStart.equals(firstWD.getWrkdStartTime())){
        			curMaxRest = (int) DateHelper.getMinutesBetween(firstWD.getWrkdStartTime(), intervalStart);
        			maxRest = validateRestPeriod(curMaxRest,
                                                 maxRest,
                                                 intervalStart,
                                                 firstWD.getWrkdStartTime(),
                                                 pars);
        			if (logger.isDebugEnabled()) logger.debug("curMaxRest:"+curMaxRest+" maxRest:"+maxRest);
        		}

        		intervalStart = firstWD.getWrkdEndTime();
        	}

        	// last WD, compare end time with end of interval if maxRest is still < minRestPeriodDuration*60
        	if (((firstWDindex == -1)) || ((firstWDindex == wdl.size()-1))
                && (maxRest < pars.minRestPeriodDuration*60)){
        		curMaxRest = (int) DateHelper.getMinutesBetween(intervalEnd, intervalStart);
        		maxRest = validateRestPeriod(curMaxRest, maxRest,
                                             intervalStart, intervalEnd,pars);
        		if (logger.isDebugEnabled()) logger.debug("last curMaxRest:"+curMaxRest+" maxRest:"+maxRest);
        		break;
        	}
    	}

    	return maxRest;
    }

	/*
	 * If applicable, check to see if the rest period falls within the specified rest period
	 * If so, return max(cur, max);
	 */
	private int validateRestPeriod(int cur, int max, Date breakStart,
                                   Date breakEnd,
                                   ParametersResolved pars){
		int result = max;

		//Check if rest period takes place during desired times
		if(!StringHelper.isEmpty(pars.requiredRestPeriodStartTime)
           && !StringHelper.isEmpty(pars.requiredRestPeriodEndTime)){
			Date requiredRestPeriodStart = DateHelper.setTimeValues(breakStart,
                (DateHelper.convertStringToDate(pars.requiredRestPeriodStartTime, "yyyyMMdd HHmmss")));

			if (requiredRestPeriodStart.before(breakStart)) {
                requiredRestPeriodStart = DateHelper.addDays(
                    requiredRestPeriodStart, 1);
            }

			Date requiredRestPeriodEnd = DateHelper.setTimeValues(
                         requiredRestPeriodStart,
                         (DateHelper.convertStringToDate(pars.requiredRestPeriodEndTime, "yyyyMMdd HHmmss")));

			if (requiredRestPeriodEnd.before(requiredRestPeriodStart)) {
                requiredRestPeriodEnd = DateHelper.addDays(
                    requiredRestPeriodEnd, 1);
            }

	    	if (logger.isDebugEnabled()) logger.debug("--- validateRestPeriod");
			if (logger.isDebugEnabled()) logger.debug("requiredRestPeriodStart:"+requiredRestPeriodStart+ "restPeriodEnd:"+requiredRestPeriodEnd);
			if (logger.isDebugEnabled()) logger.debug("breakStart:"+breakStart+ "breakEnd:"+breakEnd);

			if(((breakStart.compareTo(requiredRestPeriodStart) == 0)
                || (breakStart.compareTo(requiredRestPeriodStart) < 0)) &&
			   ((breakEnd.compareTo(requiredRestPeriodEnd) == 0)
                || (breakEnd.compareTo(requiredRestPeriodEnd) > 0))
               && (cur > max)){
				result = cur;
			}
		} else if (cur > max){
			result = cur;
		}

		return result;

	}

    class ParametersResolved {
        /*Rule Parameter Variables*/
        String limitToCalendarWeek;
        String dayWeekStart;
        int daysToLookBack;
        int minWorkedHours;
        String validWorkTimeCode;
        String validWorkHourType;
        String validNonWorkNonRestTimeCode;
        String validNonWorkNonRestHourType;
        String splitTime;
        int minRestPeriodDuration;
        String requiredRestPeriodStartTime = null;
        String requiredRestPeriodEndTime = null;
    }
}

