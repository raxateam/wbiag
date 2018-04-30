package com.wbiag.app.ta.quickrules;

// standard imports for rules
import com.workbrain.app.ta.ruleengine.Parameters;
import com.workbrain.app.ta.ruleengine.RuleParameterInfo;
import com.workbrain.app.ta.ruleengine.WBData;
import com.workbrain.app.ta.ruleengine.RuleHelper;
import com.workbrain.app.ta.model.*;
import com.workbrain.sql.DBConnection;
import java.util.ArrayList;
import java.util.List;

// extra imports needed for this rule
import com.workbrain.app.ta.rules.GenericOvertimeRule;
import com.workbrain.util.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Weekly Overtime rule to first apply to home dept(or any labor attribute) and then borrowed dept(or any labor attribute)
 */
public class WeeklyOvertimeBorrowRule extends GenericOvertimeExtRule {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(WeeklyOvertimeBorrowRule.class);

    public final static String PARAM_HOURSET_DESCRIPTION = "HourSetDescription";
    public final static String PARAM_WORKDETAIL_TIMECODES = "WorkDetailTimeCodes";
    public final static String PARAM_ELIGIBLE_HOURTYPES = "EligibleHourTypes";
    public final static String PARAM_PREMIUM_TIMECODES_COUNTED = "PremiumTimeCodesCounted";
    public final static String PARAM_DISCOUNT_TIMECODES = "DiscountTimeCodes";
    public final static String PARAM_DAY_WEEK_STARTS = "DayWeekStarts";
    public final static String PARAM_PREMIUM_TIMECODE_INSERTED = "PremiumTimeCodeInserted";
    public final static String PARAM_HOURTYPE_FOR_OVERTIME_WORKDETAILS = "HourTypeForOvertimeWorkDetails";
    public final static String PARAM_WORK_DETAIL_BORROW_FIELD = "WorkDetailBorrowField";
    public final static String PARAM_EMP_BORROW_FIELD = "EmployeeBorrowField";
    public final static List PARAM_VAL_WD_BORROW_VALS = new ArrayList();
    static {
        PARAM_VAL_WD_BORROW_VALS.add("dept");
        PARAM_VAL_WD_BORROW_VALS.add("job");
        PARAM_VAL_WD_BORROW_VALS.add("wbt");
        PARAM_VAL_WD_BORROW_VALS.add("proj");
        for (int i = 1; i <= 10; i++) {
            PARAM_VAL_WD_BORROW_VALS.add("Udf" + i);
        }
    }

    public final static List PARAM_VAL_EMP_BORROW_VALS = new ArrayList();
    static {
        PARAM_VAL_EMP_BORROW_VALS.add("edlaDept");
        PARAM_VAL_EMP_BORROW_VALS.add("edlaJob");
        PARAM_VAL_EMP_BORROW_VALS.add("edlaWbt");
        PARAM_VAL_EMP_BORROW_VALS.add("edlaProj");
        for (int i = 1; i <= 20; i++) {
            PARAM_VAL_EMP_BORROW_VALS.add("empVal" + i);
        }
        PARAM_VAL_EMP_BORROW_VALS.add("empWbt");
    }

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

        result.add(new RuleParameterInfo(PARAM_PREMIUM_TIMECODE_INSERTED, RuleParameterInfo.STRING_TYPE, true));

        result.add(new RuleParameterInfo(PARAM_HOURTYPE_FOR_OVERTIME_WORKDETAILS, RuleParameterInfo.STRING_TYPE, true));

        RuleParameterInfo rpiWdBrwFld = new RuleParameterInfo(PARAM_WORK_DETAIL_BORROW_FIELD, RuleParameterInfo.CHOICE_TYPE, false);
        Iterator iter = PARAM_VAL_WD_BORROW_VALS.iterator();
        while (iter.hasNext()) {
            String item = (String)iter.next();
            rpiWdBrwFld.addChoice(item);
        }
        result.add(rpiWdBrwFld);

        RuleParameterInfo rpiEmpBrwFld = new RuleParameterInfo(PARAM_EMP_BORROW_FIELD, RuleParameterInfo.CHOICE_TYPE, false);
        Iterator iterE = PARAM_VAL_EMP_BORROW_VALS.iterator();
        while (iterE.hasNext()) {
            String item = (String)iterE.next();
            rpiEmpBrwFld.addChoice(item);
        }
        result.add(rpiEmpBrwFld);

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
        String premiumTimeCodeInserted = parameters.getParameter(
            PARAM_PREMIUM_TIMECODE_INSERTED, null);
        // assignBetterRate is to be passed to GenericOvertimeRule
        boolean assignBetterRate = Boolean.valueOf(parameters.getParameter(
            PARAM_ASSIGN_BETTERRATE, "true")).booleanValue();
        String hourTypeForOvertimeWorkDetails = parameters.getParameter(
            PARAM_HOURTYPE_FOR_OVERTIME_WORKDETAILS, null);
        String wdBrwField = parameters.getParameter(
            PARAM_WORK_DETAIL_BORROW_FIELD, null);
        if (!PARAM_VAL_WD_BORROW_VALS.contains(wdBrwField)) {
            throw new RuntimeException ("Work Detail Borrow fields must be one of " + StringHelper.createCSVForCharacter(PARAM_VAL_WD_BORROW_VALS));
        }
        String empBrwField = parameters.getParameter(
            PARAM_EMP_BORROW_FIELD, "empWbt");
        if (!PARAM_VAL_EMP_BORROW_VALS.contains(empBrwField)) {
            throw new RuntimeException ("Employee Borrow field must be one of " + StringHelper.createCSVForCharacter(PARAM_VAL_EMP_BORROW_VALS));
        }

        ruleWeeklyOvertime(wbData, hourSetDescription,
                           dayWeekStarts, workDetailTimeCodes,
                           premiumTimeCodesCounted,
                           eligibleHourTypes, discountTimeCodes,
                           premiumTimeCodeInserted ,
                           hourTypeForOvertimeWorkDetails, assignBetterRate,
                           wdBrwField, empBrwField);
        // *** make sure yuo add current date if recalc doesn't do it
        wbData.addEmployeeDateToAutoRecalculate(wbData.getEmpId() , wbData.getWrksWorkDate());
    }

    protected void ruleWeeklyOvertime(WBData wbData, String hourSetDescription,
                                      String dayWeekStarts, String workDetailTimeCodes,
                                      String premiumTimeCodesCounted, String eligibleHourTypes,
                                      String discountTimeCodes, String premiumTimeCodeInserted,
                                      String hourTypeForOvertimeWorkDetails, boolean assignBetterRate,
                                      String wdBrwField,
                                      String empBrwField) throws Exception {

        int seedMinutes = 0;
        Date workSummaryDate = wbData.getRuleData().getWorkSummary().getWrksWorkDate();
        Date dateWeekStarts = DateHelper.nextDay(DateHelper.addDays(workSummaryDate, -7), dayWeekStarts);
        Date dateWeekEnd = DateHelper.addDays(dateWeekStarts, 6);

        String  wrkdProp = getWrkdProp(wbData , wdBrwField);
        Object  homeBorrowVal = getEmpPropVal(wbData , empBrwField , dateWeekEnd);
        if (logger.isDebugEnabled()) logger.debug("Employee home field resolved to :" + homeBorrowVal);
        if (logger.isDebugEnabled()) logger.debug("Checking overtime for " + wrkdProp + " => " + homeBorrowVal);
        // *** first do home dept
        String exprStringHomeBorrow =  null;
        if (!StringHelper.isEmpty(homeBorrowVal)) {
            exprStringHomeBorrow = wrkdProp + "=" + homeBorrowVal + "||" +  wrkdProp + RuleHelper.IS_EMPTY;
        }
        if (premiumTimeCodesCounted != null) {
            seedMinutes += wbData.getMinutesWorkDetailPremiumRange(dateWeekStarts,
                DateHelper.addDays(dateWeekStarts, 6), null, null,
                premiumTimeCodesCounted, true, eligibleHourTypes, true, "P" , false);
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
        parametersForGenericOvertimeRule.addParameter(super.PARAM_ELIGIBLE_WORK_DETAIL_EXPRESSION_STRING, exprStringHomeBorrow);
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

        // *** then do non-home dept if employee has home dept val
        if (!StringHelper.isEmpty(homeBorrowVal)) {
            //seedMinutes = 0;
            parametersForGenericOvertimeRule = new Parameters();
            String exprStringNoneNoneHomeBorrow = wrkdProp + "!=" +
                homeBorrowVal + "&&" + wrkdProp + RuleHelper.IS_NOT_EMPTY;
            seedMinutes += super.getWorkedMinutes(wbData,
                                                  wbData.getRuleData().
                                                  getWorkSummary().getWrksId(),
                                                  dateWeekStarts, dateWeekEnd, null, null,
                                                  true, workDetailTimeCodes,
                                                  eligibleHourTypes,
                                                  exprStringHomeBorrow);
            seedMinutes += super.getWorkedMinutes(wbData,
                                                  wbData.getRuleData().
                                                  getWorkSummary().getWrksId(),
                                                  dateWeekStarts,
                                                  workSummaryDate, null, null,
                                                  true, workDetailTimeCodes,
                                                  eligibleHourTypes,
                                                  exprStringNoneNoneHomeBorrow);
            if (logger.isDebugEnabled()) logger.debug("Checking overtime for " + wrkdProp + " <> " +  homeBorrowVal);
            parametersForGenericOvertimeRule.addParameter(super.
                PARAM_HOURSET_DESCRIPTION, hourSetDescription);
            parametersForGenericOvertimeRule.addParameter(super.
                PARAM_INTERVAL_STARTDATE, dateFormat.format(workSummaryDate));
            parametersForGenericOvertimeRule.addParameter(super.
                PARAM_INTERVAL_ENDDATE, dateFormat.format(workSummaryDate));
            parametersForGenericOvertimeRule.addParameter(super.
                PARAM_ADDITIONAL_MINUTES_WORKED, Integer.toString(seedMinutes));
            parametersForGenericOvertimeRule.addParameter(super.
                PARAM_ELIGIBLE_TIMECODES, workDetailTimeCodes);
            parametersForGenericOvertimeRule.addParameter(super.
                PARAM_ARE_TIMECODES_INCLUSIVE, "true");
            parametersForGenericOvertimeRule.addParameter(super.
                PARAM_ELIGIBLE_HOURTYPES, eligibleHourTypes);
            parametersForGenericOvertimeRule.addParameter(super.
                PARAM_ELIGIBLE_WORK_DETAIL_EXPRESSION_STRING,
                exprStringNoneNoneHomeBorrow);
            parametersForGenericOvertimeRule.addParameter(super.
                PARAM_ADD_PREMIUMRECORD, "" + (premiumTimeCodeInserted != null));
            parametersForGenericOvertimeRule.addParameter(super.
                PARAM_PREMIUM_TIMECODE, premiumTimeCodeInserted);
            parametersForGenericOvertimeRule.addParameter(super.
                PARAM_SKDZONE_STARTDATE, dateFormat.format(dateWeekStarts));
            parametersForGenericOvertimeRule.addParameter(super.
                PARAM_SKDZONE_ENDDATE,
                dateFormat.format(DateHelper.addDays(dateWeekStarts, 6)));
            parametersForGenericOvertimeRule.addParameter(super.
                PARAM_HOURTYPE_FOR_OVERTIME_WORKDETAILS,
                hourTypeForOvertimeWorkDetails);
            parametersForGenericOvertimeRule.addParameter(super.
                PARAM_ASSIGN_BETTERRATE, String.valueOf(assignBetterRate));
            parametersForGenericOvertimeRule.addParameter(super.
                PARAM_INTERVAL_STARTTIME, null);
            parametersForGenericOvertimeRule.addParameter(super.
                PARAM_INTERVAL_ENDTIME, null);
            parametersForGenericOvertimeRule.addParameter(super.
                PARAM_STARTTIME_WITHIN_SHIFT, null);
            parametersForGenericOvertimeRule.addParameter(super.
                PARAM_ENDTIME_WITHIN_SHIFT, null);
            super.execute(wbData, parametersForGenericOvertimeRule);
        }

    }

    private String getWrkdProp(WBData wbData , String wdBrwField){
        EmployeeDefaultLaborData edl = wbData.getRuleData().getEmpDefaultLabor(0);
        String edlaProp = wdBrwField.startsWith("Udf") ? ("edla" + wdBrwField) : (wdBrwField + "Id");
        return wdBrwField.startsWith("Udf") ? ("wrkd" + wdBrwField) : (wdBrwField + "Id");
    }

    /**
     * Retrieves home value for emp as of end of week
     * @param wbData
     * @param empBrwField
     * @param dateWeekEnd
     * @return
     */
    private Object getEmpPropVal(WBData wbData , String empBrwField, Date dateWeekEnd){

        String prop = empBrwField.startsWith("edla")
            ? (empBrwField.substring(4).toLowerCase() + "Id") : (empBrwField);
        Object empBrwVal = null;
        if (empBrwField.startsWith("edla")) {
            List edls =
                wbData.getRuleData().getCalcDataCache()
                .getEmployeeDefaultLaborRecords(wbData.getEmpId() , dateWeekEnd);
            if (edls.size() > 0) {
                empBrwVal = ((EmployeeDefaultLaborData) edls.get(0)).getProperty(prop);
            }
        }
        else if ("empWbt".equals(empBrwField)) {
            int empHomeWbtId = wbData.getRuleData().getCalcDataCache()
                .getEmployeeHomeTeam(wbData.getEmpId() , dateWeekEnd, wbData.getDBconnection());
            empBrwVal = new Integer(empHomeWbtId);
        }
        else if (empBrwField.startsWith("emp")) {
            EmployeeData emp = wbData.getRuleData().getCalcDataCache().getEmployeeData(
                wbData.getEmpId() , wbData.getWrksWorkDate() , wbData.getDBconnection() , wbData.getCodeMapper());
            empBrwVal =  emp.getProperty(prop);
        }
        return empBrwVal;
    }

    public String getComponentName() {
        return "WBIAG: Weekly Overtime Borrow Rule";
    }


    public List getSuitableConditions() {
        List list = new ArrayList();
        list.add(new com.workbrain.app.ta.conditions.AlwaysTrueCondition());
        return list;
    }
}
