package com.wbiag.app.ta.conditions;

import java.util.*;
import com.workbrain.util.*;
import com.workbrain.sql.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.app.ta.model.*;

/**
 * The condition determines is work summary date is in past/future
 *
 * @author WBIAG
 * @see  com.workbrain.app.ta.ruleengine.Rule
 */
public class IsCalcDateToCurrentDateCondition extends Condition {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(IsCalcDateToCurrentDateCondition.class);

    public static final String PARAM_NUMBER_OF_DAYS = "NumberOfDays";
    public static final String PARAM_OPERATOR = "Operator";
    public static final String PARAM_PERIOD = "Period";
    public static final String PARAM_VAL_PERIOD_PAST = "PAST";
    public static final String PARAM_VAL_PERIOD_FUTURE = "FUTURE";
    public static final String PARAM_USE_EMPLOYEE_TIMEZONE = "UseEmployeeTimezone";


    public IsCalcDateToCurrentDateCondition() {
    }

    /**
     * Displays the parameters used by the Condition in RuleBuilder
     *
     * @param conn - database Connection Object
     * @return List - a list of RuleParameterInfo instances that describe the parameters used by the rule component
     * <ul>
     * <li>NDays 			 - Number of Days of employment
     * <li>Operator - The operator used to compare the current balance with the BalanceValue
     * </ul>
     */
    public List getParameterInfo(DBConnection conn) {

        ArrayList result = new ArrayList();

        result.add(new RuleParameterInfo(PARAM_NUMBER_OF_DAYS,
                                         RuleParameterInfo.STRING_TYPE, false));
        RuleParameterInfo rpi = new RuleParameterInfo(PARAM_OPERATOR,
            RuleParameterInfo.CHOICE_TYPE, false);
        rpi.addChoice(RuleHelper.EQ);
        rpi.addChoice(RuleHelper.LESS);
        rpi.addChoice(RuleHelper.BIGGER);
        rpi.addChoice(RuleHelper.LESSEQ);
        rpi.addChoice(RuleHelper.BIGGEREQ);
        rpi.addChoice(RuleHelper.NOTEQ1);
        result.add(rpi);

        RuleParameterInfo rpi2 = new RuleParameterInfo(PARAM_PERIOD,
            RuleParameterInfo.CHOICE_TYPE, false);
        rpi2.addChoice(PARAM_VAL_PERIOD_FUTURE);
        rpi2.addChoice(PARAM_VAL_PERIOD_PAST);
        result.add(rpi2);

        result.add(new RuleParameterInfo(PARAM_USE_EMPLOYEE_TIMEZONE,
                                         RuleParameterInfo.STRING_TYPE));
        return result;
    }

    /**
     * Returns the name of the Rule
     *
     * @return String - the unique name of this rule component
     */
    public String getComponentName() {
        return "WBIAG: Is Calc Date to Current Date Condition";
    }

    /**
     * This method compares the number of days employeed
     * with the given number.
     *
     * @param wbData - all the information <BR>
     *  parameters - RuleData parameters
     */
    public boolean evaluate(WBData wbData, Parameters parameters) throws
        Exception {

        int daysValue = parameters.getIntegerParameter(PARAM_NUMBER_OF_DAYS);
        String period = parameters.getParameter(PARAM_PERIOD);
        if (!PARAM_VAL_PERIOD_FUTURE.equals(period)
            && !PARAM_VAL_PERIOD_PAST.equals(period)) {
            throw new RuntimeException ("Period must be : " +  PARAM_VAL_PERIOD_FUTURE + " or " + PARAM_VAL_PERIOD_PAST);
        }
        String operator = parameters.getParameter(PARAM_OPERATOR);
        boolean useEmpTz = Boolean.valueOf(
             parameters.getParameter(PARAM_USE_EMPLOYEE_TIMEZONE,
                                     Boolean.TRUE.toString())).booleanValue();

        Date wrksWorkDate = wbData.getWrksWorkDate();
        Date currentDate = DateHelper.getCurrentDate();
        // *** if employee has timezone defined, use that
        if (useEmpTz) {
            Integer empTzId = wbData.getRuleData().getEmployeeData().getTzId();
            if (empTzId != null) {
                TimezoneData tzd = wbData.getCodeMapper().getTimeZoneById(
                    empTzId.intValue());
                currentDate = TimeZoneUtil.getSystemDateForTimeZone(
                    TimeZone.getTimeZone(tzd.getTzJavaName()));
                currentDate = DateHelper.truncateToDays(currentDate);
            }
        }
        int coef = PARAM_VAL_PERIOD_FUTURE.equals(period) ? 1 : -1;
        Date compare = DateHelper.addDays(currentDate , coef * daysValue) ;
        if (logger.isDebugEnabled()) logger.debug("Comparing " + compare + " " + operator + " " + wrksWorkDate);
        boolean bolResult = RuleHelper.evaluate(compare,
                                                wrksWorkDate,
                                                operator);

        return bolResult;

    }

}
