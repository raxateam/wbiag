package com.wbiag.app.ta.conditions;

import java.util.*;
import com.workbrain.util.DateHelper;
import com.workbrain.sql.*;
import com.workbrain.app.ta.ruleengine.*;

/**
 * The condition compares the number of days employeed
 * with a given number.
 *
 * @deprecated Use {@link IsEmployedPeriodCondition}
 */
public class IsEmployedNDaysCondition
    extends Condition {

    public static final String PARAM_N_DAYS = "NDays";
    public static final String PARAM_OPERATOR = "Operator";


    public IsEmployedNDaysCondition() {
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

        result.add(new RuleParameterInfo(PARAM_N_DAYS,
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


        return result;
    }

    /**
     * Returns the name of the Rule
     *
     * @return String - the unique name of this rule component
     */
    public String getComponentName() {
        return "WBIAG: Is Employed N Days Condition";
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

        double NDaysValue = (double) parameters.getIntegerParameter(
            PARAM_N_DAYS);
        Date now = wbData.getWrksWorkDate();
        Date hired = wbData.getEmpHireDate();
        double currentDays = DateHelper.dateDifferenceInDays(now,hired);

        String operator = parameters.getParameter(PARAM_OPERATOR);
        boolean bolResult = RuleHelper.evaluate(new Double(currentDays),
                                                new Double(NDaysValue),
                                                operator);


        return bolResult;

    } //end of execute() method

} //end of class
