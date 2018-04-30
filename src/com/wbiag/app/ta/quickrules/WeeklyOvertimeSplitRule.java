package com.wbiag.app.ta.quickrules;

import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.DBConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Calendar;
import java.util.GregorianCalendar;


import com.workbrain.app.ta.rules.GenericOvertimeRule;
import com.workbrain.util.DateHelper;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * WeeklyOvertimeSplitRule rule for split week solution
 *
 *@deprecated As of 5.0.2.0, use core classes 
 */
public class WeeklyOvertimeSplitRule extends GenericOvertimeExtRule {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(WeeklyOvertimeSplitRule.class);

    public final static String PARAM_HOURSET_DESCRIPTION = "HourSetDescription";
    public final static String PARAM_WORKDETAIL_TIMECODES = "WorkDetailTimeCodes";
    public final static String PARAM_ELIGIBLE_HOURTYPES = "EligibleHourTypes";
    public final static String PARAM_PREMIUM_TIMECODES_COUNTED = "PremiumTimeCodesCounted";
    public final static String PARAM_DISCOUNT_TIMECODES = "DiscountTimeCodes";
    public final static String PARAM_DISCOUNT_HOURTYPES = "DiscountHourTypes";
    public final static String PARAM_DAY_WEEK_STARTS = "DayWeekStarts";
    public final static String PARAM_PREMIUM_TIMECODE_INSERTED = "PremiumTimeCodeInserted";
    public final static String PARAM_ASSIGN_BETTERRATE = "AssignBetterRate";
    public final static String PARAM_HOURTYPE_FOR_OVERTIME_WORKDETAILS = "HourTypeForOvertimeWorkDetails";
    public final static String PARAM_P24_HOUR_STARTTIME = "p24HourStartTime";
    public final static String PARAM_STARTTIME_DAY = "StartTimeDay";

    public final static String PARAM_VALUE_PREVIOUS_DAY = "PREVIOUS DAY";
    public final static String PARAM_VALUE_DAY_WEEK_STARTS = "DAY WEEK STARTS";

    public final String P24HOUR_DATE_FORMAT = "yyyyMMdd HHmmss";
    public final String P24HOUR_DEFAULT = "19000101 000000";

    // Parameters



    public String getComponentName() {
        return "WBIAG: Weekly Overtime Split Rule";
    }


    public List getParameterInfo(DBConnection conn) {

    	List result = new ArrayList();
    	RuleParameterInfo params = null;

        result.add(new RuleParameterInfo(PARAM_HOURSET_DESCRIPTION, RuleParameterInfo.STRING_TYPE, false));
        result.add(new RuleParameterInfo(PARAM_WORKDETAIL_TIMECODES, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_ELIGIBLE_HOURTYPES, RuleParameterInfo.STRING_TYPE, false));
        result.add(new RuleParameterInfo(PARAM_PREMIUM_TIMECODES_COUNTED, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_DISCOUNT_TIMECODES, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_DISCOUNT_HOURTYPES, RuleParameterInfo.STRING_TYPE, true));

        params = new RuleParameterInfo(PARAM_DAY_WEEK_STARTS, RuleParameterInfo.CHOICE_TYPE, false);
        params.addChoice("Sunday");
        params.addChoice("Monday");
        params.addChoice("Tuesday");
        params.addChoice("Wednesday");
        params.addChoice("Thursday");
        params.addChoice("Friday");
        params.addChoice("Saturday");
        result.add(params);

        result.add(new RuleParameterInfo(PARAM_PREMIUM_TIMECODE_INSERTED, RuleParameterInfo.STRING_TYPE, true));

        RuleParameterInfo rpiAssignBetterRateChoice = new RuleParameterInfo(PARAM_ASSIGN_BETTERRATE, RuleParameterInfo.CHOICE_TYPE, true);
        rpiAssignBetterRateChoice.addChoice("true");
        rpiAssignBetterRateChoice.addChoice("false");
        result.add(rpiAssignBetterRateChoice);

        result.add(new RuleParameterInfo(PARAM_HOURTYPE_FOR_OVERTIME_WORKDETAILS, RuleParameterInfo.STRING_TYPE, true));

        result.add(new RuleParameterInfo(PARAM_P24_HOUR_STARTTIME, RuleParameterInfo.STRING_TYPE, false));

        params = new RuleParameterInfo(PARAM_STARTTIME_DAY, RuleParameterInfo.CHOICE_TYPE, true);
        params.addChoice(PARAM_VALUE_PREVIOUS_DAY);
        params.addChoice(PARAM_VALUE_DAY_WEEK_STARTS);
        result.add(params);

        return result;
    }

    /**
     * Execute the rule.
     *
     */
    public void execute(WBData wbData, Parameters parameters) throws java.lang.Exception {

    	// Retrieve parameters
    	ParametersResolved pars = getParameters(wbData, parameters);

        if (pars.startTimeDay.equals(PARAM_VALUE_DAY_WEEK_STARTS)) {

        	calcFromDayWeekStarts(wbData , pars);

        } else if (pars.startTimeDay.equals(PARAM_VALUE_PREVIOUS_DAY)) {

        	calcFromDayWeekStartsPrevDay(wbData, pars);

        } else {
        	throw new IllegalArgumentException("Unknown value for parameter " + PARAM_STARTTIME_DAY);
        }
    }


    /**
     * Calculates weekly OT where the 24hr start time falls on the day before Day Week Starts.
     *
     * Eg. if day week starts is SUNDAY, and 2hr start time is 06:00, then the range is
     * Saturday 06:00 to Saturday 06:00.
     *
     * @param wbData
     * @throws Exception
     */
    protected void calcFromDayWeekStartsPrevDay(WBData wbData, ParametersResolved pars) throws Exception {

        int seedMinutes = 0;
        DateFormat dateFormat = new SimpleDateFormat(WBData.DATE_FORMAT_STRING);

        Date workSummaryDate = wbData.getRuleData().getWorkSummary().getWrksWorkDate();


        Date dateWeekStarts = DateHelper.nextDay(DateHelper.addDays(workSummaryDate, -7),
                                                 pars.dayWeekStarts);
        Date dateWeekEnds = DateHelper.addDays(dateWeekStarts , 6);
        boolean isLastDayOfWeek = workSummaryDate.compareTo(dateWeekEnds) == 0;
        boolean isFirstDayOfWeek = workSummaryDate.compareTo(dateWeekStarts) == 0;

        Date p24Time = DateHelper.parseDate(pars.p24HourStartTime , P24HOUR_DATE_FORMAT);
        Date daySplitStart = DateHelper.setTimeValues(workSummaryDate , p24Time);
        Date daySplitEnd = DateHelper.addDays(daySplitStart , 1) ;


        // Start time is on the day previous to the day week starts.
        Date weekStartTime = DateHelper.setTimeValues(DateHelper.addDays(dateWeekStarts, -1), p24Time);
        Date weekEndTime = DateHelper.setTimeValues(DateHelper.addDays(dateWeekEnds, 1), p24Time);
        // *** tt758 consider Emp Day start time
        long noonInMillisSinceMidnight = DateHelper.getDateSetToNoon().getTime()
            - DateHelper.truncateToDays(DateHelper.getDateSetToNoon()).getTime();
        Date dayStartTime = RuleHelper.getDayStartTime(
            wbData.getRuleData().getEmployeeData() , wbData.getDBconnection());
        if (DateHelper.getDayFraction(dayStartTime) > noonInMillisSinceMidnight ) {
            //weekStartTime = DateHelper.addDays(weekStartTime , -1);
            weekEndTime = DateHelper.addDays(weekEndTime , -1);
            daySplitStart = DateHelper.addDays(daySplitStart , -1);
            daySplitEnd= DateHelper.addDays(daySplitEnd , -1);
        }

        if (logger.isDebugEnabled()) logger.debug("dateWeekStarts:" + dateWeekStarts + "-dateWeekEnds" + dateWeekEnds);
        if (logger.isDebugEnabled()) logger.debug("weekStartTime:" + weekStartTime + "-weekEndTime" + weekEndTime);
        if (logger.isDebugEnabled()) logger.debug("daySplitStart:" + daySplitStart + "-daySplitEnd" + daySplitEnd);

        wbData.getRuleData().getWorkDetails().splitAt(daySplitStart);
        wbData.getRuleData().getWorkDetails().splitAt(daySplitEnd);

        // Need to look back one extra day in case there are work detail that cross midnight.
        dateWeekStarts = DateHelper.addDays(dateWeekStarts, -1);

        if (isFirstDayOfWeek) {

            Date startDatePrevWeek = DateHelper.addDays(dateWeekStarts, -7);
            Date startTimePrevWeek = DateHelper.addDays(weekStartTime, -7);
            Date endDatePrevWeek = DateHelper.addDays(dateWeekEnds, -7);
            Date endTimePrevWeek = DateHelper.addDays(weekEndTime, -7);

            if (logger.isDebugEnabled()) logger.debug("startDatePrevWeek:" + startDatePrevWeek + "-endDatePrevWeek" + endDatePrevWeek);
            if (logger.isDebugEnabled()) logger.debug("startTimePrevWeek:" + startTimePrevWeek + "-endTimePrevWeek" + endTimePrevWeek);

            // Calc from the previous week, up the the 24hr start time on the first day of current week.
            calcRange(wbData, startDatePrevWeek, startTimePrevWeek, endDatePrevWeek, endTimePrevWeek, null, endTimePrevWeek, pars);

            // Calc from the 24hr start time on the first day of the week.
            calcRange(wbData, dateWeekStarts, weekStartTime,
                      dateWeekEnds, weekEndTime, wbData.getMinStartTime(), null, pars);
        }
        else if (isLastDayOfWeek) {
        	calcRange(wbData, dateWeekStarts, weekStartTime,
                      dateWeekEnds, weekEndTime, null, weekEndTime, pars);

        } else {
        	calcRange(wbData, dateWeekStarts, weekStartTime,
                      dateWeekEnds, weekEndTime, null, null, pars);
    	}
    }


    /**
     * Calculates weekly OT where the 24hr start time falls on the Day Week Starts.
     *
     * Eg. if day week starts is SUNDAY, and 2hr start time is 06:00, then the range is
     * Sunday 06:00 to Sunday 06:00.
     *
     * Need to calcute in 3 scenarios.
     *
     * 1) if first day of week
     * 		a) calc up to the 24hr start time.
     * 		b) calc from 24hr start time on first day of week to the remainder of the day.
     *
     * 2) if the last day of the week
     * 		a) calc from 24hr start time on first day of week to 24hr start time on the day
     * 			after the last day of the week.
     *
     * 3) if a midweek day
     * 		a) calc from 24hr start time on first day of week to the remainder of the day.
     *
     * @param wbData
     * @throws Exception
     */
    protected void calcFromDayWeekStarts(WBData wbData, ParametersResolved pars) throws Exception {

        Date workSummaryDate = wbData.getRuleData().getWorkSummary().getWrksWorkDate();
        Date dateWeekStarts = DateHelper.nextDay(DateHelper.addDays(workSummaryDate, -7),
                                                 pars.dayWeekStarts);
        Date dateWeekEnds = DateHelper.addDays(dateWeekStarts , 6);
        boolean isFirstDayOfWeek = workSummaryDate.compareTo(dateWeekStarts) == 0;
        boolean isLastDayOfWeek = workSummaryDate.compareTo(dateWeekEnds) == 0;

        Date p24Time = DateHelper.parseDate(pars.p24HourStartTime , P24HOUR_DATE_FORMAT);

        Date daySplitStart = DateHelper.setTimeValues(workSummaryDate , p24Time);
        Date daySplitEnd = DateHelper.addDays(daySplitStart , 1) ;
        Date weekStartTime = DateHelper.setTimeValues(dateWeekStarts, p24Time);
        Date weekEndTime = DateHelper.setTimeValues(DateHelper.addDays(dateWeekEnds, 1), p24Time);

        wbData.getRuleData().getWorkDetails().splitAt(daySplitStart);
        wbData.getRuleData().getWorkDetails().splitAt(daySplitEnd);

        // Need to look back one extra day in case there are work detail that cross midnight.
        dateWeekStarts = DateHelper.addDays(dateWeekStarts, -1);

        if (isFirstDayOfWeek) {

        	Date startDatePrevWeek = DateHelper.addDays(dateWeekStarts, -7);
        	Date startTimePrevWeek = DateHelper.addDays(weekStartTime, -7);
        	Date endDatePrevWeek = DateHelper.addDays(dateWeekEnds, -7);
        	Date endTimePrevWeek = DateHelper.addDays(weekEndTime, -7);

	        // Calc from the previous week, up the the 24hr start time on the first day of current week.
        	calcRange(wbData, startDatePrevWeek, startTimePrevWeek,
                      endDatePrevWeek, endTimePrevWeek, null, endTimePrevWeek, pars);

	        // Calc from the 24hr start time on the first day of the week.
        	calcRange(wbData, dateWeekStarts, weekStartTime,
                      dateWeekEnds, weekEndTime, wbData.getMinStartTime(), null, pars);

	    // Last day of the week.
        } else if (isLastDayOfWeek){

	        // Calc up to the 24hr start time on the day after the last day of the week.
        	calcRange(wbData, dateWeekStarts, weekStartTime, dateWeekEnds,
                      weekEndTime, null, weekEndTime, pars);

        // Middle of the week.
        } else {
        	// No restriction on the work details within the day.
        	calcRange(wbData, dateWeekStarts, weekStartTime, dateWeekEnds,
                      weekEndTime, null, null, pars);
        }
    }

    /**
     * Performs the calc for a given week interval, and range within a day.
     *
     * @param wbData
     * @param weekStartDate
     * @param weekStartTime
     * @param weekEndTime
     * @param startTimeInShift
     * @param endTimeInShift
     * @throws Exception
     */
    protected void calcRange(WBData wbData,
    							Date weekStartDate, Date weekStartTime,
    							Date weekEndDate, Date weekEndTime,
								Date startTimeInShift, Date endTimeInShift,
                                ParametersResolved pars) throws Exception {

    	int seedMinutes = 0;
        Date workSummaryDate = wbData.getRuleData().getWorkSummary().getWrksWorkDate();

        Parameters parametersForGenericOvertimeRule = new Parameters();
        DateFormat dateFormat = new SimpleDateFormat(WBData.DATE_FORMAT_STRING);

        // Include the work premium minutes if premiumTimeCodesCounted is provided
        if (pars.premiumTimeCodesCounted != null) {
            seedMinutes += wbData.getMinutesWorkDetailPremiumRange(weekStartDate,
					                weekEndDate,
					                null, null,
					                pars.premiumTimeCodesCounted, true,
					                pars.eligibleHourTypes, true, "P" , false);
        }

        if (pars.discountTimeCodes != null) {

        	// This method is deprecated but is still used by the core WeeklyOvertimeRule.  Not
        	// comfortable changing this until core makes the change as well.
            seedMinutes += wbData.getWorkedMinutes(
					                wbData.getRuleData().getWorkSummary().getWrksId(),
					                weekStartDate,
					                weekEndDate,
					                weekStartTime ,
					                weekEndTime , true, pars.discountTimeCodes, pars.discountHourTypes);
        }

        parametersForGenericOvertimeRule.addParameter(super.PARAM_ADDITIONAL_MINUTES_WORKED, Integer.toString(seedMinutes));

        parametersForGenericOvertimeRule.addParameter(super.PARAM_INTERVAL_STARTDATE, dateFormat.format(weekStartDate));
        parametersForGenericOvertimeRule.addParameter(super.PARAM_INTERVAL_STARTTIME, dateFormat.format(weekStartTime));
        parametersForGenericOvertimeRule.addParameter(super.PARAM_INTERVAL_ENDDATE, dateFormat.format(workSummaryDate));
        parametersForGenericOvertimeRule.addParameter(super.PARAM_INTERVAL_ENDTIME, null);
        parametersForGenericOvertimeRule.addParameter(super.PARAM_STARTTIME_WITHIN_SHIFT, startTimeInShift == null ? null : dateFormat.format(startTimeInShift));
        parametersForGenericOvertimeRule.addParameter(super.PARAM_ENDTIME_WITHIN_SHIFT, endTimeInShift == null ? null : dateFormat.format(endTimeInShift));
        parametersForGenericOvertimeRule.addParameter(super.PARAM_SKDZONE_STARTDATE, dateFormat.format(weekStartDate));
        parametersForGenericOvertimeRule.addParameter(super.PARAM_SKDZONE_ENDDATE, dateFormat.format(DateHelper.addDays(weekStartDate , 6)));

        parametersForGenericOvertimeRule.addParameter(super.PARAM_HOURSET_DESCRIPTION, pars.hourSetDescription);
        parametersForGenericOvertimeRule.addParameter(super.PARAM_ELIGIBLE_TIMECODES, pars.workDetailTimeCodes);
        parametersForGenericOvertimeRule.addParameter(super.PARAM_ARE_TIMECODES_INCLUSIVE, "true");
        parametersForGenericOvertimeRule.addParameter(super.PARAM_ELIGIBLE_HOURTYPES, pars.eligibleHourTypes);
        parametersForGenericOvertimeRule.addParameter(super.PARAM_ADD_PREMIUMRECORD, "" + (pars.premiumTimeCodeInserted != null));
        parametersForGenericOvertimeRule.addParameter(super.PARAM_PREMIUM_TIMECODE, pars.premiumTimeCodeInserted);
        parametersForGenericOvertimeRule.addParameter(super.PARAM_HOURTYPE_FOR_OVERTIME_WORKDETAILS, pars.hourTypeForOvertimeWorkDetails);
        parametersForGenericOvertimeRule.addParameter(super.PARAM_ASSIGN_BETTERRATE, String.valueOf(pars.assignBetterRate) );        // Execute the core rule

        super.execute(wbData, parametersForGenericOvertimeRule);
    }

    /**
     * Get the input parameters.
     *
     * @param wbData
     * @param parameters
     */
    protected ParametersResolved getParameters(WBData wbData, Parameters parameters) {
        ParametersResolved ret = new ParametersResolved();
        ret.hourSetDescription = parameters.getParameter(PARAM_HOURSET_DESCRIPTION);
        ret.workDetailTimeCodes = parameters.getParameter(PARAM_WORKDETAIL_TIMECODES);
        ret.eligibleHourTypes = parameters.getParameter(PARAM_ELIGIBLE_HOURTYPES,"REG");
        ret.premiumTimeCodesCounted = parameters.getParameter(PARAM_PREMIUM_TIMECODES_COUNTED, null);
        ret.discountTimeCodes = parameters.getParameter(PARAM_DISCOUNT_TIMECODES, null);
        ret.discountHourTypes = parameters.getParameter(PARAM_DISCOUNT_HOURTYPES, null);
        ret.dayWeekStarts = parameters.getParameter(PARAM_DAY_WEEK_STARTS);
        ret.premiumTimeCodeInserted = parameters.getParameter(PARAM_PREMIUM_TIMECODE_INSERTED, null);

        // *** assignBetterRate is a protected property inherited from GenericOvertimeRule
        ret.assignBetterRate = Boolean.valueOf(parameters.getParameter(PARAM_ASSIGN_BETTERRATE, "true")).booleanValue();

        ret.hourTypeForOvertimeWorkDetails = parameters.getParameter(PARAM_HOURTYPE_FOR_OVERTIME_WORKDETAILS, null);
        ret.p24HourStartTime = parameters.getParameter(PARAM_P24_HOUR_STARTTIME, P24HOUR_DEFAULT);
        ret.startTimeDay = parameters.getParameter(PARAM_STARTTIME_DAY, PARAM_VALUE_PREVIOUS_DAY);
        return ret;
    }


    public class ParametersResolved {
        public String hourSetDescription = null;
        public String workDetailTimeCodes = null;
        public String eligibleHourTypes = null;
        public String premiumTimeCodesCounted = null;
        public String discountTimeCodes = null;
        public String discountHourTypes= null;
        public String dayWeekStarts = null;
        public String premiumTimeCodeInserted = null;
        public String hourTypeForOvertimeWorkDetails = null;
        public String p24HourStartTime = null;
        public String startTimeDay = null;
        public boolean assignBetterRate;

    }
}