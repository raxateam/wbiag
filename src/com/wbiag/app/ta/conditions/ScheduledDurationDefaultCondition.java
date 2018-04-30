package com.wbiag.app.ta.conditions;

import java.util.*;
import com.workbrain.sql.DBConnection;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.util.*;
/**
 * The condition compares the ScheduleDuration to PARAM_COMPARE_TO_DURATION for employee's default schedule
 *
 *
 * @author WBIAG
 * @see  com.workbrain.app.ta.ruleengine.Rule
 */
public class ScheduledDurationDefaultCondition
    extends Condition {

    public static final String PARAM_COMPARE_TO_DURATION = "CompareToDuration";
    public static final String PARAM_OPERATOR = "Operator";

    public ScheduledDurationDefaultCondition() {
    }

    /**
     * Displays the parameters used by the Condition in RuleBuilder
     *
     * @param conn - database Connection Object
     * @return List - a list of RuleParameterInfo instances that describe the parameters used by the rule component
     * <ul>
     * <li>Operator - The operator used to compare the current duration with PARAM_COMPARE_TO_DURATION
     * <li>PARAM_COMPARE_TO_DURATION 		 - The value checked against the current duration, with the operator
     * </ul>
     */
    public List getParameterInfo(DBConnection conn) {

        ArrayList result = new ArrayList();

        result.add(new RuleParameterInfo(PARAM_COMPARE_TO_DURATION,
                                         RuleParameterInfo.INT_TYPE, false));

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
        return "WBIAG: Scheduled Duration Default";
    }

    /**
     * This method compares the current schedule duration to the
     * PARAM_COMPARE_TO_DURATION with PARAM_OPERATOR
     *
     * @param wbData - all the information <BR>
     *  parameters - RuleData parameters
     */
    public boolean evaluate(WBData wbData, Parameters parameters) throws
        Exception {

        String operator = parameters.getParameter(PARAM_OPERATOR);
        double defaultValue = (double) parameters.getIntegerParameter(
            PARAM_COMPARE_TO_DURATION);

        double currentValue = getDefaultScheduleDuration(wbData );
        boolean bolResult = RuleHelper.evaluate(new Double(currentValue),
                                                new Double(defaultValue),
                                                operator);

        return bolResult;

    } //end of execute() method

    private int getDefaultScheduleDuration(WBData wbData) {
        int appliedMinutes = 0;
        if (!wbData.getEmployeeScheduleData().isEmployeeScheduledDefault()) {
            return appliedMinutes;
        }
        ShiftWithBreaks swb = new ShiftWithBreaks();
        ShiftData sd = new ShiftData();
        sd.setShftStartTime(wbData.getEmployeeScheduleData().getEmpskdDefStartTime());
        sd.setShftEndTime(wbData.getEmployeeScheduleData().getEmpskdDefEndTime());
        swb.setShift(sd);
        List shiftBreaks = new ArrayList();
        shiftBreaks = wbData.getCodeMapper().getShiftBreaksByShiftId(wbData.getEmployeeScheduleData().getEmpskdDefShiftId());
        // *** always normalize break times so that shiftWithBreaks holds the normalizeds
        for (int k = 0; k < shiftBreaks.size(); k++) {
            ShiftBreakData sbData = (ShiftBreakData) shiftBreaks.get(k);
            ShiftBreakData.normalizeShiftBreak(sbData, sd.getShftStartTime());
        }
        swb.setShiftBreaks(shiftBreaks);
        return swb.retrieveScheduledMinutesPaid(wbData.getCodeMapper());

    }



} //end of class

