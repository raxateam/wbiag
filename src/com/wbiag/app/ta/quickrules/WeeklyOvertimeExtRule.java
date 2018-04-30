package com.wbiag.app.ta.quickrules;

// standard imports for rules
import com.workbrain.app.ta.ruleengine.Parameters;
import com.workbrain.app.ta.ruleengine.RuleParameterInfo;
import com.workbrain.app.ta.ruleengine.WBData;
import com.workbrain.sql.DBConnection;
import java.util.ArrayList;
import java.util.List;

// extra imports needed for this rule
import com.workbrain.app.ta.rules.GenericOvertimeRule;
import com.workbrain.util.DateHelper;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;


public class WeeklyOvertimeExtRule extends GenericOvertimeExtRule {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(WeeklyOvertimeExtRule.class);

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
    public final static String PARAM_VAL_APPLY_BASED_ON_SCHEDULE_EXT_ED = "EXT_EVERY_DAY";
    /**************************
     * DONOT PUT INSTANCE VARIABLES FOR QUICKRULES/RULES
     * ALL CLASS VARIABLES SHOULD BE STATIC and FINAL
     **************************/

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
        rpiApplyBasedOnScheduleChoice.addChoice(PARAM_VAL_APPLY_BASED_ON_SCHEDULE_EXT_ED);
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
        String sApplyBasedOnSchedule = parameters.getParameter(PARAM_APPLY_BASED_ON_SCHEDULE, "false");
        boolean applyBasedOnSchedule = false, applyBasedOnScheduleExtEveryDay = false;
        if ("true".equalsIgnoreCase(sApplyBasedOnSchedule)
            || "false".equalsIgnoreCase(sApplyBasedOnSchedule)) {
            applyBasedOnSchedule = Boolean.valueOf(
                sApplyBasedOnSchedule).booleanValue();
        }
        else if (PARAM_VAL_APPLY_BASED_ON_SCHEDULE_EXT_ED.equals(sApplyBasedOnSchedule)) {
            applyBasedOnScheduleExtEveryDay = true;

        }
        String premiumTimeCodeInserted = parameters.getParameter(
            PARAM_PREMIUM_TIMECODE_INSERTED, null);
        // assignBetterRate is to be passed to GenericOvertimeRule
        boolean assignBetterRate = Boolean.valueOf(parameters.getParameter(
            PARAM_ASSIGN_BETTERRATE, "true")).booleanValue();
        String hourTypeForOvertimeWorkDetails = parameters.getParameter(
            PARAM_HOURTYPE_FOR_OVERTIME_WORKDETAILS, null);

        if (applyBasedOnSchedule) {
            ruleWeeklyOvertimeSchedule(wbData, hourSetDescription,
                                       dayWeekStarts, workDetailTimeCodes,
                                       premiumTimeCodesCounted,
                                       eligibleHourTypes, discountTimeCodes,
                                       premiumTimeCodeInserted , hourTypeForOvertimeWorkDetails, assignBetterRate);
        }
        else if (applyBasedOnScheduleExtEveryDay) {
            ruleWeeklyOvertimeScheduleExtEveryDay(wbData, hourSetDescription,
                                       dayWeekStarts, workDetailTimeCodes,
                                       premiumTimeCodesCounted,
                                       eligibleHourTypes, discountTimeCodes,
                                       premiumTimeCodeInserted , hourTypeForOvertimeWorkDetails, assignBetterRate);
        }
        else {
            ruleWeeklyOvertime(wbData, hourSetDescription,
                               dayWeekStarts, workDetailTimeCodes,
                               premiumTimeCodesCounted,
                               eligibleHourTypes, discountTimeCodes,
                               premiumTimeCodeInserted , hourTypeForOvertimeWorkDetails, assignBetterRate);
        }
    }

    protected void ruleWeeklyOvertimeSchedule(WBData wbData,
                                              String hourSetDescription, String dayWeekStarts,
                                              String workDetailTimeCodes, String premiumTimeCodesCounted, String eligibleHourTypes,
                                              String discountTimeCodes, String premiumTimeCodeInserted,
                                              String hourTypeForOvertimeWorkDetails, boolean assignBetterRate) throws Exception {

        // if off day, do as regular weekly ot
        if (!wbData.getRuleData().getEmployeeScheduleData().isEmployeeScheduledActual()) {
            ruleWeeklyOvertime(wbData, hourSetDescription, dayWeekStarts,
                               workDetailTimeCodes, premiumTimeCodesCounted,
                               eligibleHourTypes, discountTimeCodes,
                               premiumTimeCodeInserted , hourTypeForOvertimeWorkDetails, assignBetterRate);
            return;
        }

        int seedMinutes = 0;
        Date workSummaryDate = wbData.getRuleData().getWorkSummary().getWrksWorkDate();
        Date dateWeekStarts = DateHelper.nextDay(DateHelper.addDays(workSummaryDate, -7), dayWeekStarts);

        seedMinutes = wbData.getWorkedMinutes(wbData.getRuleData().getWorkSummary().getWrksId(), dateWeekStarts, wbData.getRuleData().getWorkSummary().getWrksWorkDate(), null, null, false, null, eligibleHourTypes);

        // Include the work premium minutes if premiumTimeCodesCounted is provided
        if (premiumTimeCodesCounted != null) {
            seedMinutes += wbData.getMinutesWorkDetailPremiumRange(dateWeekStarts, DateHelper.addDays(dateWeekStarts, 6), null, null, premiumTimeCodesCounted, true, eligibleHourTypes, true, "P" , false);
        }

        if (discountTimeCodes != null) {
            final Date THE_DISTANT_PAST = new java.util.GregorianCalendar(1900, 0, 1).getTime();
            final Date THE_DISTANT_FUTURE = new java.util.GregorianCalendar(3000, 0, 1).getTime();
            seedMinutes += wbData.getWorkedMinutes(wbData.getRuleData().getWorkSummary().getWrksId(), dateWeekStarts, DateHelper.addDays(dateWeekStarts, 6), THE_DISTANT_PAST, THE_DISTANT_FUTURE, true, discountTimeCodes, null);
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
        parametersForGenericOvertimeRule.addParameter(super.PARAM_ASSIGN_BETTERRATE, String.valueOf(assignBetterRate));
        parametersForGenericOvertimeRule.addParameter(super.PARAM_INTERVAL_STARTTIME, null);
        parametersForGenericOvertimeRule.addParameter(super.PARAM_INTERVAL_ENDTIME, null);
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
        parametersForGenericOvertimeRule.addParameter(super.PARAM_ASSIGN_BETTERRATE, String.valueOf(assignBetterRate));

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
        parametersForGenericOvertimeRule.addParameter(super.PARAM_ASSIGN_BETTERRATE, String.valueOf(assignBetterRate));

        super.execute(wbData, parametersForGenericOvertimeRule);

        // applyRates();
    }

    /**
     * Extended weekly OT based on schedule
     * Applies OT- on every day schedule. Needs auto recalc or manual recalc of whoe week for accuracy
     */
    protected void ruleWeeklyOvertimeScheduleExtEveryDay(WBData wbData,
                                              String hourSetDescription, String dayWeekStarts,
                                              String workDetailTimeCodes, String premiumTimeCodesCounted, String eligibleHourTypes,
                                              String discountTimeCodes, String premiumTimeCodeInserted,
                                              String hourTypeForOvertimeWorkDetails, boolean assignBetterRate) throws Exception {

        // if off day, do as regular weekly ot
        // *** off days also count as unscheduled time
        /*
        if (!wbData.getRuleData().getEmployeeScheduleData().isEmployeeScheduledActual()) {
            ruleWeeklyOvertime(wbData, hourSetDescription, dayWeekStarts,
                               workDetailTimeCodes, premiumTimeCodesCounted,
                               eligibleHourTypes, discountTimeCodes,
                               premiumTimeCodeInserted , hourTypeForOvertimeWorkDetails,
                               assignBetterRate
                               );
            return;
        }
        */
        int premiumMinutes = 0, discountMinutes = 0;
        Date workSummaryDate = wbData.getRuleData().getWorkSummary().getWrksWorkDate();
        Date dateWeekStarts = DateHelper.nextDay(DateHelper.addDays(workSummaryDate, -7), dayWeekStarts);
        Date dateWeekEnds = DateHelper.addDays(dateWeekStarts, 6);

        Parameters parametersForGenericOvertimeRule = new Parameters();
        DateFormat dateFormat = new SimpleDateFormat(WBData.DATE_FORMAT_STRING);

        // Include the work premium minutes if premiumTimeCodesCounted is provided
        if (premiumTimeCodesCounted != null) {
            premiumMinutes = wbData.getMinutesWorkDetailPremiumRange(dateWeekStarts, dateWeekEnds, null, null, premiumTimeCodesCounted, true, eligibleHourTypes, true, "P" , false);
        }

        if (discountTimeCodes != null) {
            final Date THE_DISTANT_PAST = new java.util.GregorianCalendar(1900, 0, 1).getTime();
            final Date THE_DISTANT_FUTURE = new java.util.GregorianCalendar(3000, 0, 1).getTime();
            discountMinutes = wbData.getWorkedMinutes(wbData.getRuleData().getWorkSummary().getWrksId(), dateWeekStarts, dateWeekEnds, THE_DISTANT_PAST, THE_DISTANT_FUTURE, true, discountTimeCodes, null);
        }

        if (wbData.getRuleData().getWorkDetailCount() == 0) return;
        Date minStartTime = wbData.getRuleData().getWorkDetail(0).getWrkdStartTime();
        Date maxEndTime = wbData.getRuleData().getWorkDetail(wbData.getRuleData().getWorkDetailCount() - 1).getWrkdEndTime();


        int minutesInsideScheduleRange[] = new int[7];	//array to hold the entire week's scheduled minutes
        int minutesWorkDetailRange[] = new int[7];		//array to hold the entire week's worked minutes
        int seedMinutesInShift[] = new int[7];			//array to hold the entire week's scheduled shift seeds
        int count = 0;
        int totalMinutesInsideScheduleRange = 0;
        int totalMinutesWorkDetailRange = 0;
        int todayIndex = 0;

        for (Date date = dateWeekStarts; date.compareTo(dateWeekEnds) <= 0;date = DateHelper.addDays(date, 1)) {

            minutesInsideScheduleRange[count] = wbData.
                getMinutesWorkDetailRange(date, date,
                                          wbData.getEmployeeScheduleData(date).getEmpskdActStartTime(),
                                          wbData.getEmployeeScheduleData(date).getEmpskdActEndTime(),
                                          workDetailTimeCodes, true,
                                          eligibleHourTypes, true);
            minutesWorkDetailRange[count] = wbData.
                getMinutesWorkDetailRange(date, date, null, null,
                                          workDetailTimeCodes, true,
                                          eligibleHourTypes, true);
            totalMinutesInsideScheduleRange += minutesInsideScheduleRange[count];
            totalMinutesWorkDetailRange += minutesWorkDetailRange[count];

            if (count == 0)
                seedMinutesInShift[count] = premiumMinutes + discountMinutes;
            else
                seedMinutesInShift[count] = seedMinutesInShift[count-1] + minutesInsideScheduleRange[count-1];


            //figure out the current workday index in the week
            if (date.compareTo(workSummaryDate) == 0)
                todayIndex = count;

            count++;
        }

        int minutesOutsideOfShiftUsed = 0;
        int seedBefore = premiumMinutes + discountMinutes;
        int seedAfter = premiumMinutes + discountMinutes;
        //allocate the seed minutes of TODAY's before and after shifts appropriately
        for (int i=0;i<7;i++){
            if (i < todayIndex){
                minutesOutsideOfShiftUsed += minutesWorkDetailRange[i] - minutesInsideScheduleRange[i];
            }
            else if (i == todayIndex) {
                seedBefore += totalMinutesInsideScheduleRange + minutesOutsideOfShiftUsed;
                seedAfter += totalMinutesInsideScheduleRange + minutesOutsideOfShiftUsed +
                    wbData.getMinutesWorkDetail(minStartTime, wbData.getRuleData().getEmployeeScheduleData().getEmpskdActStartTime(), workDetailTimeCodes, true, eligibleHourTypes, true, null);
            }
        }

        // in shift
        if (wbData.getRuleData().getEmployeeScheduleData().isEmployeeScheduledActual()) {
            parametersForGenericOvertimeRule.addParameter("HourSetDescription",
                hourSetDescription);
            parametersForGenericOvertimeRule.addParameter(
                "AdditionalMinutesWorked",
                String.valueOf(seedMinutesInShift[todayIndex]));
            parametersForGenericOvertimeRule.addParameter(
                "StartTimeWithinShift",
                dateFormat.format(wbData.getRuleData().getEmployeeScheduleData().
                                  getEmpskdActStartTime()));
            parametersForGenericOvertimeRule.addParameter("EndTimeWithinShift",
                dateFormat.format(wbData.getRuleData().getEmployeeScheduleData().
                                  getEmpskdActEndTime()));
            parametersForGenericOvertimeRule.addParameter("EligibleTimeCodes",
                workDetailTimeCodes);
            parametersForGenericOvertimeRule.addParameter(
                "AreTimeCodesInclusive", "true");
            parametersForGenericOvertimeRule.addParameter("EligibleHourTypes",
                eligibleHourTypes);
            parametersForGenericOvertimeRule.addParameter("AddPremiumRecord",
                "" + (premiumTimeCodeInserted != null));
            parametersForGenericOvertimeRule.addParameter("PremiumTimeCode",
                premiumTimeCodeInserted);
            parametersForGenericOvertimeRule.addParameter("SkdZoneStartDate",
                dateFormat.format(dateWeekStarts));
            parametersForGenericOvertimeRule.addParameter("SkdZoneEndDate",
                dateFormat.format(DateHelper.addDays(dateWeekStarts, 6)));
            parametersForGenericOvertimeRule.addParameter(
                "HourTypeForOvertimeWorkDetails",
                hourTypeForOvertimeWorkDetails);
            super.execute(wbData, parametersForGenericOvertimeRule);

            // before shift
            parametersForGenericOvertimeRule = new Parameters();
            parametersForGenericOvertimeRule.addParameter("HourSetDescription",
                hourSetDescription);
            parametersForGenericOvertimeRule.addParameter(
                "StartTimeWithinShift", dateFormat.format(minStartTime));
            parametersForGenericOvertimeRule.addParameter("EndTimeWithinShift",
                dateFormat.format(wbData.getRuleData().getEmployeeScheduleData().
                                  getEmpskdActStartTime()));
            parametersForGenericOvertimeRule.addParameter(
                "AdditionalMinutesWorked", String.valueOf(seedBefore));
            parametersForGenericOvertimeRule.addParameter("EligibleTimeCodes",
                workDetailTimeCodes);
            parametersForGenericOvertimeRule.addParameter(
                "AreTimeCodesInclusive", "true");
            parametersForGenericOvertimeRule.addParameter("EligibleHourTypes",
                eligibleHourTypes);
            parametersForGenericOvertimeRule.addParameter("PremiumTimeCode",
                premiumTimeCodeInserted);
            parametersForGenericOvertimeRule.addParameter("SkdZoneStartDate",
                dateFormat.format(dateWeekStarts));
            parametersForGenericOvertimeRule.addParameter("SkdZoneEndDate",
                dateFormat.format(dateWeekEnds));
            parametersForGenericOvertimeRule.addParameter(
                "HourTypeForOvertimeWorkDetails",
                hourTypeForOvertimeWorkDetails);
            super.execute(wbData, parametersForGenericOvertimeRule);

            // after shift
            parametersForGenericOvertimeRule = new Parameters();
            parametersForGenericOvertimeRule.addParameter("HourSetDescription",
                hourSetDescription);
            parametersForGenericOvertimeRule.addParameter(
                "StartTimeWithinShift",
                dateFormat.format(wbData.getRuleData().getEmployeeScheduleData().
                                  getEmpskdActEndTime()));
            parametersForGenericOvertimeRule.addParameter("EndTimeWithinShift",
                dateFormat.format(maxEndTime));
            parametersForGenericOvertimeRule.addParameter(
                "AdditionalMinutesWorked", String.valueOf(seedAfter));
            parametersForGenericOvertimeRule.addParameter("EligibleTimeCodes",
                workDetailTimeCodes);
            parametersForGenericOvertimeRule.addParameter(
                "AreTimeCodesInclusive", "true");
            parametersForGenericOvertimeRule.addParameter("EligibleHourTypes",
                eligibleHourTypes);
            parametersForGenericOvertimeRule.addParameter("AddPremiumRecord",
                "" + (premiumTimeCodeInserted != null));
            parametersForGenericOvertimeRule.addParameter("PremiumTimeCode",
                premiumTimeCodeInserted);
            parametersForGenericOvertimeRule.addParameter("SkdZoneStartDate",
                dateFormat.format(dateWeekStarts));
            parametersForGenericOvertimeRule.addParameter("SkdZoneEndDate",
                dateFormat.format(dateWeekEnds));
            parametersForGenericOvertimeRule.addParameter(
                "HourTypeForOvertimeWorkDetails",
                hourTypeForOvertimeWorkDetails);
            super.execute(wbData, parametersForGenericOvertimeRule);
        }
        else {
            parametersForGenericOvertimeRule.addParameter("HourSetDescription",
                hourSetDescription);
            parametersForGenericOvertimeRule.addParameter(
                "AdditionalMinutesWorked",
                String.valueOf(seedBefore));
            parametersForGenericOvertimeRule.addParameter("EligibleTimeCodes",
                workDetailTimeCodes);
            parametersForGenericOvertimeRule.addParameter(
                "AreTimeCodesInclusive", "true");
            parametersForGenericOvertimeRule.addParameter("EligibleHourTypes",
                eligibleHourTypes);
            parametersForGenericOvertimeRule.addParameter("AddPremiumRecord",
                "" + (premiumTimeCodeInserted != null));
            parametersForGenericOvertimeRule.addParameter("PremiumTimeCode",
                premiumTimeCodeInserted);
            parametersForGenericOvertimeRule.addParameter("SkdZoneStartDate",
                dateFormat.format(dateWeekStarts));
            parametersForGenericOvertimeRule.addParameter("SkdZoneEndDate",
                dateFormat.format(DateHelper.addDays(dateWeekStarts, 6)));
            parametersForGenericOvertimeRule.addParameter(
                "HourTypeForOvertimeWorkDetails",
                hourTypeForOvertimeWorkDetails);
            super.execute(wbData, parametersForGenericOvertimeRule);

        }

        // applyRates();
    }

    protected void ruleWeeklyOvertime(WBData wbData, String hourSetDescription,
                                      String dayWeekStarts, String workDetailTimeCodes,
                                      String premiumTimeCodesCounted, String eligibleHourTypes,
                                      String discountTimeCodes, String premiumTimeCodeInserted,
                                      String hourTypeForOvertimeWorkDetails, boolean assignBetterRate) throws Exception {

        int seedMinutes = 0;
        Date workSummaryDate = wbData.getRuleData().getWorkSummary().getWrksWorkDate();
        Date dateWeekStarts = DateHelper.nextDay(DateHelper.addDays(workSummaryDate, -7), dayWeekStarts);

        // Include the work premium minutes if premiumTimeCodesCounted is provided
        if (premiumTimeCodesCounted != null) {
            seedMinutes += wbData.getMinutesWorkDetailPremiumRange(dateWeekStarts, DateHelper.addDays(dateWeekStarts, 6), null, null, premiumTimeCodesCounted, true, eligibleHourTypes, true, "P" , false);
        }

        if (discountTimeCodes != null) {
            final Date THE_DISTANT_PAST = new java.util.GregorianCalendar(1900, 0, 1).getTime();
            final Date THE_DISTANT_FUTURE = new java.util.GregorianCalendar(3000, 0, 1).getTime();
            seedMinutes += wbData.getWorkedMinutes(wbData.getRuleData().getWorkSummary().getWrksId(), dateWeekStarts, DateHelper.addDays(dateWeekStarts, 6), THE_DISTANT_PAST, THE_DISTANT_FUTURE, true, discountTimeCodes, null);
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
        parametersForGenericOvertimeRule.addParameter(super.PARAM_ASSIGN_BETTERRATE, String.valueOf(assignBetterRate));
        parametersForGenericOvertimeRule.addParameter(super.PARAM_INTERVAL_STARTTIME, null);
        parametersForGenericOvertimeRule.addParameter(super.PARAM_INTERVAL_ENDTIME, null);
        parametersForGenericOvertimeRule.addParameter(super.PARAM_STARTTIME_WITHIN_SHIFT, null);
        parametersForGenericOvertimeRule.addParameter(super.PARAM_ENDTIME_WITHIN_SHIFT, null);
        super.execute(wbData, parametersForGenericOvertimeRule);

        // applyRates();
    }

    public String getComponentName() {
        return "WBIAG: Weekly Overtime Extended Rule";
    }


    public List getSuitableConditions() {
        List list = new ArrayList();
        list.add(new com.workbrain.app.ta.conditions.AlwaysTrueCondition());
        return list;
    }
}