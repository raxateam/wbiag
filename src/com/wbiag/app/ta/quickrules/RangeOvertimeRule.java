package com.wbiag.app.ta.quickrules;

import java.text.*;
import java.util.*;

import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.app.ta.rules.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;

/**
 * Overtime Rule that can be applied for range of
 * <ul>
 * <li>Week
 * <li>Pay Period
 * <li>Month
 * <li>Quarter
 * <li>Year
 * </ul>
 */
public class RangeOvertimeRule extends GenericOvertimeRule {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(RangeOvertimeRule.class);

    public final static String PARAM_HOURSET_DESCRIPTION = "HourSetDescription";
    public final static String PARAM_WORKDETAIL_TIMECODES = "WorkDetailTimeCodes";
    public final static String PARAM_ELIGIBLE_HOURTYPES = "EligibleHourTypes";
    public final static String PARAM_PREMIUM_TIMECODES_COUNTED = "PremiumTimeCodesCounted";
    public final static String PARAM_DISCOUNT_TIMECODES = "DiscountTimeCodes";
    public final static String PARAM_APPLY_ON_UNIT = "ApplyOnUnit";
    public final static String PARAM_APPLY_ON_VALUE_START = "ApplyOnValueStart";
    public final static String PARAM_APPLY_ON_VALUE_END = "ApplyOnValueEnd";
    public final static String PARAM_APPLY_BASED_ON_SCHEDULE = "ApplyBasedOnSchedule";
    public final static String PARAM_PREMIUM_TIMECODE_INSERTED = "PremiumTimeCodeInserted";
    public final static String PARAM_ASSIGN_BETTERRATE = "AssignBetterRate";
    public final static String PARAM_HOURTYPE_FOR_OVERTIME_WORKDETAILS = "HourTypeForOvertimeWorkDetails";

    public List getParameterInfo(DBConnection parm1) {
        List result = new ArrayList();

        result.add(new RuleParameterInfo(PARAM_HOURSET_DESCRIPTION, RuleParameterInfo.STRING_TYPE, false));
        result.add(new RuleParameterInfo(PARAM_WORKDETAIL_TIMECODES, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_ELIGIBLE_HOURTYPES, RuleParameterInfo.STRING_TYPE, false));
        result.add(new RuleParameterInfo(PARAM_PREMIUM_TIMECODES_COUNTED, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_DISCOUNT_TIMECODES, RuleParameterInfo.STRING_TYPE, true));

        RuleParameterInfo rpiApplyOnUnit = new RuleParameterInfo(PARAM_APPLY_ON_UNIT, RuleParameterInfo.CHOICE_TYPE, false);
        rpiApplyOnUnit.addChoice(DateHelper.APPLY_ON_UNIT_MONTH);
        rpiApplyOnUnit.addChoice(DateHelper.APPLY_ON_UNIT_PAYPERIOD);
        rpiApplyOnUnit.addChoice(DateHelper.APPLY_ON_UNIT_QTR);
        rpiApplyOnUnit.addChoice(DateHelper.APPLY_ON_UNIT_WEEK);
        rpiApplyOnUnit.addChoice(DateHelper.APPLY_ON_UNIT_YEAR);
        result.add(rpiApplyOnUnit);
        // *** could be DateHelper.APPLY_ON_FIRST/LAST_DAY or a number valid for applyOnUnit
        result.add(new RuleParameterInfo(PARAM_APPLY_ON_VALUE_START, RuleParameterInfo.STRING_TYPE, false));
        result.add(new RuleParameterInfo(PARAM_APPLY_ON_VALUE_END, RuleParameterInfo.STRING_TYPE, false));

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

        // Retrieve parameters
        String hourSetDescription = parameters.getParameter(PARAM_HOURSET_DESCRIPTION);
        String workDetailTimeCodes = parameters.getParameter(PARAM_WORKDETAIL_TIMECODES);
        String eligibleHourTypes = parameters.getParameter(PARAM_ELIGIBLE_HOURTYPES,   "REG");
        String premiumTimeCodesCounted = parameters.getParameter(PARAM_PREMIUM_TIMECODES_COUNTED, null);
        String discountTimeCodes = parameters.getParameter(PARAM_DISCOUNT_TIMECODES, null);
        String applyOnUnit = parameters.getParameter(PARAM_APPLY_ON_UNIT);
        if (StringHelper.isEmpty(applyOnUnit)) {
            throw new RuntimeException("Apply on unit cannot be empty");
        }
        String applyOnValueStart = parameters.getParameter(PARAM_APPLY_ON_VALUE_START);
        if (StringHelper.isEmpty(applyOnValueStart)) {
            throw new RuntimeException("Apply on value start cannot be empty");
        }
        String applyOnValueEnd = parameters.getParameter(PARAM_APPLY_ON_VALUE_END);
        if (StringHelper.isEmpty(applyOnValueEnd)) {
            throw new RuntimeException("Apply on value end cannot be empty");
        }

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
            ruleRangeOvertimeSchedule(wbData, hourSetDescription,
                                       applyOnUnit, applyOnValueStart, applyOnValueEnd,
                                       workDetailTimeCodes,
                                       premiumTimeCodesCounted,
                                       eligibleHourTypes, discountTimeCodes,
                                       premiumTimeCodeInserted ,
                                       hourTypeForOvertimeWorkDetails,
                                       assignBetterRate);
        }
        else {
            ruleRangeOvertime(wbData, hourSetDescription,
                               applyOnUnit, applyOnValueStart, applyOnValueEnd,
                               workDetailTimeCodes,
                               premiumTimeCodesCounted,
                               eligibleHourTypes, discountTimeCodes,
                               premiumTimeCodeInserted ,
                               hourTypeForOvertimeWorkDetails,
                               assignBetterRate);
        }
    }

    protected void ruleRangeOvertimeSchedule(WBData wbData,
            String hourSetDescription, String applyOnUnit,  String applyOnValueStart, String applyOnValueEnd,
            String workDetailTimeCodes, String premiumTimeCodesCounted, String eligibleHourTypes,
            String discountTimeCodes, String premiumTimeCodeInserted,
            String hourTypeForOvertimeWorkDetails,
            boolean assignBetterRate) throws Exception {

        // if off day, do as regular weekly ot
        if (!wbData.getRuleData().getEmployeeScheduleData().isEmployeeScheduledActual()) {
            ruleRangeOvertime(wbData, hourSetDescription, applyOnUnit, applyOnValueStart, applyOnValueEnd,
                               workDetailTimeCodes, premiumTimeCodesCounted,
                               eligibleHourTypes, discountTimeCodes,
                               premiumTimeCodeInserted ,
                               hourTypeForOvertimeWorkDetails,
                               assignBetterRate);
            return;
        }

        int seedMinutes = 0;
        Date workSummaryDate = wbData.getRuleData().getWorkSummary().getWrksWorkDate();
        Date dateStarts = getRangeDate(wbData , applyOnUnit , applyOnValueStart) ;
        Date dateEnds = getRangeDate(wbData , applyOnUnit , applyOnValueEnd) ;

        seedMinutes = wbData.getWorkedMinutes(wbData.getRuleData().getWorkSummary().getWrksId(),
                                              dateStarts,
                                              wbData.getWrksWorkDate(),
                                              null, null, false, null, eligibleHourTypes);

        // Include the work premium minutes if premiumTimeCodesCounted is provided
        if (premiumTimeCodesCounted != null) {
            seedMinutes += wbData.getMinutesWorkDetailPremiumRange(dateStarts,
                dateEnds, null, null,
                premiumTimeCodesCounted, true, eligibleHourTypes, true, "P" , false);
        }

        if (discountTimeCodes != null) {
            final Date THE_DISTANT_PAST = new java.util.GregorianCalendar(1900, 0, 1).getTime();
            final Date THE_DISTANT_FUTURE = new java.util.GregorianCalendar(3000, 0, 1).getTime();
            seedMinutes += wbData.getWorkedMinutes(wbData.getRuleData().getWorkSummary().getWrksId(),
                dateStarts,
                dateEnds,
                THE_DISTANT_PAST, THE_DISTANT_FUTURE, true, discountTimeCodes, null);
        }

        if (wbData.getRuleData().getWorkDetailCount() == 0) return;
        Date minStartTime = wbData.getRuleData().getWorkDetail(0).getWrkdStartTime();
        Date maxEndTime = wbData.getRuleData().getWorkDetail(wbData.getRuleData().getWorkDetailCount() - 1).getWrkdEndTime();

        // in shift
        Parameters parametersForGenericOvertimeRule = new Parameters();
        DateFormat dateFormat = new SimpleDateFormat(WBData.DATE_FORMAT_STRING);
        parametersForGenericOvertimeRule.addParameter(GenericOvertimeRule.PARAM_HOURSET_DESCRIPTION, hourSetDescription);
        parametersForGenericOvertimeRule.addParameter(GenericOvertimeRule.PARAM_ADDITIONAL_MINUTES_WORKED, Integer.toString(seedMinutes));
        parametersForGenericOvertimeRule.addParameter(GenericOvertimeRule.PARAM_STARTTIME_WITHIN_SHIFT, dateFormat.format(wbData.getRuleData().getEmployeeScheduleData().getEmpskdActStartTime()));
        parametersForGenericOvertimeRule.addParameter(GenericOvertimeRule.PARAM_ENDTIME_WITHIN_SHIFT, dateFormat.format(wbData.getRuleData().getEmployeeScheduleData().getEmpskdActEndTime()));
        parametersForGenericOvertimeRule.addParameter(GenericOvertimeRule.PARAM_ELIGIBLE_TIMECODES, workDetailTimeCodes);
        parametersForGenericOvertimeRule.addParameter(GenericOvertimeRule.PARAM_ARE_TIMECODES_INCLUSIVE, "true");
        parametersForGenericOvertimeRule.addParameter(GenericOvertimeRule.PARAM_ELIGIBLE_HOURTYPES, eligibleHourTypes);
        parametersForGenericOvertimeRule.addParameter(GenericOvertimeRule.PARAM_ADD_PREMIUMRECORD, "" + (premiumTimeCodeInserted != null));
        parametersForGenericOvertimeRule.addParameter(GenericOvertimeRule.PARAM_PREMIUM_TIMECODE, premiumTimeCodeInserted);
        parametersForGenericOvertimeRule.addParameter(GenericOvertimeRule.PARAM_SKDZONE_STARTDATE, dateFormat.format(dateStarts));
        parametersForGenericOvertimeRule.addParameter(GenericOvertimeRule.PARAM_SKDZONE_ENDDATE, dateFormat.format(dateEnds));
        parametersForGenericOvertimeRule.addParameter(GenericOvertimeRule.PARAM_HOURTYPE_FOR_OVERTIME_WORKDETAILS,
            hourTypeForOvertimeWorkDetails);
        parametersForGenericOvertimeRule.addParameter(super.PARAM_ASSIGN_BETTERRATE,
            String.valueOf(assignBetterRate));
        super.execute(wbData, parametersForGenericOvertimeRule);

        // before shift
        parametersForGenericOvertimeRule = new Parameters();
        parametersForGenericOvertimeRule.addParameter(GenericOvertimeRule.PARAM_HOURSET_DESCRIPTION, hourSetDescription);
        parametersForGenericOvertimeRule.addParameter(GenericOvertimeRule.PARAM_STARTTIME_WITHIN_SHIFT, dateFormat.format(minStartTime));
        parametersForGenericOvertimeRule.addParameter(GenericOvertimeRule.PARAM_ENDTIME_WITHIN_SHIFT, dateFormat.format(wbData.getRuleData().getEmployeeScheduleData().getEmpskdActStartTime()));
        parametersForGenericOvertimeRule.addParameter(GenericOvertimeRule.PARAM_INTERVAL_STARTTIME, dateFormat.format(wbData.getRuleData().getEmployeeScheduleData().getEmpskdActStartTime()));
        parametersForGenericOvertimeRule.addParameter(GenericOvertimeRule.PARAM_INTERVAL_ENDTIME, dateFormat.format(wbData.getRuleData().getEmployeeScheduleData().getEmpskdActEndTime()));
        parametersForGenericOvertimeRule.addParameter(GenericOvertimeRule.PARAM_ADDITIONAL_MINUTES_WORKED, Integer.toString(seedMinutes));
        parametersForGenericOvertimeRule.addParameter(GenericOvertimeRule.PARAM_ELIGIBLE_TIMECODES, workDetailTimeCodes);
        parametersForGenericOvertimeRule.addParameter(GenericOvertimeRule.PARAM_ARE_TIMECODES_INCLUSIVE, "true");
        parametersForGenericOvertimeRule.addParameter(GenericOvertimeRule.PARAM_ELIGIBLE_HOURTYPES , eligibleHourTypes);
        parametersForGenericOvertimeRule.addParameter(GenericOvertimeRule.PARAM_PREMIUM_TIMECODE, premiumTimeCodeInserted);
        parametersForGenericOvertimeRule.addParameter(GenericOvertimeRule.PARAM_SKDZONE_STARTDATE, dateFormat.format(dateStarts));
        parametersForGenericOvertimeRule.addParameter(GenericOvertimeRule.PARAM_SKDZONE_ENDDATE, dateFormat.format(dateEnds));
        parametersForGenericOvertimeRule.addParameter(GenericOvertimeRule.PARAM_HOURTYPE_FOR_OVERTIME_WORKDETAILS,
            hourTypeForOvertimeWorkDetails);
        parametersForGenericOvertimeRule.addParameter(super.PARAM_ASSIGN_BETTERRATE,
            String.valueOf(assignBetterRate));
        super.execute(wbData, parametersForGenericOvertimeRule);

        // after shift
        parametersForGenericOvertimeRule = new Parameters();
        parametersForGenericOvertimeRule.addParameter(GenericOvertimeRule.PARAM_HOURSET_DESCRIPTION, hourSetDescription);
        parametersForGenericOvertimeRule.addParameter(GenericOvertimeRule.PARAM_STARTTIME_WITHIN_SHIFT, dateFormat.format(wbData.getRuleData().getEmployeeScheduleData().getEmpskdActEndTime()));
        parametersForGenericOvertimeRule.addParameter(GenericOvertimeRule.PARAM_ENDTIME_WITHIN_SHIFT, dateFormat.format(maxEndTime));
        parametersForGenericOvertimeRule.addParameter(GenericOvertimeRule.PARAM_INTERVAL_STARTTIME, dateFormat.format(minStartTime));
        parametersForGenericOvertimeRule.addParameter(GenericOvertimeRule.PARAM_INTERVAL_ENDTIME, dateFormat.format(wbData.getRuleData().getEmployeeScheduleData().getEmpskdActEndTime()));
        parametersForGenericOvertimeRule.addParameter(GenericOvertimeRule.PARAM_ADDITIONAL_MINUTES_WORKED, Integer.toString(seedMinutes));
        parametersForGenericOvertimeRule.addParameter(GenericOvertimeRule.PARAM_ELIGIBLE_TIMECODES, workDetailTimeCodes);
        parametersForGenericOvertimeRule.addParameter(GenericOvertimeRule.PARAM_ARE_TIMECODES_INCLUSIVE, "true");
        parametersForGenericOvertimeRule.addParameter(GenericOvertimeRule.PARAM_ELIGIBLE_HOURTYPES, eligibleHourTypes);
        parametersForGenericOvertimeRule.addParameter(GenericOvertimeRule.PARAM_ADD_PREMIUMRECORD, "" + (premiumTimeCodeInserted != null));
        parametersForGenericOvertimeRule.addParameter(GenericOvertimeRule.PARAM_PREMIUM_TIMECODE, premiumTimeCodeInserted);
        parametersForGenericOvertimeRule.addParameter(GenericOvertimeRule.PARAM_SKDZONE_STARTDATE, dateFormat.format(dateStarts));
        parametersForGenericOvertimeRule.addParameter(GenericOvertimeRule.PARAM_SKDZONE_ENDDATE, dateFormat.format(dateEnds));
        parametersForGenericOvertimeRule.addParameter(GenericOvertimeRule.PARAM_HOURTYPE_FOR_OVERTIME_WORKDETAILS,
            hourTypeForOvertimeWorkDetails);
        parametersForGenericOvertimeRule.addParameter(super.PARAM_ASSIGN_BETTERRATE,
            String.valueOf(assignBetterRate));
        super.execute(wbData, parametersForGenericOvertimeRule);

        // applyRates();
    }

    protected void ruleRangeOvertime(WBData wbData, String hourSetDescription,
            String applyOnUnit, String applyOnValueStart,
            String applyOnValueEnd, String workDetailTimeCodes,
            String premiumTimeCodesCounted, String eligibleHourTypes,
            String discountTimeCodes, String premiumTimeCodeInserted,
            String hourTypeForOvertimeWorkDetails,
            boolean assignBetterRate) throws Exception {

        int seedMinutes = 0;
        Date workSummaryDate = wbData.getRuleData().getWorkSummary().getWrksWorkDate();
        Date dateStarts = getRangeDate(wbData , applyOnUnit , applyOnValueStart) ;
        Date dateEnds = getRangeDate(wbData , applyOnUnit , applyOnValueEnd) ;
        System.out.println(dateStarts + "-" + dateEnds);

        // Include the work premium minutes if premiumTimeCodesCounted is provided
        if (premiumTimeCodesCounted != null) {
            seedMinutes += wbData.getMinutesWorkDetailPremiumRange(dateStarts,
                dateEnds,
                null, null,
                premiumTimeCodesCounted, true, eligibleHourTypes, true, "P" , false);
        }

        if (discountTimeCodes != null) {
            final Date THE_DISTANT_PAST = new java.util.GregorianCalendar(1900, 0, 1).getTime();
            final Date THE_DISTANT_FUTURE = new java.util.GregorianCalendar(3000, 0, 1).getTime();
            seedMinutes += wbData.getWorkedMinutes(wbData.getRuleData().getWorkSummary().getWrksId(),
                dateStarts, dateEnds,
                THE_DISTANT_PAST, THE_DISTANT_FUTURE, true, discountTimeCodes, null);
        }

        Parameters parametersForGenericOvertimeRule = new Parameters();
        DateFormat dateFormat = new SimpleDateFormat(WBData.DATE_FORMAT_STRING);
        parametersForGenericOvertimeRule.addParameter("HourSetDescription", hourSetDescription);
        parametersForGenericOvertimeRule.addParameter("IntervalStartDate", dateFormat.format(dateStarts));
        parametersForGenericOvertimeRule.addParameter("IntervalEndDate", dateFormat.format(workSummaryDate));
        parametersForGenericOvertimeRule.addParameter("AdditionalMinutesWorked", Integer.toString(seedMinutes));
        parametersForGenericOvertimeRule.addParameter("EligibleTimeCodes", workDetailTimeCodes);
        parametersForGenericOvertimeRule.addParameter("AreTimeCodesInclusive", "true");
        parametersForGenericOvertimeRule.addParameter("EligibleHourTypes", eligibleHourTypes);
        parametersForGenericOvertimeRule.addParameter("AddPremiumRecord", "" + (premiumTimeCodeInserted != null));
        parametersForGenericOvertimeRule.addParameter("PremiumTimeCode", premiumTimeCodeInserted);
        parametersForGenericOvertimeRule.addParameter("SkdZoneStartDate", dateFormat.format(dateStarts));
        parametersForGenericOvertimeRule.addParameter("SkdZoneEndDate", dateFormat.format(dateEnds));
        parametersForGenericOvertimeRule.addParameter("HourTypeForOvertimeWorkDetails",
            hourTypeForOvertimeWorkDetails);
        parametersForGenericOvertimeRule.addParameter(super.PARAM_ASSIGN_BETTERRATE,
            String.valueOf(assignBetterRate));
        super.execute(wbData, parametersForGenericOvertimeRule);

        // applyRates();
    }

    protected Date getRangeDate(WBData wbData, String applyOnUnit , String applyOnValue) {
        Date ret = null;
        if (DateHelper.APPLY_ON_UNIT_MONTH.equals(applyOnUnit)) {
            ret = DateHelper.getUnitMonth(applyOnValue , false, wbData.getWrksWorkDate());
        }
        else if (DateHelper.APPLY_ON_UNIT_PAYPERIOD.equals(applyOnUnit)) {
            PayGroupData pgd = wbData.getRuleData().getCodeMapper().getPayGroupById(wbData.getPaygrpId());
            ret = DateHelper.getUnitPayPeriod(applyOnValue , false, wbData.getWrksWorkDate() , pgd);
        }
        else if (DateHelper.APPLY_ON_UNIT_QTR.equals(applyOnUnit)) {
            ret = DateHelper.getUnitQtr(applyOnValue , false, wbData.getWrksWorkDate());
        }
        else if (DateHelper.APPLY_ON_UNIT_YEAR.equals(applyOnUnit)) {
            ret = DateHelper.getUnitYear(applyOnValue , false, wbData.getWrksWorkDate());
        }
        else {
            throw new RuntimeException("ApplyonUnit not supported : " + applyOnUnit);
        }
        return ret;
    }

    public String getComponentName() {
        return "WBIAG: Range Overtime Rule";
    }

    public String getComponentUI() {
        return "/quickrules/qRangeOvertimeParams.jsp";
    }

}