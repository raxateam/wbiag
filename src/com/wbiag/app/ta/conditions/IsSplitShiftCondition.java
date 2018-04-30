package com.wbiag.app.ta.conditions;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.sql.DBConnection;
import com.workbrain.util.DateHelper;

/**
 * Condition to check # of shifts in a day and minutes between (i.e returns true if any of the shifts have a duration that satify criteria) <b>
 * If both parameters are supplied, the results will be ANDed
 */

public class IsSplitShiftCondition extends Condition {

	private final static Logger logger = Logger.getLogger(IsSplitShiftCondition.class);
	public final static String PARAM_NUMBER_OF_SHIFTS = "NumberOfShifts";
	public final static String PARAM_NUMBER_OF_SHIFTS_OPERATOR = "NumberOfShiftsOperator";
	public final static String PARAM_MINS_BETWEEN = "MinsBetween";
	public final static String PARAM_MINS_BETWEEN_OPERATOR = "MinsBetweenOperator";


    public List getParameterInfo(DBConnection dBConnection) {
        List result = new ArrayList();
        RuleParameterInfo rpi = new RuleParameterInfo(PARAM_NUMBER_OF_SHIFTS_OPERATOR,
            RuleParameterInfo.CHOICE_TYPE, true);
        rpi.addChoice(RuleHelper.EQ);
        rpi.addChoice(RuleHelper.LESS);
        rpi.addChoice(RuleHelper.BIGGER);
        rpi.addChoice(RuleHelper.LESSEQ);
        rpi.addChoice(RuleHelper.BIGGEREQ);
        rpi.addChoice(RuleHelper.NOTEQ1);
        result.add(rpi);
        result.add(new RuleParameterInfo(PARAM_NUMBER_OF_SHIFTS, RuleParameterInfo.STRING_TYPE, true));
        RuleParameterInfo rpiB = new RuleParameterInfo(PARAM_MINS_BETWEEN_OPERATOR,
            RuleParameterInfo.CHOICE_TYPE, true);
        rpiB.addChoice(RuleHelper.EQ);
        rpiB.addChoice(RuleHelper.LESS);
        rpiB.addChoice(RuleHelper.BIGGER);
        rpiB.addChoice(RuleHelper.LESSEQ);
        rpiB.addChoice(RuleHelper.BIGGEREQ);
        rpiB.addChoice(RuleHelper.NOTEQ1);
        result.add(rpiB);
        result.add(new RuleParameterInfo(PARAM_MINS_BETWEEN, RuleParameterInfo.STRING_TYPE, true));

        return result;
    }

	/**
     * evaluate
     */
    public boolean evaluate(WBData wbData, Parameters parameters) throws java.lang.Exception {

        List swbList = wbData.getShiftsWithBreaks();
    	boolean resultNs = true;
        int numShifts = parameters.getIntegerParameter(PARAM_NUMBER_OF_SHIFTS , -1);
        if (numShifts >= 0) {
            String nsOp = parameters.getParameter(PARAM_NUMBER_OF_SHIFTS_OPERATOR , RuleHelper.EQ);
            resultNs = RuleHelper.evaluate(new Integer(swbList.size()) ,
                                           new Integer(numShifts), nsOp) ;
            if (logger.isDebugEnabled()) logger.debug("Evaluated to :" + resultNs + " for shift Count:" + swbList.size() + " , NumberOfShifts:" + numShifts + " , operator:" + nsOp);
        }
    	boolean resultMb = false;
        int minsBetween = parameters.getIntegerParameter(PARAM_MINS_BETWEEN , -1);
        String mbOp = parameters.getParameter(PARAM_NUMBER_OF_SHIFTS_OPERATOR , RuleHelper.EQ);
        if (minsBetween >= 0) {
            for (int k = 0, l = swbList.size(); k < l; k++) {
                ShiftWithBreaks swbThis = wbData.getShiftWithBreaks(k);
                if (!swbThis.isScheduledActual()) {
                    continue;
                }
                ShiftWithBreaks swbBef = wbData.getShiftWithBreaksBefore(k);
                if (swbBef != null) {
                    int diff = (int)DateHelper.getMinutesBetween(swbThis.getShftStartTime() , swbBef.getShftEndTime());
                    boolean res = RuleHelper.evaluate(new Integer(diff) , new Integer(minsBetween), mbOp) ;
                    if (logger.isDebugEnabled()) logger.debug("Evaluated to :" + res + " for shift duration between:" + diff + " , MinsBetween:" + minsBetween + " , operator:" + mbOp);
                    resultMb |= res;
                    if (resultMb) break;
                }
            }
        }

        return resultMb && resultNs;
    }

    /**
     * @return component name
     */
	public String getComponentName() {
        return  "WBIAG: Is Split Shift Condition";
    }



}

