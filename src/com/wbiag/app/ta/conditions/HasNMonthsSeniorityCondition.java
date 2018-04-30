package com.wbiag.app.ta.conditions;

import java.util.*;
import com.workbrain.util.DateHelper;
import com.workbrain.sql.*;
import com.workbrain.app.ta.ruleengine.*;

/**
 * The condition compares the number of months seniority
 * with a given number.
 *
 * @deprecated Use {@link IsEmployedPeriodCondition}
 */
public class HasNMonthsSeniorityCondition
    extends Condition {

    public static final String PARAM_N_MONTHS = "SeniorityMonths";
    public static final String PARAM_OPERATOR = "Operator";


    public HasNMonthsSeniorityCondition() {
    }

    /**
     * Displays the parameters used by the Condition in RuleBuilder
     *
     * @param conn - database Connection Object
     * @return List - a list of RuleParameterInfo instances that describe the parameters used by the rule component
     * <ul>
     * <li>SeniorityMonths 			 - Number of Months of Seniority
     * <li>Operator - The operator used to compare the current Months Seniority with the SeniorityMonths
     * </ul>
     */
    public List getParameterInfo(DBConnection conn) {

        ArrayList result = new ArrayList();

        result.add(new RuleParameterInfo(PARAM_N_MONTHS,
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
        return "WBIAG: Has N Months Seniority Condition";
    }

    /**
     * This method compares the number of months seniority
     * with the given number.
     *
     * @param wbData - all the information <BR>
     *  parameters - RuleData parameters
     */
    public boolean evaluate(WBData wbData, Parameters parameters) throws
        Exception {

        double NMonthsValue = (double) parameters.getIntegerParameter(
            PARAM_N_MONTHS);
        Date now = wbData.getWrksWorkDate();
        Date seniority = wbData.getEmpSeniorityDate();
        double currentMonths =  (int) DateHelper.getMonthsBetween(seniority,now);

        String operator = parameters.getParameter(PARAM_OPERATOR);
        boolean bolResult = RuleHelper.evaluate(new Double(currentMonths),
                                                new Double(NMonthsValue),
                                                operator);


        return bolResult;

    } //end of execute() method

} //end of class
