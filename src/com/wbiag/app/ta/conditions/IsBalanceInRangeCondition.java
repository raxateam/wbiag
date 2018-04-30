package com.wbiag.app.ta.conditions;

import java.util.*;
import com.workbrain.sql.*;
import com.workbrain.app.ta.ruleengine.*;

/**
 * The condition compares the current value of a given Balance(ex: FLOAT,VACATION etc)
 * with a given number.
 *
 * @author WBIAG
 * @see  com.workbrain.app.ta.ruleengine.Rule
 */
public class IsBalanceInRangeCondition
    extends Condition {

    public static final String PARAM_BALANCE_NAME = "BalanceName";
    public static final String PARAM_BALANCE_VALUE = "BalanceValue";
    public static final String PARAM_OPERATOR = "Operator";

    public IsBalanceInRangeCondition() {
    }

    /**
     * Displays the parameters used by the Condition in RuleBuilder
     *
     * @param conn - database Connection Object
     * @return List - a list of RuleParameterInfo instances that describe the parameters used by the rule component
     * <ul>
     * <li>BalanceName 			 - The name of the balance to compare
     * <li>Operator - The operator used to compare the current balance with the BalanceValue
     * <li>BalanceValue 		 - The value checked against the current balance, with the operator
     * </ul>
     */
    public List getParameterInfo(DBConnection conn) {

        ArrayList result = new ArrayList();

        result.add(new RuleParameterInfo(PARAM_BALANCE_NAME,
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

        result.add(new RuleParameterInfo(PARAM_BALANCE_VALUE,
                                         RuleParameterInfo.INT_TYPE, false));

        return result;
    }

    /**
     * Returns the name of the Rule
     *
     * @return String - the unique name of this rule component
     */
    public String getComponentName() {
        return "WBIAG: Is Balance In Range Condition";
    }

    /**
     * This method compares the current value of a given Balance(ex: FLOAT,VACATION etc)
     * with the given number.
     *
     * @param wbData - all the information <BR>
     *  parameters - RuleData parameters
     */
    public boolean evaluate(WBData wbData, Parameters parameters) throws
        Exception {

        String balanceName = parameters.getParameter("BalanceName");
        String balanceOperator = parameters.getParameter("Operator");
        double balanceValue = (double) parameters.getIntegerParameter(
            "BalanceValue");

        double currentBalance = wbData.getEmployeeBalanceValue(balanceName);
        boolean bolResult = RuleHelper.evaluate(new Double(currentBalance),
                                                new Double(balanceValue),
                                                balanceOperator);

        return bolResult;

    } //end of execute() method

} //end of class
