package com.wbiag.app.ta.quickrules;


import java.sql.*;
import java.text.*;
import java.util.*;
import java.util.Date;

import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.app.ta.rules.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;

public class DailyOvertime24HourSkdRollRule extends GenericOvertimeRule {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(DailyOvertime24HourSkdRollRule.class);

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

    public final static String P24HOUR_DATE_FORMAT = "yyyyMMdd HHmmss";

    // Possible values for p24HourStartTime
    public final static String PARAM_VAL_24_HOUR_SCHEDULE = "SCHEDULE";
    public final static String PARAM_VAL_SCHEDULE_ROLLING_LAST_HSET = "SCHEDULE_ROLLING_LAST_HSET";

    // Possible values for CalculatePeriod.
    public final static String PARAM_VAL_BEFORE_CUTOFF = "BEFORE_START_TIME";
    public final static String PARAM_VAL_AFTER_CUTOFF = "AFTER_START_TIME";
    public final static String PARAM_VAL_ALL = "ALL";

    // Rule Parameters.


    /**
     *  (non-Javadoc)
     * @see com.workbrain.app.ta.ruleengine.RuleComponent#getParameterInfo(com.workbrain.sql.DBConnection)
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
        result.add(new RuleParameterInfo(PARAM_P24_HOUR_STARTTIME, RuleParameterInfo.STRING_TYPE, false));

        rpi = new RuleParameterInfo(PARAM_CALC_PERIOD, RuleParameterInfo.CHOICE_TYPE, true);
        rpi.addChoice(PARAM_VAL_BEFORE_CUTOFF);
        rpi.addChoice(PARAM_VAL_AFTER_CUTOFF);
        rpi.addChoice(PARAM_VAL_ALL);
        result.add(rpi);

        rpi = new RuleParameterInfo(PARAM_ASSIGN_BETTERRATE, RuleParameterInfo.CHOICE_TYPE, true);
        rpi.addChoice(Boolean.TRUE.toString());
        rpi.addChoice(Boolean.FALSE.toString());
        result.add(rpi);

        result.add(new RuleParameterInfo(PARAM_HOURTYPE_FOR_OVERTIME_WORKDETAILS, RuleParameterInfo.STRING_TYPE, true));

        return result;
    }

    /**
     * Retrieve the values in the config parameters.
     *
     * @param wbData
     * @param parameters
     */
    protected ParametersResolved getParameters(WBData wbData, Parameters parameters) {
        ParametersResolved ret = new ParametersResolved();
        ret.hourSetDescription = parameters.getParameter(PARAM_HOURSET_DESCRIPTION);
        ret.addPremiumForFirstHourtypeToken = Boolean.valueOf(parameters.getParameter(PARAM_ADD_PREMIUM_FOR_FIRSTHOURTYPETOKEN, Boolean.FALSE.toString())).booleanValue();
        ret.workDetailTimeCodes = parameters.getParameter(PARAM_WORKDETAIL_TIMECODES);
        ret.eligibleHourTypes = parameters.getParameter(PARAM_ELIGIBLE_HOURTYPES, "REG");
        ret.premiumTimeCodesCounted = parameters.getParameter(PARAM_PREMIUM_TIMECODES_COUNTED, null);
        ret.discountTimeCodes = parameters.getParameter(PARAM_DISCOUNT_TIMECODES, null);
        ret.premiumTimeCodeInserted = parameters.getParameter(PARAM_PREMIUM_TIMECODE_INSERTED, null);
        ret.twentyFourHourStartTime = parameters.getParameter(PARAM_P24_HOUR_STARTTIME, null);

        // *** assignBetterRate is a protected property inherited from GenericOvertimeRule
        ret.assignBetterRate = Boolean.valueOf(parameters.getParameter(PARAM_ASSIGN_BETTERRATE, Boolean.TRUE.toString())).booleanValue();

        ret.hourTypeForOvertimeWorkDetails = parameters.getParameter(PARAM_HOURTYPE_FOR_OVERTIME_WORKDETAILS, null);
        ret.calcPeriod = parameters.getParameter(PARAM_CALC_PERIOD, PARAM_VAL_ALL);

        return ret;
    }


    /**
     *  (non-Javadoc)
     * @see com.workbrain.app.ta.ruleengine.Rule#execute(com.workbrain.app.ta.ruleengine.WBData, com.workbrain.app.ta.ruleengine.Parameters)
     */
    public void execute(WBData wbData, Parameters parameters) throws java.lang.Exception {

    	// If there are no work details there is nothing to do.
        if (wbData.getRuleData().getWorkDetails().size() == 0) {
            if (logger.isDebugEnabled()) {
            	logger.debug("No work details to apply overtime");
            }
            return;
        }

    	// Retrieve parameters
    	ParametersResolved pars = getParameters(wbData, parameters);

    	// Run the rule.
    	ruleDailyOTBySchedule(wbData, pars);
    }


    /**
     * Performs the OT Calculations.
     *
     * @param wbData
     * @throws java.lang.Exception
     */
    protected void ruleDailyOTBySchedule (WBData wbData, ParametersResolved pars ) throws java.lang.Exception {

        Date workSummaryDate = wbData.getRuleData().getWorkSummary().getWrksWorkDate();
        EmployeeScheduleData esdYest = wbData.getEmployeeScheduleData(DateHelper.addDays(workSummaryDate , -1) ) ;
        EmployeeScheduleData esdYestBefore = wbData.getEmployeeScheduleData(DateHelper.addDays(workSummaryDate , -2) ) ;
        EmployeeScheduleData esdCurr = wbData.getEmployeeScheduleData();
        EmployeeScheduleData esdTom = wbData.getEmployeeScheduleData(DateHelper.addDays(workSummaryDate , 1));
        Date cutOffCurr = null;
        Date cutOffYest = null;

        if (esdCurr.isEmployeeScheduledActual()) {
            cutOffCurr = get24HourStartTime(esdCurr);
        }

        if (esdYest.isEmployeeScheduledActual()) {

        	cutOffYest = get24HourStartTime(esdYest);

            // *** if the day before yesterday is off, adjust cutoff time
            if (!esdYestBefore.isEmployeeScheduledActual()) {
                WorkDetailList wdlYest = wbData.getWorkDetailsForDate(esdYest.getWorkDate());
                if (wdlYest.size() > 0) {
                    cutOffYest = wdlYest.getWorkDetail(0).getWrkdStartTime();
                }
            }
        }

        if (logger.isDebugEnabled()) {
        	logger.debug("cutOffCurr : " + cutOffCurr + "-cutOffYest : " + cutOffYest);
        }

        // before cutOff time
        if (PARAM_VAL_BEFORE_CUTOFF.equals(pars.calcPeriod) || PARAM_VAL_ALL.equals(pars.calcPeriod)) {

        	if (cutOffYest != null) {

        		executeGenericOT(wbData,
	                             pars.discountTimeCodes,
	                             pars.hourSetDescription,
	                             pars.addPremiumForFirstHourtypeToken,
	                             pars.workDetailTimeCodes, pars.eligibleHourTypes,
	                             pars.premiumTimeCodeInserted,
	                             pars.hourTypeForOvertimeWorkDetails,
	                             pars.premiumTimeCodesCounted,
	                             null,
	                             cutOffCurr,
	                             DateHelper.addDays(workSummaryDate, -1),
	                             DateHelper.addDays(workSummaryDate, -1),
	                             cutOffYest,
	                             wbData.getRuleData().getWorkDetail(0).
	                             getWrkdStartTime());
	        }
        }

        // after cutOff time
        if (PARAM_VAL_AFTER_CUTOFF.equals(pars.calcPeriod) || PARAM_VAL_ALL.equals(pars.calcPeriod)) {

	        Date startTimeForAfterCutOffTime = findStartTimeForAfterCutOffTime(wbData, cutOffCurr , cutOffYest);
	        if (logger.isDebugEnabled()) {
	        	logger.debug("startTimeForAfterCutOffTime : " + startTimeForAfterCutOffTime);
	        }

	        // Apply the Last Hourset functionality.
	        // If the last hourset occurs on the previous day, then all work details
	        // today until the employee leaves is also set to the last hourset.
	        if (PARAM_VAL_SCHEDULE_ROLLING_LAST_HSET.equals(pars.twentyFourHourStartTime)) {
	        	processRollingSkd(wbData, pars.hourSetDescription,
                                  cutOffCurr, pars.workDetailTimeCodes, pars);
	        }

	        // Process OT for if the last hourset function does not apply, or if it does but
	        // then the employee returned to work.
            executeGenericOT(wbData,
                         pars.discountTimeCodes,
                         pars.hourSetDescription, pars.addPremiumForFirstHourtypeToken,
                         pars.workDetailTimeCodes, pars.eligibleHourTypes, pars.premiumTimeCodeInserted,
                         pars.hourTypeForOvertimeWorkDetails,
                         pars.premiumTimeCodesCounted,
                         startTimeForAfterCutOffTime,
                         DateHelper.addDays(cutOffCurr, 1),
                         null,
                         null,
                         null,
                         null);
        }
    }

    /**
     * Set-up and execute the generic OT rule.
     *
     * @param wbData
     * @param discountTimeCodes
     * @param hourSetDescription
     * @param addPremiumForFirstHourtypeToken
     * @param workDetailTimeCodes
     * @param eligibleHourTypes
     * @param premiumTimeCodeInserted
     * @param hourTypeForOvertimeWorkDetails
     * @param premiumTimeCodesCounted
     * @param starttimeWithinShift
     * @param endtimeWithinShift
     * @param intervalStartDate
     * @param intervalEndDate
     * @param intervalStartTime
     * @param intervalEndTime
     * @throws Exception
     */
    private void executeGenericOT(WBData wbData,
                                  String discountTimeCodes,
                                  String hourSetDescription,
                                  boolean addPremiumForFirstHourtypeToken,
                                  String workDetailTimeCodes,
                                  String eligibleHourTypes,
                                  String premiumTimeCodeInserted,
                                  String hourTypeForOvertimeWorkDetails,
                                  String premiumTimeCodesCounted,
                                  Date starttimeWithinShift,
                                  Date endtimeWithinShift,
                                  Date intervalStartDate,
                                  Date intervalEndDate,
                                  Date intervalStartTime,
                                  Date intervalEndTime
                                  ) throws Exception {

    	DateFormat dateFormat = new SimpleDateFormat(WBData.DATE_FORMAT_STRING);

        int seedMinutes = 0;
        if (premiumTimeCodesCounted != null) {
            seedMinutes += wbData.getMinutesWorkPremium(premiumTimeCodesCounted, true, null, true, null);
        }

        if (discountTimeCodes != null) {
            seedMinutes +=
                wbData.getMinutesWorkDetailPremiumRange(intervalStartDate, intervalEndDate,
                intervalStartTime, intervalEndTime, discountTimeCodes, true,
                eligibleHourTypes, true, "D", false);
        }


        Parameters parametersForGenericOvertimeRule = new Parameters();
        parametersForGenericOvertimeRule.addParameter("AdditionalMinutesWorked",
            String.valueOf(seedMinutes));
        parametersForGenericOvertimeRule.addParameter("HourSetDescription",
            hourSetDescription);
        parametersForGenericOvertimeRule.addParameter(
            "AddPremiumForFirstHourtypeToken",
            String.valueOf(addPremiumForFirstHourtypeToken));
        if (starttimeWithinShift != null) {
            parametersForGenericOvertimeRule.addParameter("StartTimeWithinShift",
                dateFormat.format(starttimeWithinShift));
        }

        if (endtimeWithinShift != null) {
            parametersForGenericOvertimeRule.addParameter("EndTimeWithinShift",
                dateFormat.format(endtimeWithinShift));
        }
        parametersForGenericOvertimeRule.addParameter("EligibleTimeCodes",
            workDetailTimeCodes);
        parametersForGenericOvertimeRule.addParameter("AreTimeCodesInclusive",
            Boolean.TRUE.toString());
        parametersForGenericOvertimeRule.addParameter("EligibleHourTypes",
            eligibleHourTypes);
        parametersForGenericOvertimeRule.addParameter("AddPremiumRecord",
            String.valueOf(premiumTimeCodeInserted != null));
        parametersForGenericOvertimeRule.addParameter("PremiumTimeCode",
            premiumTimeCodeInserted);
        if (intervalStartDate != null) {
            parametersForGenericOvertimeRule.addParameter("IntervalStartDate",
                dateFormat.format(intervalStartDate));
        }
        if (intervalEndDate != null) {
            parametersForGenericOvertimeRule.addParameter("IntervalEndDate",
                dateFormat.format(intervalEndDate));
        }
        if (intervalStartTime != null) {
            parametersForGenericOvertimeRule.addParameter("IntervalStartTime",
                dateFormat.format(intervalStartTime));
        }
        if (intervalEndTime != null) {
            parametersForGenericOvertimeRule.addParameter("IntervalEndTime",
                dateFormat.format(intervalEndTime));
        }
        parametersForGenericOvertimeRule.addParameter("HourTypeForOvertimeWorkDetails",
            hourTypeForOvertimeWorkDetails);

        super.execute(wbData, parametersForGenericOvertimeRule);
    }

    /**
     * If the last hourset appears on the previous day, but not all day, then extend
     * that hour type until the employee goes home today.
     *
     * @param wbData
     * @param hourSetDescription
     * @param cutOff
     * @throws SQLException
     */
    private void processRollingSkd(WBData wbData, String hourSetDescription,
                                   Date cutOff, String workDetailTimeCodes,
                                   ParametersResolved pars) throws SQLException {

    	Date endWorkTime = null;

    	// Get a list of hour types from the hourset.
    	List htypes = com.wbiag.util.StringHelper.detokenizeStringAsNameValueList(hourSetDescription, ",", "=", true);
        if (htypes.size() == 0) {
            return;
        }

        // Check if any of the last hour type exists in the previous day.
        if (isExtendLastHtype(wbData, htypes, cutOff, pars)) {

        	// Find what time the employee
        	// is relieved from work. ie. UAT or GAP.
        	endWorkTime = wbData.getMinStartTime("UAT,GAP", false);

	        // Get the last hour type.
	        String lastHtypeName = ((NameValue) htypes.get(htypes.size() - 1)).getName();

        	// Apply the last hour type to all hours until the employee
        	// is relieved from work.
        	wbData.setWorkDetailHtypeName(lastHtypeName, cutOff, endWorkTime, workDetailTimeCodes, true);
        }
    }

    /**
	 * @param wbData
	 * @param otherHTypeName
	 * @param lastHtypeName
	 * @param cutOff
	 * @return
	 */
	private boolean isExtendLastHtype(WBData wbData, List htypes,
                                      Date cutOff,
                                      ParametersResolved pars) throws SQLException {

		// See if there are any details before the default shift start time.
		int index = wbData.getRuleData().getWorkDetails().getLastRecordIndex(null, wbData.getEmployeeScheduleData().getEmpskdDefStartTime(), true);

		if (index == -1) {
			// If there no records prior to the default start time, then not extending.
			return false;

		} else {
			String lastHTypeName = ((NameValue) htypes.get(htypes.size() - 1)).getName();
			int lastHTypeId = wbData.getRuleData().getCodeMapper().getHourTypeByName(lastHTypeName).getHtypeId();

			// If the last record before the default start time has the last hour type
			// in the hourset.
			if (wbData.getRuleData().getWorkDetail(index).getHtypeId() == lastHTypeId) {

				// Ensure that the last hour type was not set by some other rule or override.
				// To do this, ensure that at least x minutes was worked in the hour types
				// not including the last hour type.
				int minutesQualifyLastHType = getMinutesQualifyLastHType(htypes);
				int minutesWorkedIn24Hr = wbData.getMinutesWorkDetailRange(
												DateHelper.addDays(wbData.getWrksWorkDate(), -1),
												wbData.getWrksWorkDate(),
												wbData.getEmployeeScheduleData(DateHelper.addDays(
																				wbData.getWrksWorkDate(), -1)
																				).getEmpskdDefStartTime(),
												wbData.getEmployeeScheduleData().getEmpskdDefStartTime(),
												pars.workDetailTimeCodes, true,
												pars.eligibleHourTypes, true);

				if (minutesWorkedIn24Hr > minutesQualifyLastHType) {
					return true;
				} else {
					return false;
				}

			} else {
				return false;
			}
		}
	}


	/**
	 * Sum the minutes in each token of the hourset, not including the last token.
	 *
	 * @param htypes
	 * @return
	 */
	private int getMinutesQualifyLastHType(List htypes) {

		int minutes = 0;

		for (int i=0; i < htypes.size() -1; i++) {
			minutes += Integer.parseInt(((NameValue) htypes.get(i)).getValue());
		}

		return minutes;
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
    private Date findStartTimeForAfterCutOffTime(WBData wbData, Date cutOff, Date cutOffYest) {

    	for (int i=0; i<wbData.getRuleData().getWorkDetails().size(); i++) {

    		WorkDetailData curWorkDetail = wbData.getRuleData().getWorkDetail(i);
            Date startTime = curWorkDetail.getWrkdStartTime();
            Date endTime = curWorkDetail.getWrkdEndTime();

            if (cutOffYest == null && i==0) {
                return startTime;
            }

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


    /**
     * Retrieves the default shift start time or null if off.
     *
     * @return Datetime object
     */
    private java.util.Date get24HourStartTime(EmployeeScheduleData esd) {
        return esd.getEmpskdActStartTime();
    }


    public String getComponentName() {
        return "WBIAG: Daily Overtime 24 Hour Schedule Rolling Rule";
    }


    public String getComponentUI() {
        return "/quickrules/qDailyOvertime24HrParams.jsp";
    }

    public class ParametersResolved {
        public String hourSetDescription = null;
        public boolean addPremiumForFirstHourtypeToken = false;
        public String workDetailTimeCodes = null;
        public String eligibleHourTypes = null;
        public String premiumTimeCodesCounted = null;
        public String discountTimeCodes = null;
        public String premiumTimeCodeInserted = null;
        public String twentyFourHourStartTime = null;
        public String hourTypeForOvertimeWorkDetails = null;
        public String calcPeriod = null;
        public boolean assignBetterRate = false;

    }
}