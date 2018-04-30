package com.wbiag.app.ta.conditions;

import java.util.*;
import com.workbrain.util.DateHelper;
import com.workbrain.sql.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.app.ta.model.PayGroupData;

/**
 * The condition compares the number of units (DAY/WEEK/PAY PERIOD/MONTH/QUARTER/YEAR) employeed
 * with given unit value based on hire/seniroty or constant date.
 *
 */
public class IsEmployedPeriodCondition    extends Condition {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(IsEmployedPeriodCondition.class);


    public static final String PARAM_REFERENCE = "Reference";
    public static final String PARAM_VAL_REFERENCE_HIRE = "HIRE_DATE";
    public static final String PARAM_VAL_REFERENCE_SENIORITY = "SENIORITY_DATE";
    public static final String PARAM_VAL_REFERENCE_BIRTH = "BIRTH_DATE";
    public static final String PARAM_VAL_DATE_PREFIX = "DATE=";
    public static final String PARAM_VAL_DATE_FORMAT = "MM/dd/yyyy";
    public static final String PARAM_UNIT_PERIOD = "UnitPeriod";
    public static final String PARAM_UNIT_VALUE = "UnitValue";
    public static final String PARAM_OPERATOR = "Operator";


    public List getParameterInfo(DBConnection conn) {

        ArrayList result = new ArrayList();

        RuleParameterInfo rpiRef = new RuleParameterInfo(PARAM_REFERENCE,
            RuleParameterInfo.STRING_TYPE, false);
        result.add(rpiRef);

        RuleParameterInfo rpi = new RuleParameterInfo(PARAM_OPERATOR,
            RuleParameterInfo.CHOICE_TYPE, false);
        rpi.addChoice(RuleHelper.EQ);
        rpi.addChoice(RuleHelper.LESS);
        rpi.addChoice(RuleHelper.BIGGER);
        rpi.addChoice(RuleHelper.LESSEQ);
        rpi.addChoice(RuleHelper.BIGGEREQ);
        rpi.addChoice(RuleHelper.NOTEQ1);
        result.add(rpi);

        RuleParameterInfo rpiPer = new RuleParameterInfo(PARAM_REFERENCE,
            RuleParameterInfo.CHOICE_TYPE, false);
        rpiPer.addChoice(DateHelper.APPLY_ON_UNIT_DAY);
        rpiPer.addChoice(DateHelper.APPLY_ON_UNIT_WEEK);
        rpiPer.addChoice(DateHelper.APPLY_ON_UNIT_PAYPERIOD);
        rpiPer.addChoice(DateHelper.APPLY_ON_UNIT_MONTH);
        rpiPer.addChoice(DateHelper.APPLY_ON_UNIT_QTR);
        rpiPer.addChoice(DateHelper.APPLY_ON_UNIT_YEAR);
        result.add(rpiPer);

        result.add(new RuleParameterInfo(PARAM_UNIT_VALUE,
                                         RuleParameterInfo.STRING_TYPE, false));


        return result;
    }

    /**
     * Returns the name of the Rule
     *
     * @return String - the unique name of this rule component
     */
    public String getComponentName() {
        return "WBIAG: Is Employed Period Generic Condition";
    }

    /**
     * This method compares the number of days employeed
     * with the given number.
     *
     * @param wbData
     * @param parameters
     */
    public boolean evaluate(WBData wbData, Parameters parameters) throws
        Exception {

        Date employed = null;
        String ref = parameters.getParameter(PARAM_REFERENCE);
        if (PARAM_VAL_REFERENCE_HIRE.equals(ref)) {
            employed = wbData.getEmpHireDate();
        }
        else if (PARAM_VAL_REFERENCE_SENIORITY.equals(ref)) {
            employed = wbData.getEmpSeniorityDate();
        }
        else if (PARAM_VAL_REFERENCE_BIRTH.equals(ref)) {
            employed = wbData.getEmpBirthDate();
        }
        else if (ref.startsWith(PARAM_VAL_DATE_PREFIX) ){
            String dat = ref.substring(PARAM_VAL_DATE_PREFIX.length());
            employed = DateHelper.convertStringToDate(ref , PARAM_VAL_DATE_FORMAT) ;
        }
        else {
            throw new RuleEngineException (PARAM_REFERENCE + " must be "
            +  PARAM_VAL_REFERENCE_HIRE + " , " +  PARAM_VAL_REFERENCE_BIRTH + " , " + PARAM_VAL_REFERENCE_SENIORITY + " or constant date (i.e DATE=01/01/2000");
        }
        int val = parameters.getIntegerParameter(PARAM_UNIT_VALUE);
        String unit = parameters.getParameter(PARAM_UNIT_PERIOD);
        int unitsBetween = getUnitsBetween(wbData , unit ,employed);
        if (logger.isDebugEnabled()) logger.debug("Found : " + unitsBetween + " " + unit + "(s) since date :" + employed + " . Comparing it to : " + val + " " + unit + "(s)" );

        String operator = parameters.getParameter(PARAM_OPERATOR);
        boolean bolResult = RuleHelper.evaluate(new Integer(unitsBetween),
                                                new Integer(val),
                                                operator);


        return bolResult;

    }

    private int getUnitsBetween(WBData wbData, String unit, Date employed) throws Exception {
        double ret;
        Date currDate = wbData.getWrksWorkDate();
        if (DateHelper.APPLY_ON_UNIT_DAY.equals(unit)) {
            ret  = DateHelper.getDifferenceInDays(currDate , employed);
        }
        else if (DateHelper.APPLY_ON_UNIT_WEEK.equals(unit)) {
            ret  = DateHelper.getWeeksBetween(currDate , employed);
        }
        else if (DateHelper.APPLY_ON_UNIT_MONTH.equals(unit)) {
            ret  = DateHelper.getMonthsBetween(employed , currDate);
        }
        else if (DateHelper.APPLY_ON_UNIT_PAYPERIOD.equals(unit)) {
            PayGroupData pgd = wbData.getCodeMapper().getPayGroupById(wbData.getPaygrpId());
            ret  = DateHelper.getDaysBetween(currDate , employed)
                / DateHelper.getDifferenceInDays(pgd.getPaygrpEndDate() , pgd.getPaygrpStartDate());
        }
        else if (DateHelper.APPLY_ON_UNIT_QTR.equals(unit)) {
            ret  = DateHelper.getDaysBetween(currDate , employed) / 90;
        }
        else if (DateHelper.APPLY_ON_UNIT_YEAR.equals(unit)) {
            ret  = DateHelper.getDaysBetween(currDate , employed) / 365;
        }
        else {
            throw new RuleEngineException ("Unit not supported : " + unit);
        }

        return (int)ret;
    }
} //end of class

