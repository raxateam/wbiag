package com.wbiag.app.ta.conditions;

import com.wbiag.app.ta.model.StartEndTime;
import com.wbiag.util.ScheduleHelper;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.DBConnection;
import com.workbrain.util.DateHelper;
import com.workbrain.app.ta.model.EmployeeScheduleData;
import com.workbrain.app.ta.model.WorkDetailList;

import java.util.*;

/**
 *  Title:          WorkedNMinutesContinuousCondition
 *  Description:    A condition to check the number of minutes worked continuously.
 *  Copyright:      Copyright (c) 2003
 *  Company:        Workbrain Inc
 *
 *  @author         Brian Viveiros
 */

public class WorkedNMinutesContinuousCondition extends Condition
{
    public final static String PARAM_WORK_MINUTES = "WorkMinutes";
    public final static String PARAM_OPERATOR = "Operator";
    public final static String PARAM_TCODE_NAME_LIST = "TcodeNameList";
    public final static String PARAM_TCODE_INCLUSIVE = "TcodeInclusive";
    public final static String PARAM_HTYPE_NAME_LIST = "HtypeNameList";
    public final static String PARAM_HTYPE_INCLUSIVE = "HtypeInclusive";
    public final static String PARAM_CONTINUE_TCODE_NAME_LIST = "ContinueTcodeNameList";
    public final static String PARAM_TIME_SPAN = "TimeSpan";

    public final static String CHOICE_WITHIN_SCHEDULE = "Within Schedule";
    public final static String CHOICE_OUTSIDE_SCHEDULE = "Outside Schedule";
    public final static String CHOICE_ALL = "All";





	/**
	 * Executes the condition.
	 */
    public boolean evaluate(WBData wbData, Parameters parameters) throws Exception {

    	int minutes = 0;

    	// Get the parameters.
    	ParametersResolved pars = getParameters(parameters);

    	// It is important that the work details appear in chronological order.
    	wbData.getRuleData().getWorkDetails().sort();

    	// Get the number of minutes based on the timeSpan given.
    	if (CHOICE_ALL.equalsIgnoreCase(pars.timeSpan)) {
    		minutes = getMinutesAll(wbData, pars);
    	}
        else if (CHOICE_WITHIN_SCHEDULE.equalsIgnoreCase(pars.timeSpan)) {
    		minutes = getMinutesWithinSchedule(wbData, pars);
    	}
        else if (CHOICE_OUTSIDE_SCHEDULE.equalsIgnoreCase(pars.timeSpan)){
    		minutes = getMinutesOutsideSchedule(wbData, pars);
    	}
        else {
            throw new RuleEngineException ("TimeSpan value not supported : " + pars.timeSpan);
        }

    	// Evaluate the condition.
        return RuleHelper.evaluate(new Integer(minutes),
                                   new Integer(pars.requiredMinutes), pars.operator);
    }


    /**
     * Retrieves the parameters as configured in the rule builder.
     *
     * @param parameters
     */
    private ParametersResolved getParameters(Parameters parameters) {
        ParametersResolved ret = new ParametersResolved();
    	ret.requiredMinutes = parameters.getIntegerParameter(PARAM_WORK_MINUTES, 0);
        ret.operator = parameters.getParameter(PARAM_OPERATOR, RuleHelper.BIGGEREQ);
        ret.tcodeNameList = parameters.getParameter(PARAM_TCODE_NAME_LIST, null);
        ret.tcodeInclusive = Boolean.valueOf(parameters.getParameter(PARAM_TCODE_INCLUSIVE,"true")).booleanValue();
        ret.htypeNameList = parameters.getParameter(PARAM_HTYPE_NAME_LIST, null);
        ret.htypeInclusive = Boolean.valueOf(parameters.getParameter(PARAM_HTYPE_INCLUSIVE,"true")).booleanValue();
        ret.continueTcodeNameList = parameters.getParameter(PARAM_CONTINUE_TCODE_NAME_LIST, null);
        ret.timeSpan = parameters.getParameter(PARAM_TIME_SPAN, CHOICE_ALL);

        return ret;
    }


    /**
     * Returns the minutes of the max continuous segment found in the work details.
     *
     * @param wbData
     * @return
     */
    private int getMinutesAll(WBData wbData, ParametersResolved pars) {
        return getMinutesRange(wbData, wbData.getMinStartTime(null, false),
                               wbData.getMaxEndTime(null, false),
                               pars);
    }


    /**
     * Returns the minutes of the max continuous segment found within a shift.
     * Accounts for multiple shifts.
     *
     * @param wbData
     * @return
     */
    private int getMinutesWithinSchedule(WBData wbData, ParametersResolved pars) {

    	int minutes = 0;
    	EmployeeScheduleData scheduleData = wbData.getEmployeeScheduleData();

    	if (scheduleData != null) {

    		for (int i=1; i <= scheduleData.retrieveNumberOfScheduledShiftIndexes(); i++) {

	            minutes = Math.max(minutes,
	            			getMinutesRange(wbData,
	            							scheduleData.getEmpskdActStartTime(i),
											scheduleData.getEmpskdActEndTime(i), pars)
										);
    		}
    	}

    	return minutes;
    }


    /**
     * Returns the minutes of the max continuous segment found within work that does not fall
     * within a shift.  Accounts for multiple shifts.
     *
     * @param wbData
     * @return
     */
    private int getMinutesOutsideSchedule(WBData wbData, ParametersResolved pars) {

    	int minutes = 0;
    	Map nonShiftStartEndTimes = ScheduleHelper.getNonShiftStartEndTimesSorted(wbData.getEmployeeScheduleData(),
    																wbData.getMinStartTime(null, false),
																	wbData.getMaxEndTime(null, false));
        Iterator i = null;
        StartEndTime nonShiftStartEnd = null;

        if (nonShiftStartEndTimes != null) {

    		i = nonShiftStartEndTimes.values().iterator();

    		while (i.hasNext()) {

    			nonShiftStartEnd = (StartEndTime) i.next();

                minutes = Math.max(minutes,
                                   getMinutesRange(wbData,
                    nonShiftStartEnd.getStartTime(),
                    nonShiftStartEnd.getEndTime(), pars));
    		}
        }


        return minutes;
    }

    /**
     * Returns the minutes of the max continuous segment found within the time range.
     *
     * @param wbData
     * @param startTime
     * @param endTime
     * @return
     */
    private int getMinutesRange(WBData wbData, Date startTime, Date endTime,
                                ParametersResolved pars) {

    	int currentContinuousMinutes = 0;
    	int maxContinuousMinutes = 0;
        WorkDetailList wdl = wbData.getRuleData().getWorkDetails();

        wdl.splitAt(startTime);
        wdl.splitAt(endTime);

        for (int i = 0; i < wdl.size(); i++) {

        	// Only consider the work details within the given time range.
        	if (DateHelper.compare(wbData.getWrkdStartTime(i), startTime) >= 0
        			&& DateHelper.compare(wbData.getWrkdEndTime(i), endTime) <= 0) {

	        	// If this is one of our time codes then accumulate the minutes.
	        	if ( (RuleHelper.isCodeInList(pars.htypeNameList, wdl.getWorkDetail(i).getWrkdHtypeName()) == pars.htypeInclusive)
	                && (RuleHelper.isCodeInList(pars.tcodeNameList, wdl.getWorkDetail(i).getWrkdTcodeName()) == pars.tcodeInclusive))
	            {
	        		currentContinuousMinutes += wdl.getWorkDetail(i).getWrkdMinutes();

	        	// else if this is not in our continue time code, then reset our counter.
	            } else if (!RuleHelper.isCodeInList(pars.continueTcodeNameList, wdl.getWorkDetail(i).getWrkdTcodeName())) {
	            	maxContinuousMinutes = Math.max(currentContinuousMinutes, maxContinuousMinutes);
	            	currentContinuousMinutes = 0;
	            }
        	}
        }

        // After the last record, check the current count vs the largest so far.
        maxContinuousMinutes = Math.max(currentContinuousMinutes, maxContinuousMinutes);

        return maxContinuousMinutes;
    }


    /**
     * Returns the List of parameters that appear in the rule builder.
     */
    public List getParameterInfo(DBConnection conn) {

    	List result = new ArrayList();

        result.add(new RuleParameterInfo(PARAM_WORK_MINUTES, RuleParameterInfo.INT_TYPE, false));

        RuleParameterInfo rpi = new RuleParameterInfo(PARAM_OPERATOR, RuleParameterInfo.CHOICE_TYPE, false);
        rpi.addChoice(RuleHelper.EQ);
        rpi.addChoice(RuleHelper.LESS);
        rpi.addChoice(RuleHelper.BIGGER);
        rpi.addChoice(RuleHelper.LESSEQ);
        rpi.addChoice(RuleHelper.BIGGEREQ);
        rpi.addChoice(RuleHelper.NOTEQ1);
        result.add(rpi);

        result.add(new RuleParameterInfo(PARAM_TCODE_NAME_LIST, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_TCODE_INCLUSIVE, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_HTYPE_NAME_LIST, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_HTYPE_INCLUSIVE, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_CONTINUE_TCODE_NAME_LIST, RuleParameterInfo.STRING_TYPE, true));

        rpi = new RuleParameterInfo(PARAM_TIME_SPAN, RuleParameterInfo.CHOICE_TYPE, false);
        rpi.addChoice(CHOICE_ALL);
        rpi.addChoice(CHOICE_WITHIN_SCHEDULE);
        rpi.addChoice(CHOICE_OUTSIDE_SCHEDULE);
        result.add(rpi);

        return result;
    }


    /**
     * Returns the component name displayed in the rule builder.
     */
    public String getComponentName() {
        return "WBIAG: Worked N Minutes Continuous Condition";
    }

    class ParametersResolved {
        int requiredMinutes = 0;
        String operator = null;
        String tcodeNameList = null;
        boolean tcodeInclusive = false;
        String htypeNameList = null;
        boolean htypeInclusive = false;
        String continueTcodeNameList = null;
        String timeSpan = null;

    }
}
