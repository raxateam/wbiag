package com.wbiag.app.ta.quickrules;

// standard imports for rules
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.sql.DBConnection;
import java.util.*;

// extra imports needed for this rule
import com.workbrain.app.ta.rules.GenericOvertimeRule;
import com.workbrain.util.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.sql.SQLException;

public class DailyOvertime24HourRule extends GenericOvertimeRule {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(DailyOvertime24HourRule.class);

    public final static int DEFAULT_DAYS_LOOK_BACK = 14;

    public final static String PARAM_HOURSET_DESCRIPTION = "HourSetDescription";
    public final static String PARAM_ADD_PREMIUM_FOR_FIRSTHOURTYPETOKEN = "AddPremiumForFirstHourtypeToken";
    public final static String PARAM_WORKDETAIL_TIMECODES = "WorkDetailTimeCodes";
    public final static String PARAM_ELIGIBLE_HOURTYPES = "EligibleHourTypes";
    public final static String PARAM_PREMIUM_TIMECODES_COUNTED = "PremiumTimeCodesCounted";
    public final static String PARAM_DISCOUNT_TIMECODES = "DiscountTimeCodes";
    public final static String PARAM_APPLY_BASED_ON_SCHEDULE = "ApplyBasedOnSchedule";
    public final static String PARAM_PREMIUM_TIMECODE_INSERTED = "PremiumTimeCodeInserted";
    public final static String PARAM_P24_HOUR_STARTTIME = "p24HourStartTime";
    public final static String PARAM_CALC_PERIOD = "CalculatePeriod";
    public final static String PARAM_ASSIGN_BETTERRATE = "AssignBetterRate";
    public final static String PARAM_HOURTYPE_FOR_OVERTIME_WORKDETAILS = "HourTypeForOvertimeWorkDetails";
    public final static String PARAM_MAX_DAYS_LOOK_BACK = "MaxDaysLookBack";

    public final static String CHOICE_EMP_VAL = "emp_val";
    public final static String CHOICE_24_HOUR_RESET = "24_HOUR_RESET";
    public final static String CHOICE_PREVIOUS_DAY_START = "PREVIOUS_DAY_START";

    // These were replace by CHOICE_24_HOUR_RESET, CHOICE_PREVIOUS_DAY_START.
    public final static String OLD_CHOICE_24_HOUR_REFRESH = "24_HOUR_REFRESH";
    public final static String OLD_CHOICE_24_HOUR_ROLLING = "24_HOUR_ROLLING";

    public final static String P24HOUR_DATE_FORMAT = "yyyyMMdd HHmmss";

    // Possible values for CalculatePeriod.
    public final static String PARAM_VAL_BEFORE_CUTOFF = "BEFORE_START_TIME";
    public final static String PARAM_VAL_AFTER_CUTOFF = "AFTER_START_TIME";
    public final static String PARAM_VAL_AFTER_24HOURS = "AFTER_24HOURS";
    public final static String PARAM_VAL_ALL = "ALL";

    //String discountTimeCodes = null;
    //int maxDaysLookBack = 0;

    /**
     * getParameterInfo
     */
    public List getParameterInfo(DBConnection conn) {

        List result = new ArrayList();
        RuleParameterInfo rpi = null;

        result.add(new RuleParameterInfo(PARAM_HOURSET_DESCRIPTION, RuleParameterInfo.STRING_TYPE, false));

        rpi = new RuleParameterInfo(PARAM_ADD_PREMIUM_FOR_FIRSTHOURTYPETOKEN, RuleParameterInfo.CHOICE_TYPE, true);
        rpi.addChoice(Boolean.TRUE.toString());
        rpi.addChoice(Boolean.FALSE.toString());
        result.add(rpi);

        result.add(new RuleParameterInfo(PARAM_WORKDETAIL_TIMECODES, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_ELIGIBLE_HOURTYPES, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_PREMIUM_TIMECODES_COUNTED, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_DISCOUNT_TIMECODES, RuleParameterInfo.STRING_TYPE, true));

        rpi = new RuleParameterInfo(PARAM_APPLY_BASED_ON_SCHEDULE, RuleParameterInfo.CHOICE_TYPE, true);
        rpi.addChoice(Boolean.TRUE.toString());
        rpi.addChoice(Boolean.FALSE.toString());
        result.add(rpi);

        result.add(new RuleParameterInfo(PARAM_PREMIUM_TIMECODE_INSERTED, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_P24_HOUR_STARTTIME, RuleParameterInfo.STRING_TYPE, true));

        rpi = new RuleParameterInfo(PARAM_CALC_PERIOD, RuleParameterInfo.CHOICE_TYPE, true);
        rpi.addChoice(PARAM_VAL_BEFORE_CUTOFF);
        rpi.addChoice(PARAM_VAL_AFTER_CUTOFF);
        rpi.addChoice(PARAM_VAL_AFTER_24HOURS);
        rpi.addChoice(PARAM_VAL_ALL);
        result.add(rpi);

        result.add(new RuleParameterInfo(PARAM_MAX_DAYS_LOOK_BACK, RuleParameterInfo.INT_TYPE, true));

        rpi = new RuleParameterInfo(PARAM_ASSIGN_BETTERRATE, RuleParameterInfo.CHOICE_TYPE, true);
        rpi.addChoice(Boolean.TRUE.toString());
        rpi.addChoice(Boolean.FALSE.toString());
        result.add(rpi);

        result.add(new RuleParameterInfo(PARAM_HOURTYPE_FOR_OVERTIME_WORKDETAILS, RuleParameterInfo.STRING_TYPE, true));

        return result;
    }


    /**
     * execute
     */
    public void execute(WBData wbData, Parameters parameters) throws Exception {

    	// Retrieve parameters
        String hourSetDescription = parameters.getParameter(PARAM_HOURSET_DESCRIPTION);
        boolean addPremiumForFirstHourtypeToken = Boolean.valueOf(parameters.getParameter(PARAM_ADD_PREMIUM_FOR_FIRSTHOURTYPETOKEN, Boolean.FALSE.toString())).booleanValue();
        String workDetailTimeCodes = parameters.getParameter(PARAM_WORKDETAIL_TIMECODES);
        String eligibleHourTypes = parameters.getParameter(PARAM_ELIGIBLE_HOURTYPES, "REG");
        String premiumTimeCodesCounted = parameters.getParameter(PARAM_PREMIUM_TIMECODES_COUNTED, null);
        String discountTimeCodes = parameters.getParameter(PARAM_DISCOUNT_TIMECODES, null);
        //boolean applyBasedOnSchedule = Boolean.valueOf(parameters.getParameter(PARAM_APPLY_BASED_ON_SCHEDULE, Boolean.FALSE.toString())).booleanValue();
        String premiumTimeCodeInserted = parameters.getParameter(PARAM_PREMIUM_TIMECODE_INSERTED, null);
        String twentyFourHourStartTime = parameters.getParameter(PARAM_P24_HOUR_STARTTIME, null);
        // *** assignBetterRate is a protected property inherited from GenericOvertimeRule
        boolean assignBetterRate = Boolean.valueOf(parameters.getParameter(PARAM_ASSIGN_BETTERRATE, Boolean.TRUE.toString())).booleanValue();
        String hourTypeForOvertimeWorkDetails = parameters.getParameter(PARAM_HOURTYPE_FOR_OVERTIME_WORKDETAILS, null);

        int maxDaysLookBack = parameters.getIntegerParameter(PARAM_MAX_DAYS_LOOK_BACK, DEFAULT_DAYS_LOOK_BACK);
        String calcPeriod = parameters.getParameter(PARAM_CALC_PERIOD, PARAM_VAL_ALL);

        if (!StringHelper.isEmpty(twentyFourHourStartTime)) {
            ruleDailyOTByZone(wbData, hourSetDescription, addPremiumForFirstHourtypeToken,
                              workDetailTimeCodes, eligibleHourTypes, premiumTimeCodeInserted,
                              twentyFourHourStartTime , hourTypeForOvertimeWorkDetails,
                              premiumTimeCodesCounted,
							  calcPeriod,
                              assignBetterRate,
                              maxDaysLookBack,
                              discountTimeCodes);
        }
        else {
            throw new RuntimeException("twentyFourHourStartTime must be supplied");
        }
    }

    /**
     *
     * @param wbData
     * @param hourSetDescription
     * @param addPremiumForFirstHourtypeToken
     * @param workDetailTimeCodes
     * @param eligibleHourTypes
     * @param premiumTimeCodeInserted
     * @param sTwentyFourHourStartTime
     * @param hourTypeForOvertimeWorkDetails
     * @param premiumTimeCodesCounted
     *
     * @throws Exception
     */
    protected void ruleDailyOTByZone (WBData wbData, String hourSetDescription,
                                   boolean addPremiumForFirstHourtypeToken,
                                   String workDetailTimeCodes,
                                   String eligibleHourTypes,
                                   String premiumTimeCodeInserted,
                                   String sTwentyFourHourStartTime,
                                   String hourTypeForOvertimeWorkDetails,
                                   String premiumTimeCodesCounted,
								   String calcPeriod,
                                   boolean assignBetterRate,
                                   int maxDaysLookBack,
                                   String discountTimeCodes) throws Exception {

        Date twentyFourHourStartTime = getCutoffTime(wbData,
        									sTwentyFourHourStartTime,
											workDetailTimeCodes,
											eligibleHourTypes,
                                            maxDaysLookBack) ;

        if (twentyFourHourStartTime == null) {
            return;
        }

        Date workSummaryDate = wbData.getRuleData().getWorkSummary().getWrksWorkDate();
        long noonInMillisSinceMidnight = DateHelper.getDateSetToNoon().getTime()
            - DateHelper.truncateToDays(DateHelper.getDateSetToNoon()).getTime();
        // *** 717 consider day start time and shift back a day only if the day start time AND the p24HourStartTime are after 12:00 (noon)
        Date dayStartTime = RuleHelper.getDayStartTime(
            wbData.getRuleData().getEmployeeData() , wbData.getDBconnection());
        if ((DateHelper.getDayFraction(dayStartTime) > noonInMillisSinceMidnight) &&
        		(DateHelper.getDayFraction(twentyFourHourStartTime) > noonInMillisSinceMidnight)) {
            workSummaryDate = DateHelper.addDays(workSummaryDate , -1);
        }

        Date cutOff = DateHelper.setTimeValues(workSummaryDate, twentyFourHourStartTime);
        if (logger.isDebugEnabled()) logger.debug("Final cutoff:" + cutOff);

        DateFormat dateFormat = new SimpleDateFormat(WBData.DATE_FORMAT_STRING);

        int seedMinutes = 0;

        if (premiumTimeCodesCounted != null) {
            seedMinutes += wbData.getMinutesWorkPremium(premiumTimeCodesCounted, true, null, true, null);
        }

        if (discountTimeCodes != null) {
            seedMinutes +=
                wbData.getMinutesWorkDetailPremiumRange(DateHelper.
                addDays(workSummaryDate, -1),
                wbData.getWrksWorkDate(),
                DateHelper.addDays(cutOff, -1),
                DateHelper.addDays(cutOff, 1),
                discountTimeCodes, true,
                eligibleHourTypes, true, "D", false);
        }

        Parameters parametersForGenericOvertimeRule = null;

        // ----------------- before cutOff time
        if (PARAM_VAL_BEFORE_CUTOFF.equals(calcPeriod) || PARAM_VAL_ALL.equals(calcPeriod)) {

        	parametersForGenericOvertimeRule = new Parameters();

	        parametersForGenericOvertimeRule.addParameter(super.PARAM_ADDITIONAL_MINUTES_WORKED,
	        												"" + seedMinutes);
	        parametersForGenericOvertimeRule.addParameter(super.PARAM_HOURSET_DESCRIPTION,
	        												hourSetDescription);
	        parametersForGenericOvertimeRule.addParameter(PARAM_ADD_PREMIUM_FOR_FIRSTHOURTYPETOKEN,
	        												String.valueOf(addPremiumForFirstHourtypeToken));
	        parametersForGenericOvertimeRule.addParameter(super.PARAM_ELIGIBLE_TIMECODES,
	        												workDetailTimeCodes);
	        parametersForGenericOvertimeRule.addParameter(super.PARAM_ARE_TIMECODES_INCLUSIVE,
	        												Boolean.TRUE.toString());
	        parametersForGenericOvertimeRule.addParameter(super.PARAM_ELIGIBLE_HOURTYPES,
	        												eligibleHourTypes);
	        parametersForGenericOvertimeRule.addParameter(super.PARAM_ADD_PREMIUMRECORD,
	        												"" + (premiumTimeCodeInserted != null));
	        parametersForGenericOvertimeRule.addParameter(super.PARAM_PREMIUM_TIMECODE,
	        												premiumTimeCodeInserted);

	        parametersForGenericOvertimeRule.addParameter(super.PARAM_INTERVAL_STARTDATE,
	        												dateFormat.format(DateHelper.addDays(workSummaryDate, -1)));
	        parametersForGenericOvertimeRule.addParameter(super.PARAM_INTERVAL_ENDDATE,
	        												dateFormat.format(DateHelper.addDays(workSummaryDate, -1)));
	        parametersForGenericOvertimeRule.addParameter(super.PARAM_INTERVAL_STARTTIME,
	        												dateFormat.format(DateHelper.addDays(cutOff, -1)));

	        // only include "IntervalEndTime" if it is NOT a OFF day
	        if (wbData.getEmployeeScheduleData().isEmployeeScheduledActual()
                && wbData.getRuleData().getWorkDetails().size() > 0) {
	            parametersForGenericOvertimeRule.addParameter(super.PARAM_INTERVAL_ENDTIME,
                    dateFormat.format(wbData.getRuleData().getWorkDetail(0).getWrkdStartTime()));
	        } else {
	            parametersForGenericOvertimeRule.addParameter(super.PARAM_INTERVAL_ENDTIME, null);
	        }

	        parametersForGenericOvertimeRule.addParameter(super.PARAM_STARTTIME_WITHIN_SHIFT, null);
	        parametersForGenericOvertimeRule.addParameter(super.PARAM_ENDTIME_WITHIN_SHIFT,
	        												dateFormat.format(cutOff));


	        parametersForGenericOvertimeRule.addParameter(super.PARAM_HOURTYPE_FOR_OVERTIME_WORKDETAILS,
	        												hourTypeForOvertimeWorkDetails);
            parametersForGenericOvertimeRule.addParameter(super.PARAM_ASSIGN_BETTERRATE,
                String.valueOf(assignBetterRate));
	        // Execute the GenericOvertimeRule.
	        super.execute(wbData, parametersForGenericOvertimeRule);
        }

        // ------------- after cutOff time
        if (PARAM_VAL_AFTER_CUTOFF.equals(calcPeriod) || PARAM_VAL_ALL.equals(calcPeriod)) {

	        parametersForGenericOvertimeRule = new Parameters();

	        parametersForGenericOvertimeRule.addParameter(super.PARAM_ADDITIONAL_MINUTES_WORKED,
	        												"" + seedMinutes);
	        parametersForGenericOvertimeRule.addParameter(super.PARAM_HOURSET_DESCRIPTION,
	        												hourSetDescription);
	        parametersForGenericOvertimeRule.addParameter(super.PARAM_ADD_PREMIUM_FOR_FIRSTHOURTYPETOKEN,
	        												String.valueOf(addPremiumForFirstHourtypeToken));

	        Date startTimeForAfterCutOffTime = findStartTimeForAfterCutOffTime(wbData, cutOff);

	        parametersForGenericOvertimeRule.addParameter(super.PARAM_INTERVAL_STARTDATE, null);
	        parametersForGenericOvertimeRule.addParameter(super.PARAM_INTERVAL_ENDDATE, null);
	        parametersForGenericOvertimeRule.addParameter(super.PARAM_INTERVAL_STARTTIME, null);
            parametersForGenericOvertimeRule.addParameter(super.PARAM_INTERVAL_ENDTIME, null);

	        parametersForGenericOvertimeRule.addParameter(super.PARAM_STARTTIME_WITHIN_SHIFT,
	        												dateFormat.format(startTimeForAfterCutOffTime));
	        parametersForGenericOvertimeRule.addParameter(super.PARAM_ENDTIME_WITHIN_SHIFT,
	        												dateFormat.format(DateHelper.addDays(cutOff, 1)));

	        parametersForGenericOvertimeRule.addParameter(super.PARAM_ELIGIBLE_TIMECODES,
	        												workDetailTimeCodes);
	        parametersForGenericOvertimeRule.addParameter(super.PARAM_ARE_TIMECODES_INCLUSIVE,
	        												Boolean.TRUE.toString());
	        parametersForGenericOvertimeRule.addParameter(super.PARAM_ELIGIBLE_HOURTYPES,
	        												eligibleHourTypes);
	        parametersForGenericOvertimeRule.addParameter(super.PARAM_ADD_PREMIUMRECORD,
	        												"" + (premiumTimeCodeInserted != null));
	        parametersForGenericOvertimeRule.addParameter(super.PARAM_PREMIUM_TIMECODE,
	        												premiumTimeCodeInserted);
	        parametersForGenericOvertimeRule.addParameter(super.PARAM_HOURTYPE_FOR_OVERTIME_WORKDETAILS,
	        												hourTypeForOvertimeWorkDetails);
            parametersForGenericOvertimeRule.addParameter(super.PARAM_ASSIGN_BETTERRATE,
                String.valueOf(assignBetterRate));

	        // Execute the GenericOvertimeRule
	        super.execute(wbData, parametersForGenericOvertimeRule);

        }

        //------------- after 24 hours
        if (PARAM_VAL_AFTER_24HOURS.equals(calcPeriod) || PARAM_VAL_ALL.equals(calcPeriod)) {

	        parametersForGenericOvertimeRule = new Parameters();

	        parametersForGenericOvertimeRule.addParameter(super.PARAM_ADDITIONAL_MINUTES_WORKED,
	        												"" + seedMinutes);
	        parametersForGenericOvertimeRule.addParameter(super.PARAM_HOURSET_DESCRIPTION,
	        												hourSetDescription);
	        parametersForGenericOvertimeRule.addParameter(super.PARAM_ADD_PREMIUM_FOR_FIRSTHOURTYPETOKEN,
	        												String.valueOf(addPremiumForFirstHourtypeToken));
			parametersForGenericOvertimeRule.addParameter(super.PARAM_ELIGIBLE_TIMECODES,
	        												workDetailTimeCodes);
	        parametersForGenericOvertimeRule.addParameter(super.PARAM_ARE_TIMECODES_INCLUSIVE,
	        												Boolean.TRUE.toString());
	        parametersForGenericOvertimeRule.addParameter(super.PARAM_ELIGIBLE_HOURTYPES,
	        												eligibleHourTypes);
	        parametersForGenericOvertimeRule.addParameter(super.PARAM_ADD_PREMIUMRECORD,
	        												"" + (premiumTimeCodeInserted != null));
	        parametersForGenericOvertimeRule.addParameter(super.PARAM_PREMIUM_TIMECODE,
	        												premiumTimeCodeInserted);
	        parametersForGenericOvertimeRule.addParameter(super.PARAM_HOURTYPE_FOR_OVERTIME_WORKDETAILS,
	        												hourTypeForOvertimeWorkDetails);


	        parametersForGenericOvertimeRule.addParameter(super.PARAM_INTERVAL_STARTDATE, null);
	        parametersForGenericOvertimeRule.addParameter(super.PARAM_INTERVAL_ENDDATE, null);
	        parametersForGenericOvertimeRule.addParameter(super.PARAM_INTERVAL_STARTTIME, null);
            parametersForGenericOvertimeRule.addParameter(super.PARAM_INTERVAL_ENDTIME, null);


	        parametersForGenericOvertimeRule.addParameter(super.PARAM_STARTTIME_WITHIN_SHIFT, dateFormat.format(DateHelper.addDays(cutOff, 1)));
	        parametersForGenericOvertimeRule.addParameter(super.PARAM_ENDTIME_WITHIN_SHIFT, null);
            parametersForGenericOvertimeRule.addParameter(super.PARAM_ASSIGN_BETTERRATE,
                String.valueOf(assignBetterRate));

	        // Execute the GenericOvertimeRule
	        super.execute(wbData, parametersForGenericOvertimeRule);
        }
    }


    /**
     *
     * @param wbData
     * @param sTwentyFourHourStartTime
     * @param workDetailTimeCodes
     * @param eligibleHourTypes
     * @return
     * @throws SQLException
     */
    protected Date getCutoffTime(WBData wbData,
    								String sTwentyFourHourStartTime,
									String workDetailTimeCodes,
									String eligibleHourTypes,
                                    int maxDaysLookBack) throws Exception {

        if (StringHelper.isEmpty(sTwentyFourHourStartTime)) {
            throw new RuntimeException("Parameter : " + PARAM_P24_HOUR_STARTTIME + " cannot be empty");
        }

        Date twentyFourHourStartTime = null;

        // Start time is taken from an emp val.
        if (sTwentyFourHourStartTime.startsWith(CHOICE_EMP_VAL)) {
        	twentyFourHourStartTime = getEmpValueCutoffTime(wbData, sTwentyFourHourStartTime);

    	// Find the earliest time of day that is not more than 24 hours apart.
        } else if (OLD_CHOICE_24_HOUR_REFRESH.equals(sTwentyFourHourStartTime)
        			|| CHOICE_24_HOUR_RESET.equals(sTwentyFourHourStartTime)) {
            twentyFourHourStartTime = get24HrResetTime(wbData,
                workDetailTimeCodes, eligibleHourTypes, maxDaysLookBack);

        /*
         * Start time equals the start time of the previous day
         * if there is no workdetails previous day then the start time equals start time of the current day.
         */
        } else if (OLD_CHOICE_24_HOUR_ROLLING.equals(sTwentyFourHourStartTime)
        			|| CHOICE_PREVIOUS_DAY_START.equals(sTwentyFourHourStartTime)) {
        	twentyFourHourStartTime = getPreviousDayStart(wbData, workDetailTimeCodes, eligibleHourTypes);

        // Start time is given as a hard coded time.
        } else {
        	twentyFourHourStartTime = getTime(sTwentyFourHourStartTime);
        }

        if (logger.isDebugEnabled()) {
        	logger.debug("Cutoff time is : " + twentyFourHourStartTime);
        }

        return twentyFourHourStartTime;
    }

    /**
     * The cutoff time is stored in an emp val.  If the emp val us empty, an exception is thrown.
     *
     * @param wbData
     * @param sTwentyFourHourStartTime
     * @return
     */
    protected Date getEmpValueCutoffTime(WBData wbData, String sTwentyFourHourStartTime) throws Exception {

        String empVal = (String) wbData.getRuleData().getEmployeeData().getField(sTwentyFourHourStartTime);

    	if (StringHelper.isEmpty(empVal)) {
            throw new RuntimeException(sTwentyFourHourStartTime + " emp_val is empty");
        }

        return DateHelper.parseDate(empVal , P24HOUR_DATE_FORMAT);
    }

    /**
     * If it is more than 24 hours since last eligible start, then today eligible start is the cutoff.
     * Else last eligible start is cutoff.
     * If no last eligible start, today's eligible start is cutoff
     *
     * @param wbData
     * @param workDetailTimeCodes
     * @param eligibleHourTypes
     * @return
     * @throws SQLException
     */
    protected Date get24HrResetTime(WBData wbData,
                                    String workDetailTimeCodes,
                                    String eligibleHourTypes,
                                    int maxDaysLookBack)
    											throws Exception {

        Date twentyFourHourStartTime = null;
        WorkDetailList workDetails = null;

        int firstRecIndex = wbData.getRuleData().getWorkDetails().getFirstRecordIndex(
        									workDetailTimeCodes , true, eligibleHourTypes , true);

        Date checkDayStartTime = null;
        Date startTimeNextDay = null;

        // If there was no match for today, just return null.
        if (firstRecIndex != -1) {

            twentyFourHourStartTime = wbData.getRuleData().getWorkDetails().getWorkDetail(firstRecIndex).getWrkdStartTime();
            startTimeNextDay = twentyFourHourStartTime;

            if (logger.isDebugEnabled()) {
            	logger.debug("startTime so far: " + twentyFourHourStartTime);
            }

            // Look at work details for previous dates until a gap of > than 24hrs between start times.
        	for (int i = 1 ; i <= maxDaysLookBack ; i++ ) {

        		// Get the work details.
        		workDetails = wbData.getWorkDetailsForDate(
                				DateHelper.addDays(wbData.getWrksWorkDate() , -1 * i));

        		// Get the first record.
                firstRecIndex = workDetails.getFirstRecordIndex(
                				workDetailTimeCodes , true, eligibleHourTypes , true);

                // Get the start time of the first record.
                // If no records are found for the date then we are done parsing.
                if (firstRecIndex == -1) {
                	if (logger.isDebugEnabled()) {
                		logger.debug("No matching records found for day: "
                						+ DateHelper.addDays(wbData.getWrksWorkDate() , -1 * i)
										+ ". No need to look furthur.");
                	}
                	break;

                } else {
                	checkDayStartTime = workDetails.getWorkDetail(firstRecIndex).getWrkdStartTime();

                	if (logger.isDebugEnabled()) {
                		logger.debug("checkDayStartTime: " + checkDayStartTime);
                		logger.debug("startTimeNextDay: " + startTimeNextDay);
                	}

                	if (DateHelper.getHoursBetween(startTimeNextDay, checkDayStartTime) > 24) {
                		// More than a 24 hour gap so use the start time we got so far.
                		if (logger.isDebugEnabled()) {
                			logger.debug("Greater than 24 hour gap found.  No need to look furthur.");
                		}
                		break;
                	} else {
                		// Less than a 24 hour gap.  Use the time from the checkDay.
                		twentyFourHourStartTime = checkDayStartTime;

                		if (logger.isDebugEnabled()) {
                			logger.debug("Less than 24hr gap.  New start time is: " + twentyFourHourStartTime);
                		}
                	}
                }

                startTimeNextDay = checkDayStartTime;
        	}
        }

        return twentyFourHourStartTime;
    }

    /**
     * Start time equals the start time of the previous day
     * if there is no workdetails previous day then the start time equals start time of the current day.
     *
     * @param wbData
     * @param workDetailTimeCodes
     * @param eligibleHourTypes
     * @return
     * @throws SQLException
     */
    protected Date getPreviousDayStart(WBData wbData, String workDetailTimeCodes, String eligibleHourTypes)
    											throws SQLException {

        Date twentyFourHourStartTime = null;

        // Get the previous day work details.
        WorkDetailList detailList = wbData.getWorkDetailsForDate(
    											DateHelper.addDays(wbData.getWrksWorkDate(), -1));

        // Get the first work detail for yesterday.
    	int firstRecIndex = detailList.getFirstRecordIndex(
    									workDetailTimeCodes , true, eligibleHourTypes , true);

    	// If a work detail existed yesterday, use the start time as the cutoff time.
    	if (firstRecIndex != -1) {
            twentyFourHourStartTime = detailList.getWorkDetail(firstRecIndex).getWrkdStartTime();

        // If a work detail did not exist yesterday, use the first record today.
    	} else {
            detailList = wbData.getRuleData().getWorkDetails();

            firstRecIndex = detailList.getFirstRecordIndex(
            							workDetailTimeCodes, true, eligibleHourTypes, true);
            if (firstRecIndex != -1) {
                twentyFourHourStartTime = detailList.getWorkDetail(firstRecIndex).getWrkdStartTime();
            }
        }

    	return twentyFourHourStartTime;
    }

    /**
     *
     * @param sTwentyFourHourStartTime
     * @return
     * @throws Exception
     */
    protected Date getTime(String sTwentyFourHourStartTime) throws Exception {

    	Date twentyFourHourStartTime = null;

    	try {
    		twentyFourHourStartTime = DateHelper.parseDate(sTwentyFourHourStartTime, P24HOUR_DATE_FORMAT);
    	} catch (Exception ex) {
    		throw new RuntimeException("Parameter value for "
    									+ PARAM_P24_HOUR_STARTTIME + " : "
										+ sTwentyFourHourStartTime + " is not supported and could not pe parsed");

    	}

    	return twentyFourHourStartTime;
    }

    /**
     * This method returns the start time for after the cutOff time
     * Logic:
     * returns the cutOff time for the following two conditions:
     *   1 - if there exist some work record covering the cutOff time, OR
     *   2 - there exist no work records after the cutOff time
     * The method will return the start time of a record, if:
     *   1 - such record starts after the cutOff time, AND
     *   2 - there exist no work record covering the cutOff time
     *
     * @param wbData
     * @param cutOff
     * @return
     */
    private Date findStartTimeForAfterCutOffTime(WBData wbData, Date cutOff) {

    	WorkDetailData curWorkDetail = null;
    	Date startTime = null;
    	Date endTime = null;

    	for (int i=0; i<wbData.getRuleData().getWorkDetails().size(); i++) {

    		curWorkDetail = wbData.getRuleData().getWorkDetail(i);
            startTime = curWorkDetail.getWrkdStartTime();
            endTime = curWorkDetail.getWrkdEndTime();

            if (startTime.getTime() <= cutOff.getTime() &&
                endTime.getTime() >= cutOff.getTime()) {
                return cutOff;
            }
            if (startTime.getTime() > cutOff.getTime()) {
                return startTime;
            }
        }

        return cutOff;
    }

    public String getComponentName() {
        return "WBIAG: Daily Overtime 24 Hour Rule";
    }


    public String getComponentUI() {
        return "/quickrules/qDailyOvertime24HrParams.jsp";
    }
}