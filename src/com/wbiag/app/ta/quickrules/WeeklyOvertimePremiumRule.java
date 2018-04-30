package com.wbiag.app.ta.quickrules;

import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import com.wbiag.util.StringHelper;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.app.ta.rules.GenericOvertimeRule;
import com.workbrain.sql.DBConnection;
import com.workbrain.util.*;


/**
 * WeeklyOvertimeRule that applies hourset to premium codes defined in PARAM_PREMIUM_TIMECODES_COUNTED
 *
 * Initially attempted to extend the core WeeklyOvertimeRule however, the following line:
 *
 * if (premiumTimeCodesCounted != null) {
 *     seedMinutes += wbData.getMinutesWorkDetailPremiumRange(dateWeekStarts, DateHelper.addDays(dateWeekStarts, 6), null, null, premiumTimeCodesCounted, true, eligibleHourTypes, true, "P" , false);
 * }
 *
 * Needed to be changed to:
 *
 * if (premiumTimeCodesCounted != null) {
 *     seedMinutes += wbData.getMinutesWorkDetailPremiumRange(dateWeekStarts, DateHelper.addDays(workSummaryDate, -1), null, null, premiumTimeCodesCounted, true, eligibleHourTypes, true, "P" , false);
 * }
 *
 * There was no way to do this by extending the class therefore, we needed to copy the code.
 *
 */
public class WeeklyOvertimePremiumRule extends GenericOvertimeRule {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(WeeklyOvertimePremiumRule.class);

    public final static String PARAM_HOURSET_DESCRIPTION = "HourSetDescription";
    public final static String PARAM_WORKDETAIL_TIMECODES = "WorkDetailTimeCodes";
    public final static String PARAM_ELIGIBLE_HOURTYPES = "EligibleHourTypes";
    public final static String PARAM_PREMIUM_TIMECODES_COUNTED = "PremiumTimeCodesCounted";
    public final static String PARAM_DISCOUNT_TIMECODES = "DiscountTimeCodes";
    public final static String PARAM_DAY_WEEK_STARTS = "DayWeekStarts";
    public final static String PARAM_APPLY_BASED_ON_SCHEDULE = "ApplyBasedOnSchedule";
    public final static String PARAM_PREMIUM_TIMECODE_INSERTED = "PremiumTimeCodeInserted";
    public final static String PARAM_ASSIGN_BETTERRATE = "AssignBetterRate";
    public final static String PARAM_HOURTYPE_FOR_OVERTIME_WORKDETAILS = "HourTypeForOvertimeWorkDetails";

    public String getComponentName() {
        return "WBIAG: Weekly Overtime Premium Rule";
    }

    public String getComponentUI() {
        return "/quickrules/qWeeklyOvertimeParams.jsp";
    }

    public List getParameterInfo(DBConnection parm1) {

    	List result = new ArrayList();

        result.add(new RuleParameterInfo(PARAM_HOURSET_DESCRIPTION, RuleParameterInfo.STRING_TYPE, false));
        result.add(new RuleParameterInfo(PARAM_WORKDETAIL_TIMECODES, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_ELIGIBLE_HOURTYPES, RuleParameterInfo.STRING_TYPE, false));
        result.add(new RuleParameterInfo(PARAM_PREMIUM_TIMECODES_COUNTED, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_DISCOUNT_TIMECODES, RuleParameterInfo.STRING_TYPE, true));

        RuleParameterInfo rpiDayWeekStartsChoice = new RuleParameterInfo(PARAM_DAY_WEEK_STARTS, RuleParameterInfo.CHOICE_TYPE, false);
        rpiDayWeekStartsChoice.addChoice("Sunday");
        rpiDayWeekStartsChoice.addChoice("Monday");
        rpiDayWeekStartsChoice.addChoice("Tuesday");
        rpiDayWeekStartsChoice.addChoice("Wednesday");
        rpiDayWeekStartsChoice.addChoice("Thursday");
        rpiDayWeekStartsChoice.addChoice("Friday");
        rpiDayWeekStartsChoice.addChoice("Saturday");
        result.add(rpiDayWeekStartsChoice);

        RuleParameterInfo rpiApplyBasedOnScheduleChoice = new RuleParameterInfo(PARAM_APPLY_BASED_ON_SCHEDULE, RuleParameterInfo.CHOICE_TYPE, true);
        rpiApplyBasedOnScheduleChoice.addChoice("true");
        rpiApplyBasedOnScheduleChoice.addChoice("false");
        result.add(rpiApplyBasedOnScheduleChoice);

        result.add(new RuleParameterInfo(PARAM_PREMIUM_TIMECODE_INSERTED, RuleParameterInfo.STRING_TYPE, true));

        RuleParameterInfo rpiAssignBetterRateChoice = new RuleParameterInfo(PARAM_ASSIGN_BETTERRATE, RuleParameterInfo.CHOICE_TYPE, true);
        rpiAssignBetterRateChoice.addChoice("true");
        rpiAssignBetterRateChoice.addChoice("false");
        result.add(rpiAssignBetterRateChoice);
        result.add(new RuleParameterInfo(PARAM_HOURTYPE_FOR_OVERTIME_WORKDETAILS, RuleParameterInfo.STRING_TYPE, true));

        return result;
    }


    public void execute(WBData wbData, Parameters parameters) throws java.lang.Exception {

    	String premiumTimeCodesAppliedTo = parameters.getParameter(PARAM_PREMIUM_TIMECODES_COUNTED, null);

    	// If not using the premium time codes parameter, no extra processing to do.
    	// In this case, just use the core WeeklyOvertimeRule.
    	if (premiumTimeCodesAppliedTo == null) {
    		executeWeeklyOvertimeRule(wbData, parameters);

    	} else {
    		// In order to apply the hourset to premiums, temporarily change the premiums to details.
    		changePremiumsToDetails(wbData , premiumTimeCodesAppliedTo);

    		// Execute the weeklyOvertimeRule.
    		executeWeeklyOvertimeRule(wbData, parameters);

    		// Change the Details back to Premiums.
    		resetDetailsToPremiums(wbData);
        }
    }

    /**
     * Copy of the core WeeklyOvertimeRule execute method.
     *
     * @param wbData
     * @param parameters
     * @throws java.lang.Exception
     */
    private void executeWeeklyOvertimeRule(WBData wbData, Parameters parameters) throws java.lang.Exception {

        // Retrieve parameters
        String hourSetDescription = parameters.getParameter(
            PARAM_HOURSET_DESCRIPTION);
        String workDetailTimeCodes = parameters.getParameter(
            PARAM_WORKDETAIL_TIMECODES);
        String eligibleHourTypes = parameters.getParameter(PARAM_ELIGIBLE_HOURTYPES,
            "REG");
        String premiumTimeCodesCounted = parameters.getParameter(
            PARAM_PREMIUM_TIMECODES_COUNTED, null);
        String discountTimeCodes = parameters.getParameter(PARAM_DISCOUNT_TIMECODES, null);
        String dayWeekStarts = parameters.getParameter(PARAM_DAY_WEEK_STARTS);
        boolean applyBasedOnSchedule = Boolean.valueOf(parameters.getParameter(
            PARAM_APPLY_BASED_ON_SCHEDULE, "false")).booleanValue();
        String premiumTimeCodeInserted = parameters.getParameter(
            PARAM_PREMIUM_TIMECODE_INSERTED, null);
        // assignBetterRate is a protected property inherited from GenericOvertimeRule
        boolean assignBetterRate = Boolean.valueOf(parameters.getParameter(
            PARAM_ASSIGN_BETTERRATE, "true")).booleanValue();
        String hourTypeForOvertimeWorkDetails = parameters.getParameter(
            PARAM_HOURTYPE_FOR_OVERTIME_WORKDETAILS, null);

        if (applyBasedOnSchedule) {
            ruleWeeklyOvertimeSchedule(wbData, hourSetDescription,
                                       dayWeekStarts, workDetailTimeCodes,
                                       premiumTimeCodesCounted,
                                       eligibleHourTypes, discountTimeCodes,
                                       premiumTimeCodeInserted ,
                                       hourTypeForOvertimeWorkDetails,
                                       assignBetterRate);
        }
        else {
            ruleWeeklyOvertime(wbData, hourSetDescription,
                               dayWeekStarts, workDetailTimeCodes,
                               premiumTimeCodesCounted,
                               eligibleHourTypes, discountTimeCodes,
                               premiumTimeCodeInserted ,
                               hourTypeForOvertimeWorkDetails,
                               assignBetterRate);
        }
    }

    protected void ruleWeeklyOvertimeSchedule(WBData wbData,
            String hourSetDescription, String dayWeekStarts,
            String workDetailTimeCodes, String premiumTimeCodesCounted,
			String eligibleHourTypes,
            String discountTimeCodes, String premiumTimeCodeInserted,
            String hourTypeForOvertimeWorkDetails,
            boolean assignBetterRate) throws Exception {

        // if off day, do as regular weekly ot
        if (!wbData.getRuleData().getEmployeeScheduleData().isEmployeeScheduledActual()) {
            ruleWeeklyOvertime(wbData, hourSetDescription, dayWeekStarts,
                               workDetailTimeCodes, premiumTimeCodesCounted,
                               eligibleHourTypes, discountTimeCodes,
                               premiumTimeCodeInserted ,
                               hourTypeForOvertimeWorkDetails,
                               assignBetterRate);
            return;
        }

        int seedMinutes = 0;
        Date workSummaryDate = wbData.getRuleData().getWorkSummary().getWrksWorkDate();
        Date dateWeekStarts = DateHelper.nextDay(DateHelper.addDays(workSummaryDate, -7), dayWeekStarts);

        seedMinutes = wbData.getWorkedMinutes(wbData.getRuleData().getWorkSummary().getWrksId(), dateWeekStarts, wbData.getRuleData().getWorkSummary().getWrksWorkDate(), null, null, false, null, eligibleHourTypes);

        // Include the work premium minutes if premiumTimeCodesCounted is provided
         /*
         * These next 2 IF statements are the only difference from the core WeeklyOvertimeRule.
         */
        if (premiumTimeCodesCounted != null) {
        	// If a tcode is in the premium list and the discount list we only want to count it
        	// once.  So here do not count a tcode if it is also in the discount list.
        	String premiumSubList = getPremiumListForSeed(premiumTimeCodesCounted, discountTimeCodes);

        	if (premiumSubList != null && premiumSubList.length() > 0) {
        		seedMinutes += getPremiumDiscountMinutes(wbData, premiumSubList, eligibleHourTypes, workSummaryDate, dateWeekStarts);
        	}
        }

        if (discountTimeCodes != null) {
            seedMinutes += wbData.getMinutesWorkDetailPremiumRange(
            							dateWeekStarts, DateHelper.addDays(dateWeekStarts, 6), null, null,
            							discountTimeCodes, true, null, true,
										null);
        }

        if (wbData.getRuleData().getWorkDetailCount() == 0) return;
        Date minStartTime = wbData.getRuleData().getWorkDetail(0).getWrkdStartTime();
        Date maxEndTime = wbData.getRuleData().getWorkDetail(wbData.getRuleData().getWorkDetailCount() - 1).getWrkdEndTime();

        // in shift
        Parameters parametersForGenericOvertimeRule = new Parameters();
        DateFormat dateFormat = new SimpleDateFormat(WBData.DATE_FORMAT_STRING);
        parametersForGenericOvertimeRule.addParameter(super.PARAM_HOURSET_DESCRIPTION, hourSetDescription);
        parametersForGenericOvertimeRule.addParameter(super.PARAM_ADDITIONAL_MINUTES_WORKED, Integer.toString(seedMinutes));
        parametersForGenericOvertimeRule.addParameter(super.PARAM_STARTTIME_WITHIN_SHIFT, dateFormat.format(wbData.getRuleData().getEmployeeScheduleData().getEmpskdActStartTime()));
        parametersForGenericOvertimeRule.addParameter(super.PARAM_ENDTIME_WITHIN_SHIFT, dateFormat.format(wbData.getRuleData().getEmployeeScheduleData().getEmpskdActEndTime()));
        parametersForGenericOvertimeRule.addParameter(super.PARAM_ELIGIBLE_TIMECODES, workDetailTimeCodes);
        parametersForGenericOvertimeRule.addParameter(super.PARAM_ARE_TIMECODES_INCLUSIVE, "true");
        parametersForGenericOvertimeRule.addParameter(super.PARAM_ELIGIBLE_HOURTYPES, eligibleHourTypes);
        parametersForGenericOvertimeRule.addParameter(super.PARAM_ADD_PREMIUMRECORD, "" + (premiumTimeCodeInserted != null));
        parametersForGenericOvertimeRule.addParameter(super.PARAM_PREMIUM_TIMECODE, premiumTimeCodeInserted);
        parametersForGenericOvertimeRule.addParameter(super.PARAM_SKDZONE_STARTDATE, dateFormat.format(dateWeekStarts));
        parametersForGenericOvertimeRule.addParameter(super.PARAM_SKDZONE_ENDDATE, dateFormat.format(DateHelper.addDays(dateWeekStarts , 6)));
        parametersForGenericOvertimeRule.addParameter(super.PARAM_HOURTYPE_FOR_OVERTIME_WORKDETAILS,
            hourTypeForOvertimeWorkDetails);
        parametersForGenericOvertimeRule.addParameter(super.PARAM_ASSIGN_BETTERRATE,
            String.valueOf(assignBetterRate));

        super.execute(wbData, parametersForGenericOvertimeRule);

        // before shift
        parametersForGenericOvertimeRule = new Parameters();
        parametersForGenericOvertimeRule.addParameter(super.PARAM_HOURSET_DESCRIPTION, hourSetDescription);
        parametersForGenericOvertimeRule.addParameter(super.PARAM_STARTTIME_WITHIN_SHIFT, dateFormat.format(minStartTime));
        parametersForGenericOvertimeRule.addParameter(super.PARAM_ENDTIME_WITHIN_SHIFT, dateFormat.format(wbData.getRuleData().getEmployeeScheduleData().getEmpskdActStartTime()));
        parametersForGenericOvertimeRule.addParameter(super.PARAM_INTERVAL_STARTTIME, dateFormat.format(wbData.getRuleData().getEmployeeScheduleData().getEmpskdActStartTime()));
        parametersForGenericOvertimeRule.addParameter(super.PARAM_INTERVAL_ENDTIME, dateFormat.format(wbData.getRuleData().getEmployeeScheduleData().getEmpskdActEndTime()));
        parametersForGenericOvertimeRule.addParameter(super.PARAM_ADDITIONAL_MINUTES_WORKED, Integer.toString(seedMinutes));
        parametersForGenericOvertimeRule.addParameter(super.PARAM_ELIGIBLE_TIMECODES, workDetailTimeCodes);
        parametersForGenericOvertimeRule.addParameter(super.PARAM_ARE_TIMECODES_INCLUSIVE, "true");
        parametersForGenericOvertimeRule.addParameter(super.PARAM_ELIGIBLE_HOURTYPES, eligibleHourTypes);
        parametersForGenericOvertimeRule.addParameter(super.PARAM_PREMIUM_TIMECODE, premiumTimeCodeInserted);
        parametersForGenericOvertimeRule.addParameter(super.PARAM_SKDZONE_STARTDATE, dateFormat.format(dateWeekStarts));
        parametersForGenericOvertimeRule.addParameter(super.PARAM_SKDZONE_ENDDATE, dateFormat.format(DateHelper.addDays(dateWeekStarts , 6)));
        parametersForGenericOvertimeRule.addParameter(super.PARAM_HOURTYPE_FOR_OVERTIME_WORKDETAILS,
            hourTypeForOvertimeWorkDetails);
        parametersForGenericOvertimeRule.addParameter(super.PARAM_ASSIGN_BETTERRATE,
            String.valueOf(assignBetterRate));
        super.execute(wbData, parametersForGenericOvertimeRule);

        // after shift
        parametersForGenericOvertimeRule = new Parameters();
        parametersForGenericOvertimeRule.addParameter(super.PARAM_HOURSET_DESCRIPTION, hourSetDescription);
        parametersForGenericOvertimeRule.addParameter(super.PARAM_STARTTIME_WITHIN_SHIFT, dateFormat.format(wbData.getRuleData().getEmployeeScheduleData().getEmpskdActEndTime()));
        parametersForGenericOvertimeRule.addParameter(super.PARAM_ENDTIME_WITHIN_SHIFT, dateFormat.format(maxEndTime));
        parametersForGenericOvertimeRule.addParameter(super.PARAM_INTERVAL_STARTTIME, dateFormat.format(minStartTime));
        parametersForGenericOvertimeRule.addParameter(super.PARAM_INTERVAL_ENDTIME, dateFormat.format(wbData.getRuleData().getEmployeeScheduleData().getEmpskdActEndTime()));
        parametersForGenericOvertimeRule.addParameter(super.PARAM_ADDITIONAL_MINUTES_WORKED, Integer.toString(seedMinutes));
        parametersForGenericOvertimeRule.addParameter(super.PARAM_ELIGIBLE_TIMECODES, workDetailTimeCodes);
        parametersForGenericOvertimeRule.addParameter(super.PARAM_ARE_TIMECODES_INCLUSIVE, "true");
        parametersForGenericOvertimeRule.addParameter(super.PARAM_ELIGIBLE_HOURTYPES, eligibleHourTypes);
        parametersForGenericOvertimeRule.addParameter(super.PARAM_ADD_PREMIUMRECORD, "" + (premiumTimeCodeInserted != null));
        parametersForGenericOvertimeRule.addParameter(super.PARAM_PREMIUM_TIMECODE, premiumTimeCodeInserted);
        parametersForGenericOvertimeRule.addParameter(super.PARAM_SKDZONE_STARTDATE, dateFormat.format(dateWeekStarts));
        parametersForGenericOvertimeRule.addParameter(super.PARAM_SKDZONE_ENDDATE, dateFormat.format(DateHelper.addDays(dateWeekStarts , 6)));
        parametersForGenericOvertimeRule.addParameter(super.PARAM_HOURTYPE_FOR_OVERTIME_WORKDETAILS,
            hourTypeForOvertimeWorkDetails);
        parametersForGenericOvertimeRule.addParameter(super.PARAM_ASSIGN_BETTERRATE,
            String.valueOf(assignBetterRate));

        super.execute(wbData, parametersForGenericOvertimeRule);
    }

    protected void ruleWeeklyOvertime(WBData wbData, String hourSetDescription,
            String dayWeekStarts, String workDetailTimeCodes,
            String premiumTimeCodesCounted, String eligibleHourTypes,
            String discountTimeCodes, String premiumTimeCodeInserted,
            String hourTypeForOvertimeWorkDetails,
            boolean assignBetterRate) throws Exception {

        int seedMinutes = 0;
        Date workSummaryDate = wbData.getRuleData().getWorkSummary().getWrksWorkDate();
        Date dateWeekStarts = DateHelper.nextDay(DateHelper.addDays(workSummaryDate, -7), dayWeekStarts);

        // Include the work premium minutes if premiumTimeCodesCounted is provided
        /*
         * These next 2 IF statements are the only difference from the core WeeklyOvertimeRule.
         */
        if (premiumTimeCodesCounted != null) {
        	// If a tcode is in the premium list and the discount list we only want to count it
        	// once.  So here do not count a tcode if it is also in the discount list.
        	String premiumSubList = getPremiumListForSeed(premiumTimeCodesCounted, discountTimeCodes);

        	if (premiumSubList != null && premiumSubList.length() > 0) {
        		seedMinutes += getPremiumDiscountMinutes(wbData, premiumSubList, eligibleHourTypes, workSummaryDate, dateWeekStarts);
        	}
        }

        if (discountTimeCodes != null) {
            seedMinutes += wbData.getMinutesWorkDetailPremiumRange(
            							dateWeekStarts, DateHelper.addDays(dateWeekStarts, 6), null, null,
            							discountTimeCodes, true, null, true,
										null);
        }

        Parameters parametersForGenericOvertimeRule = new Parameters();
        DateFormat dateFormat = new SimpleDateFormat(WBData.DATE_FORMAT_STRING);
        parametersForGenericOvertimeRule.addParameter(super.PARAM_HOURSET_DESCRIPTION, hourSetDescription);
        parametersForGenericOvertimeRule.addParameter(super.PARAM_INTERVAL_STARTDATE, dateFormat.format(dateWeekStarts));
        parametersForGenericOvertimeRule.addParameter(super.PARAM_INTERVAL_ENDDATE, dateFormat.format(workSummaryDate));
        parametersForGenericOvertimeRule.addParameter(super.PARAM_ADDITIONAL_MINUTES_WORKED, Integer.toString(seedMinutes));
        parametersForGenericOvertimeRule.addParameter(super.PARAM_ELIGIBLE_TIMECODES, workDetailTimeCodes);
        parametersForGenericOvertimeRule.addParameter(super.PARAM_ARE_TIMECODES_INCLUSIVE, "true");
        parametersForGenericOvertimeRule.addParameter(super.PARAM_ELIGIBLE_HOURTYPES, eligibleHourTypes);
        parametersForGenericOvertimeRule.addParameter(super.PARAM_ADD_PREMIUMRECORD, "" + (premiumTimeCodeInserted != null));
        parametersForGenericOvertimeRule.addParameter(super.PARAM_PREMIUM_TIMECODE, premiumTimeCodeInserted);
        parametersForGenericOvertimeRule.addParameter(super.PARAM_SKDZONE_STARTDATE, dateFormat.format(dateWeekStarts));
        parametersForGenericOvertimeRule.addParameter(super.PARAM_SKDZONE_ENDDATE, dateFormat.format(DateHelper.addDays(dateWeekStarts , 6)));
        parametersForGenericOvertimeRule.addParameter(super.PARAM_HOURTYPE_FOR_OVERTIME_WORKDETAILS,
            hourTypeForOvertimeWorkDetails);
        parametersForGenericOvertimeRule.addParameter(super.PARAM_ASSIGN_BETTERRATE,
            String.valueOf(assignBetterRate));
        super.execute(wbData, parametersForGenericOvertimeRule);

    }

    /**
     * Returns the number of minutes that should be discounted due to premiums.
     * For this rule, we only want to discount premium minutes for any dates from the begin of
     * week to the wrkdDate.  Premiums from the wrksDate to the end of week haven't occured yet
     * so we don't want to include them in the hourset.
     *
	 * @param wbData
	 * @param premiumTimeCodesCounted
	 * @param eligibleHourTypes
	 * @param workSummaryDate
	 * @param dateWeekStarts
	 * @return
	 * @throws SQLException
	 */
	protected int getPremiumDiscountMinutes(WBData wbData, String premiumTimeCodesCounted, String eligibleHourTypes, Date workSummaryDate, Date dateWeekStarts) throws SQLException {

		int premiumDiscount = 0;

		if (!workSummaryDate.equals(dateWeekStarts)) {
			premiumDiscount = wbData.getMinutesWorkDetailPremiumRange(dateWeekStarts, DateHelper.addDays(workSummaryDate, -1), null, null, premiumTimeCodesCounted, true, eligibleHourTypes, true, "P" , false);
		}

		return premiumDiscount;
	}

	/**
     * Converts Premiums to Work Details and sets the override flag.
     *
     * @param wbData
     * @param premiumTimeCodesAppliedTo
     */
    private void changePremiumsToDetails(WBData wbData , String premiumTimeCodesAppliedTo) {

    	for (int i = wbData.getRuleData().getWorkPremiums().size() - 1; i >= 0; i--) {

    		WorkDetailData curWorkPrem = wbData.getRuleData().getWorkPremium(i).duplicate();

            if (RuleHelper.isCodeInList(premiumTimeCodesAppliedTo, curWorkPrem.getWrkdTcodeName())) {

            	Date max = wbData.getRuleData().getWorkDetails().getMaxEndDate();

            	if (max == null) {
                    max = wbData.getWrksWorkDate();
                }

            	curWorkPrem.setWrkdStartTime(max);
                curWorkPrem.setWrkdEndTime(DateHelper.addMinutes(max,curWorkPrem.getWrkdMinutes()));
                curWorkPrem.setWrkdType(WorkDetailData.DETAIL_TYPE);
                curWorkPrem.setWrkdOverridden("Y");

                wbData.getRuleData().getWorkDetails().add(curWorkPrem);
                wbData.getRuleData().getWorkPremiums().remove(i);
            }
        }
    }

    /**
     * Converts details that have been marked with the overrride flag, back to premiums.
     *
     * @param wbData
     */
    private void resetDetailsToPremiums(WBData wbData) {

        for (int i = wbData.getRuleData().getWorkDetails().size() -1 ;i >= 0; i--) {

        	WorkDetailData curWorkDet = wbData.getRuleData().getWorkDetail(i);

            if ("Y".equals(curWorkDet.getWrkdOverridden())) {

            	curWorkDet = curWorkDet.duplicate();
                curWorkDet.setWrkdType(WorkDetailData.PREMIUM_TYPE);
                curWorkDet.setWrkdStartTime(DateHelper.DATE_1900);
                curWorkDet.setWrkdEndTime(DateHelper.DATE_1900);
                curWorkDet.setWrkdOverridden(null);

                wbData.getRuleData().getWorkPremiums().add(curWorkDet);
                wbData.getRuleData().getWorkDetails().remove(i);
            }
        }
    }

    private String getPremiumListForSeed(String premiumTCodes, String discountTCodes) {
    	return StringHelper.getSublist(premiumTCodes, discountTCodes, ",");
    }
}