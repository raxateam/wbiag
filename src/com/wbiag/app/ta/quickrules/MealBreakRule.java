package com.wbiag.app.ta.quickrules;

import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.*;

import com.workbrain.util.*;
import java.util.*;

import org.apache.log4j.Priority;

/**
 *  Title:        Meal Break Rule
 *  Description:  Raise exception error and/or insert premium if employee is not schedule for their necessary break.
 *  Copyright:    Copyright (c) 2006
 *  Company:      Workbrain Inc
 *
 *@version    1.0
 *@deprecated As of 5.0.2.0, use core classes 
 */
public class MealBreakRule extends Rule{

    /*Intialize Logger*/
	private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(MealBreakRule.class);

	/*Parameters*/
    public final static String PARAM_MIN_LENGTH_OF_SHIFT = "MinimumLengthOfShift";
	public final static String PARAM_CONSEC_MINUTES = "ConsecutiveWorkedMinutes";
	public final static String PARAM_INCLUDE_BREAKS_IN_WORK = "IncludeBreaksInWorkedMinutes";
	public final static String PARAM_NEW_PERIOD_AFTER_BREAK = "NewPeriodAfterBreak";
	public final static String PARAM_VALID_WORKED_TIME_CODES = "ValidWorkedTimeCodes";
	public final static String PARAM_VALID_WORKED_HOUR_TYPE = "ValidWorkedHourType";
    public final static String PARAM_RELATIVE_TO_ACTUAL_TIME = "RelativeToActualTime";
	public final static String PARAM_WORKED_SHIFT_START_FROM = "WorkedShiftStartFrom";
	public final static String PARAM_WORKED_SHIFT_START_TO = "WorkedShiftStartTo";
	public final static String PARAM_WORKED_SHIFT_END_FROM = "WorkedShiftEndFrom";
	public final static String PARAM_WORKED_SHIFT_END_TO = "WorkedShiftEndTo";
    public final static String PARAM_BREAK_FROM = "MinimumBreakStartTime";
    public final static String PARAM_BREAK_TO = "MaximumBreakEndTime";
    public final static String PARAM_DURATION_OF_BREAK = "DurationOfBreak";
    public final static String PARAM_VALID_BREAK_TIME_CODE = "ValidBreakTimeCode";
    public final static String PARAM_VALID_BREAK_HOUR_TYPE = "ValidBreakHourType";
    public final static String PARAM_SHIFT_DIVIDER_TIME_CODES = "ShiftDividerTimeCodes";
    public final static String PARAM_SHIFT_DIVIDER_INCLUSIVE = "ShiftDividerInclusive";
    public final static String PARAM_PREMIUM_TCODE = "PremiumTimeCode";
    public final static String PARAM_PREMIUM_HOURTYPE = "PremiumHourType";
    public final static String PARAM_PREMIUM_MINUTES = "PremiumMinutes";
    public final static String PARAM_PREMIUM_RATE = "PremiumRate";
    public final static String PARAM_MAX_PREMIUM_ALLOWED = "MaxPremiumAllowed";
    public final static String PARAM_LAB_JOB_SRC = "PremiumJobSource";
    public final static String PARAM_LAB_JOB_NAME = "PremiumJobName";
    public final static String PARAM_LAB_DEPT_SRC = "PremiumDeptSource";
    public final static String PARAM_LAB_DEPT_NAME = "PremiumDeptName";
    public final static String PARAM_LAB_PROJ_SRC = "PremiumProjSource";
    public final static String PARAM_LAB_PROJ_NAME = "PremiumProjName";
    public final static String PARAM_LAB_DOCK_SRC = "PremiumDockSource";
    public final static String PARAM_LAB_DOCK_NAME = "PremiumDockName";
    public final static String PARAM_LAB_TEAM_SRC = "PremiumTeamSource";
    public final static String PARAM_LAB_TEAM_NAME = "PremiumTeamName";

    /* Values for boolean choice parameters */
    public final static String VALUE_TRUE = "True";
    public final static String VALUE_FALSE = "False";
    /* Values for labor metric source choice parameters */
    public final static String VALUE_EDL = "Default Labor";
    public final static String VALUE_DEF = "Defined";
    public final static String VALUE_WORKED = "Work Details";

    /* Values passed in the dateList to indicate the state of work detail
     * traversal */
    private final static Integer VALID_WP = new Integer(0);
    private final static Integer INVALID_WP = new Integer(-1);
    private final static Integer END_WP = new Integer(1);

	/*Constants*/
	public final static String DEFAULT_WORKED_SHIFT_TIME = "-1";
	public final static String DEFAULT_MINS = "0";
	public final static int ONE_DAY = 1440;



    public MealBreakRule() {
    }

	/*Set up parameters for rule*/
    public List getParameterInfo(DBConnection conn) {
        List result = new ArrayList();
        result.add(getMinutesParam(PARAM_MIN_LENGTH_OF_SHIFT));
        result.add(getMinutesParam(PARAM_CONSEC_MINUTES));

        result.add(getBooleanChoice(PARAM_INCLUDE_BREAKS_IN_WORK));
        result.add(getBooleanChoice(PARAM_NEW_PERIOD_AFTER_BREAK));

		result.add(new RuleParameterInfo(PARAM_VALID_WORKED_TIME_CODES, RuleParameterInfo.STRING_TYPE));
        result.add(new RuleParameterInfo(PARAM_VALID_WORKED_HOUR_TYPE, RuleParameterInfo.STRING_TYPE));

        result.add(getBooleanChoice(PARAM_RELATIVE_TO_ACTUAL_TIME));

        result.add(getIntervalTime(PARAM_WORKED_SHIFT_START_FROM));
        result.add(getIntervalTime(PARAM_WORKED_SHIFT_START_TO));
        result.add(getIntervalTime(PARAM_WORKED_SHIFT_END_FROM));
        result.add(getIntervalTime(PARAM_WORKED_SHIFT_END_TO));

        result.add(getIntervalTime(PARAM_BREAK_FROM));
        result.add(getIntervalTime(PARAM_BREAK_TO));

        result.add(new RuleParameterInfo(PARAM_DURATION_OF_BREAK, RuleParameterInfo.INT_TYPE));
        result.add(new RuleParameterInfo(PARAM_VALID_BREAK_TIME_CODE, RuleParameterInfo.STRING_TYPE));
        result.add(new RuleParameterInfo(PARAM_VALID_BREAK_HOUR_TYPE, RuleParameterInfo.STRING_TYPE));

        result.add(new RuleParameterInfo(PARAM_SHIFT_DIVIDER_TIME_CODES, RuleParameterInfo.STRING_TYPE));
        result.add(getBooleanChoice(PARAM_SHIFT_DIVIDER_INCLUSIVE));

        result.add(new RuleParameterInfo(PARAM_PREMIUM_TCODE, RuleParameterInfo.STRING_TYPE));
        result.add(new RuleParameterInfo(PARAM_PREMIUM_HOURTYPE, RuleParameterInfo.STRING_TYPE));
        result.add(new RuleParameterInfo(PARAM_PREMIUM_MINUTES, RuleParameterInfo.INT_TYPE));
        result.add(new RuleParameterInfo(PARAM_PREMIUM_RATE, RuleParameterInfo.STRING_TYPE));
        result.add(new RuleParameterInfo(PARAM_MAX_PREMIUM_ALLOWED, RuleParameterInfo.INT_TYPE));

        result.add(getSourceChoice(PARAM_LAB_JOB_SRC));
        result.add(new RuleParameterInfo(PARAM_LAB_JOB_NAME, RuleParameterInfo.STRING_TYPE));
        result.add(getSourceChoice(PARAM_LAB_DEPT_SRC));
        result.add(new RuleParameterInfo(PARAM_LAB_DEPT_NAME, RuleParameterInfo.STRING_TYPE));
        result.add(getSourceChoice(PARAM_LAB_PROJ_SRC));
        result.add(new RuleParameterInfo(PARAM_LAB_PROJ_NAME, RuleParameterInfo.STRING_TYPE));
        result.add(getSourceChoice(PARAM_LAB_DOCK_SRC));
        result.add(new RuleParameterInfo(PARAM_LAB_DOCK_NAME, RuleParameterInfo.STRING_TYPE));
        result.add(getSourceChoice(PARAM_LAB_TEAM_SRC));
        result.add(new RuleParameterInfo(PARAM_LAB_TEAM_NAME, RuleParameterInfo.STRING_TYPE));

        return result;
    }

    /**
     * Creates a new boolean choice parameter
     * @param varName
     * @return
     */
    private RuleParameterInfo getBooleanChoice(String varName) {
		RuleParameterInfo choice = new RuleParameterInfo(
				varName, RuleParameterInfo.CHOICE_TYPE);
		choice.addChoice(VALUE_TRUE);
		choice.addChoice(VALUE_FALSE);
		choice.setDefaultValue(VALUE_FALSE);
        return choice;
    }

    /**
     * Creates a new source choice parameter
     * @param varName
     * @return
     */
    private RuleParameterInfo getSourceChoice(String varName) {
		RuleParameterInfo choice = new RuleParameterInfo(
				varName, RuleParameterInfo.CHOICE_TYPE);
		choice.addChoice(VALUE_EDL);
		choice.addChoice(VALUE_DEF);
		choice.addChoice(VALUE_WORKED);
		choice.setDefaultValue(VALUE_EDL);
        return choice;
    }

    /**
     * Creates a new Interval Time type parameter
     * @param varName
     * @return
     */
    private RuleParameterInfo getIntervalTime(String varName) {
		RuleParameterInfo intTime = new RuleParameterInfo(
				varName, RuleParameterInfo.INT_TYPE);
		intTime.setDefaultValue(DEFAULT_WORKED_SHIFT_TIME);
		return intTime;
    }

    /**
     * Creates a new Minutes type parmaeter
     * @param varName
     * @return
     */
    private RuleParameterInfo getMinutesParam(String varName) {
		RuleParameterInfo minutes = new RuleParameterInfo(varName, RuleParameterInfo.INT_TYPE);
		minutes.setDefaultValue(DEFAULT_MINS);
		return minutes;
    }

    /**
     * Retrieves the values of the parameter set in the Rule Editor
	 * @param parameters
	 */
	private ParametersResolved getParameters(Parameters parameters) {
        ParametersResolved ret = new ParametersResolved();
		//get parameter values
        ret.minLengthOfShift = parameters.getIntegerParameter(PARAM_MIN_LENGTH_OF_SHIFT);
		ret.consecutiveWorkedMinutes = parameters.getIntegerParameter(PARAM_CONSEC_MINUTES);

		ret.inclBrks = VALUE_TRUE.equals(parameters.getParameter(PARAM_INCLUDE_BREAKS_IN_WORK));
		ret.newPeriod = VALUE_TRUE.equals(parameters.getParameter(PARAM_NEW_PERIOD_AFTER_BREAK));

		ret.validWorkedTimeCodes = parameters.getParameter(PARAM_VALID_WORKED_TIME_CODES);
        ret.validWorkedHourTypes = parameters.getParameter(PARAM_VALID_WORKED_HOUR_TYPE);
		ret.relativetoActualTime = VALUE_TRUE.equals(parameters.getParameter(PARAM_RELATIVE_TO_ACTUAL_TIME));

		ret.shiftStartFrom = parameters.getIntegerParameter(PARAM_WORKED_SHIFT_START_FROM);
		ret.shiftStartTo = parameters.getIntegerParameter(PARAM_WORKED_SHIFT_START_TO);
		ret.shiftEndFrom = parameters.getIntegerParameter(PARAM_WORKED_SHIFT_END_FROM);
		ret.shiftEndTo = parameters.getIntegerParameter(PARAM_WORKED_SHIFT_END_TO);

		ret.breakFrom = parameters.getIntegerParameter(PARAM_BREAK_FROM,-1);
		ret.breakTo = parameters.getIntegerParameter(PARAM_BREAK_TO,-1);

        ret.durationOfBreak = parameters.getIntegerParameter(PARAM_DURATION_OF_BREAK);
        ret.validBreakTimeCode = parameters.getParameter(PARAM_VALID_BREAK_TIME_CODE);
        ret.validBreakHourType = parameters.getParameter(PARAM_VALID_BREAK_HOUR_TYPE);

        ret.shiftDivTimeCodes = parameters.getParameter(PARAM_SHIFT_DIVIDER_TIME_CODES);
        ret.shiftDivInclusive = VALUE_TRUE.equals(parameters.getParameter(PARAM_SHIFT_DIVIDER_INCLUSIVE));

        ret.premiumTCodes = parameters.getParameter(PARAM_PREMIUM_TCODE);
        ret.premiumHType = parameters.getParameter(PARAM_PREMIUM_HOURTYPE);
        ret.premiumMinutes = parameters.getIntegerParameter(PARAM_PREMIUM_MINUTES);
        ret.premiumRate = parameters.getDoubleParameter(PARAM_PREMIUM_RATE, -1.0);
        ret.maxPremAllowed = parameters.getIntegerParameter(PARAM_MAX_PREMIUM_ALLOWED, -1);

        ret.premJobSource = parameters.getParameter(PARAM_LAB_JOB_SRC);
        ret.premJobDef = parameters.getParameter(PARAM_LAB_JOB_NAME);
        ret.premDeptSource = parameters.getParameter(PARAM_LAB_DEPT_SRC);
        ret.premDeptDef = parameters.getParameter(PARAM_LAB_DEPT_NAME);
        ret.premProjSource = parameters.getParameter(PARAM_LAB_PROJ_SRC);
        ret.premProjDef = parameters.getParameter(PARAM_LAB_PROJ_NAME);
        ret.premDockSource = parameters.getParameter(PARAM_LAB_DOCK_SRC);
        ret.premDockDef = parameters.getParameter(PARAM_LAB_DOCK_NAME);
        ret.premTeamSource = parameters.getParameter(PARAM_LAB_TEAM_SRC);
        ret.premTeamDef = parameters.getParameter(PARAM_LAB_TEAM_NAME);

        /* calculate values derived from the parameters for use throughout the
         * code */
        ret.useJobDef = VALUE_DEF.equals(ret.premJobSource);
        ret.useJobWrk = VALUE_WORKED.equals(ret.premJobSource);
        ret.useDeptDef = VALUE_DEF.equals(ret.premDeptSource);
        ret.useDeptWrk = VALUE_WORKED.equals(ret.premDeptSource);
        ret.useProjDef = VALUE_DEF.equals(ret.premProjSource);
        ret.useProjWrk = VALUE_WORKED.equals(ret.premProjSource);
        ret.useDockDef = VALUE_DEF.equals(ret.premDockSource);
        ret.useDockWrk = VALUE_WORKED.equals(ret.premDockSource);
        ret.useTeamDef = VALUE_DEF.equals(ret.premTeamSource);
        ret.useTeamWrk = VALUE_WORKED.equals(ret.premTeamSource);

        ret.insertPremium = !StringHelper.isEmpty(ret.premiumTCodes)
        && !StringHelper.isEmpty(ret.premiumHType) && ret.premiumMinutes > 0;

		massageIntervals(ret);

        return ret;
	}

    public void execute(WBData wbData, Parameters parameters)
    throws java.lang.Exception {
        WorkDetailList wdl = wbData.getRuleData().getWorkDetails();
		Date startTime = null;

		if (wdl.size() <=0) return;

		//Get Parameter Information
		ParametersResolved pars = getParameters(parameters);

		/* Create list of time codes and hour types that are valid at the
		 * start of a work period */
		pars.startTimeCodes = pars.validWorkedTimeCodes;
		pars.startHourTypes = pars.validWorkedHourTypes;

		if (!StringHelper.isEmpty(pars.validBreakTimeCode))
			pars.startTimeCodes += "," + pars.validBreakTimeCode;

		if (!StringHelper.isEmpty(pars.validBreakHourType))
			pars.startHourTypes += "," + pars.validBreakHourType;

		if (logger.isDebugEnabled()) logger.debug("Tcodes " + pars.startTimeCodes);
		if (logger.isDebugEnabled()) logger.debug("Hcodes " + pars.startHourTypes);


		/*Set intial start time  - get first VALID Work Detail*/
		int startWorkDetailIndex = wdl.getFirstRecordIndex(pars.startTimeCodes,
            true, pars.startHourTypes, true);
		if (logger.isDebugEnabled()) logger.debug("Retrieve Parameter - FINE " + startWorkDetailIndex);

		if (startWorkDetailIndex == -1) return;
		startTime = wdl.getWorkDetail(startWorkDetailIndex).getWrkdStartTime();
		if (logger.isDebugEnabled()) logger.debug("Start Work Detail Index; " + startWorkDetailIndex);
		if (logger.isDebugEnabled()) logger.debug("startTime: " + startTime);

		/* Master loop, check all valid work periods for violations of meal
		 * break parameters */
		int curIter = 0;
		while (startTime != null && curIter < 100) {
			startTime = checkForMealBreak(
					getWorkPeriodFrom(startTime, wdl, pars),
					wdl,
					wbData, pars);
			curIter++;
		}

		if (curIter == 100) {
			if (logger.isEnabledFor(Priority.WARN)) {
				logger.warn("Reached max iterations for work details: " + wdl);
			}
		}
    }


    /**
     * Returns the shift start and end time as well as the period in which the
     * break must occur.  A boolean flag can also determine if the period should
     * be evaluated or if it should be skipped.
     * @param startTime
     * @return
     */
    public WorkPeriod getWorkPeriodFrom(Date startTime, WorkDetailList wdl,
                                        ParametersResolved pars) {
    	//Time from which to calculate all "minutes" values
    	Date today = DateHelper.getUnitDay("",false,startTime);
    	WorkPeriod nextInterval = new WorkPeriod();
        Date endTime = null;
        //The shift length we're looking for, whether consecutive or not
        int workPeriodLength = pars.minLengthOfShift != 0
        ? pars.minLengthOfShift : pars.consecutiveWorkedMinutes;
        /* A list of the minutes into the shift we're looking for (used to
         * define a smaller "break period" within the work period where we
         * can find our breaks)
         */
        List minutesList = new ArrayList();
        if (pars.relativetoActualTime && pars.breakFrom != pars.breakTo) {
        	minutesList.add(new Integer(pars.breakFrom));
        	minutesList.add(new Integer(pars.breakTo));
        }
        minutesList.add(new Integer(workPeriodLength));
        /* Get the times in the work detail list that correspond to the
         * relative minutes (break from, break to, length of shift) as well as
         * an indicator for where the rule last left off.
         */
    	List dateList = getWorkingTimeNMinutesAfterStart(minutesList ,
    					startTime, pars.minLengthOfShift != 0, wdl, pars);
    	if (dateList != null) {
    		//Retrieve the state of the last scan (valid, invalid, end)
    		Integer isValid = (Integer)dateList.get(dateList.size() - 1);
    		//Termination Signal
    		if (END_WP.equals(isValid)) return null;
    		//Retrieve the end time (always the second to last item in the list)
    		endTime = (Date)dateList.get(dateList.size() - 2);
    		//Ensure the last check brought back a period of sufficient length
    		nextInterval.addProcessCondition(VALID_WP.equals(isValid));
    		//Set the outer boundaries of the work period being scanned
			nextInterval.setWorkPeriodStart(startTime);
			nextInterval.setWorkPeriodEnd(endTime);
			//Get the number of minutes into the day for the start and end time
			int startMinutes = (int) DateHelper.getMinutesBetween(
					startTime,today);
			int endMinutes = (int) DateHelper.getMinutesBetween(endTime,today);
			//Ensure the start time is between the end interval parameters
			nextInterval.addProcessCondition(isMinuteValueBetween(
					startMinutes, pars.shiftStartFrom, pars.shiftStartTo));
			//Ensure the end time is between the start interval parameters
			nextInterval.addProcessCondition(isMinuteValueBetween(
					endMinutes, pars.shiftEndFrom, pars.shiftEndTo));

			Date intStart = null;
			Date intEnd = null;
			int intStartMinutes = 0;
			int intEndMinutes = 0;
			if (pars.brkFromND) {
				/* If a break from interval parameter was not set, use the work
				 * period start */
				intStart = startTime;
			} else if (pars.relativetoActualTime) {
				//If relative, use the value from the date list
				intStart = (Date)dateList.get(0);
			} else {
				//If not relative, use the hard value from the parameter
				intStart = DateHelper.addMinutes(today,pars.breakFrom);
			}
			if (pars.brkToND) {
				/* If a break to interval parameter was not set, use the work
				 * period end */
				intEnd = endTime;
			} else if (pars.relativetoActualTime) {
				//If relative, use the value from the date list
				intEnd = (Date)dateList.get(1);
			} else {
				//If not relative, use the hard value from the parameter
				intEnd = DateHelper.addMinutes(today,pars.breakTo);
			}
			//Set the interval in the work period to check for the break
			nextInterval.setBreakPeriodStart(intStart);
			nextInterval.setBreakPeriodEnd(intEnd);
			intStartMinutes = (int) DateHelper.getMinutesBetween(
					intStart, today);
			intEndMinutes = (int) DateHelper.getMinutesBetween(intEnd, today);
			/* Ensure the break period occurs between the start and end of the
			 * work period */
			nextInterval.addProcessCondition(
					pars.relativetoActualTime || isMinuteValueBetween(
							intStartMinutes, startMinutes, endMinutes) &&
					isMinuteValueBetween(intEndMinutes, startMinutes, endMinutes));
    	}

    	return nextInterval;
    }

    /**
     * Takes a list of "minutes from start" and finds the associated times
     * within the work details.  Ignores minutes from work details that don't
     * valid work time codes and valid work hour types.  If parameter is set,
     * breaks can be included as well.  Shift Divisor (gap) time codes will stop
     * the counting of minutes.
     *
     * @param minutesList
     * @param startTime
     * @param minimum
     * @param wdl
     * @return
     */
    public List getWorkingTimeNMinutesAfterStart(
    		List minutesList,
    		Date startTime,
    		boolean minimum,
    		WorkDetailList wdl,
            ParametersResolved pars) {
    	List dateList = new ArrayList();
    	Iterator minItr = minutesList.iterator();
    	int minutes = ((Integer) minItr.next()).intValue();
    	Date endTime = null;
    	/* Get the index of the next work detail after the given start time
    	 * having a valid start of shift time code and hour type.
    	 * Split if the start time lies inside the work detail */
    	int curIndex = wdl.getFirstRecordIndex(
    			startTime,
    			endTime,
    			pars.startTimeCodes,
    			true,
    			pars.startHourTypes,
    			true,
    			true);
    	int curWorkedMinutes = 0;
    	WorkDetailData curWD = null;
    	String curTcode = null;
    	String curHtype = null;
    	boolean gapFound = false;
    	boolean endFound = false;
    	while (!gapFound && !endFound && (minimum || minItr.hasNext() || curWorkedMinutes < minutes)) {
    		if (wdl.size() <= curIndex || curIndex == -1) {
    			/* Reached the end of the day's work detail list without reaching
    			 * the required minimum number of minutes */
    			endFound = true;
    		} else {
    			// Get the next work detail
        		curWD = wdl.getWorkDetail(curIndex);
    			curTcode = curWD.getWrkdTcodeName();
    			curHtype = curWD.getWrkdHtypeName();
    			if (RuleHelper.isCodeInList(pars.shiftDivTimeCodes, curTcode) == pars.shiftDivInclusive) {
    				//We've reached a gap between shifts
    				gapFound = true;
    			} else if ((RuleHelper.isCodeInList(pars.validWorkedTimeCodes, curTcode) &&
    					RuleHelper.isCodeInList(pars.validWorkedHourTypes, curHtype)) ||
    					(pars.validBreakTimeCode.equals(curTcode) &&
    							pars.validBreakHourType.equals(curHtype) &&
    							pars.inclBrks)) {
    				/* If the current work detail's time code and hour type are
    				 * for valid work, or if breaks are considered work and it
    				 * is a valid break then:
    				 * - Add the work detail's minutes to the running work
    				 * period length total
    				 * - Set the current end time to the work detail's end time
    				 * */
					curWorkedMinutes += curWD.getWrkdMinutes();
    				endTime = curWD.getWrkdEndTime();
    			}
    		}
    		while (minItr.hasNext() && curWorkedMinutes >= minutes) {
    			/* While there are more minutes and the current minutes are less
    			 * than the running work period length total:
    			 * - Add to the list, the end time minus the difference between
    			 * the current minutes and the worked minutes to get the actual
    			 * time those minutes represent in the work period
    			 * - Get the next minutes value from the list
    			 */
    			dateList.add(DateHelper.addMinutes(endTime, minutes - curWorkedMinutes));
    			minutes = ((Integer) minItr.next()).intValue();
    		}
    		curIndex++;
    	}
    	if (minimum) {
    		//If looking for the minimum length of a shift
    		//Add the last work detail's end time to the list
			dateList.add(endTime);
			/* Add a state to the list based on the rule's criteria for valid
			 * work periods */
    		if ((gapFound || endFound) && minutes < curWorkedMinutes) {
    			dateList.add(VALID_WP);
    		} else {
        		dateList.add(endFound ? END_WP : INVALID_WP);
    		}
    	} else {
    		//If looking for a defined consecutive length
    		/* Add the last work detail's end time minus the difference between
    		 * the current minutes and the worked minutes to get the actual time
    		 * the end minutes represent in the work period
    		 */
			dateList.add(DateHelper.addMinutes(endTime,
					minutes - curWorkedMinutes));
			/* Add a state to the list based on the rule's criteria for valid
			 * work periods */
    		if (minutes <= curWorkedMinutes) {
    			dateList.add(VALID_WP);
    		} else {
    			dateList.add(endFound ? END_WP : INVALID_WP);
    		}
    	}
    	return dateList;
    }

    /**
     * Checks for a break matching the rule parameter's criteria with the break
     * period of the WorkPeriod (prd) parameter.
     * @param prd
     * @param wdl
     * @param wbData
     * @return
     */
    public Date checkForMealBreak(WorkPeriod prd, WorkDetailList wdl,
                                  WBData wbData,
                                  ParametersResolved pars) {
    	Date breakEnd = null;
    	Date retVal = null;
    	//If a period was returned
    	if (prd != null) {
    		//If that period was meant to be processed
			Date prdBrkStart = prd.getBreakPeriodStart();
			Date prdBrkEnd = prd.getBreakPeriodEnd();
			Date prdWrkStart = prd.getWorkPeriodStart();
			Date prdWrkEnd = prd.getWorkPeriodEnd();
    		if (prd.getProcess(wbData)) {
    			/* Start with the full work detail record present at the break
    			 * period start time */
    			WorkDetailData curWD = wdl.getWorkDetail(DateHelper.addMinutes(prdBrkStart,1),"",true,"",true);
    			boolean brkFound = false;
    			boolean wasLastBreak = false;
    			boolean isBrkTCHT = false;
    			int consBrkMinutes = 0;
    			int curWrkdMinutes = 0;
    			//Used to track the worked labor metrics for the premium
    			int wrkJobId = 0;
    			int wrkDeptId = 0;
    			int wrkProjId = 0;
    			int wrkDockId = 0;
    			int wrkTeamId = 0;
    			/* While a break has not been found matching the rule criteria
    			 * and either breaks are included and the amount of time from
    			 * the start of the current work detail and the end of the
    			 * period is long enough to make a full break (it would not
    			 * cross the end of the allowable period or be outside of it
    			 * altogether) or breaks are not included and the current
    			 * record is a break and doesn't start after the end of the
    			 * period. */
    			while (!brkFound && curWD != null &&
    					(DateHelper.getMinutesBetween(prdBrkEnd,
    							curWD.getWrkdStartTime()) >=
    								pars.durationOfBreak - consBrkMinutes
    							|| !pars.inclBrks
    							&& !curWD.getWrkdStartTime().after(prdBrkEnd)
    							&& pars.validBreakTimeCode.equals(curWD.getWrkdTcodeName())
    							&& pars.validBreakHourType.equals(curWD.getWrkdHtypeName()))) {
    				/* Store whether the current work detail has the break time
    				 * code and hour type*/
    				isBrkTCHT = pars.validBreakTimeCode.equals(curWD.getWrkdTcodeName())
    				&& pars.validBreakHourType.equals(curWD.getWrkdHtypeName());
    				curWrkdMinutes = curWD.getWrkdMinutes();
    				consBrkMinutes = isBrkTCHT ?
    						consBrkMinutes + curWrkdMinutes: 0;
    				/* Break is found if it matches the time code and hour type
    				 * and either its length is greater than the duration of the
    				 * break or the length of the break plus the length of other
    				 * consecutive breaks is sufficient */
    				brkFound = brkFound || isBrkTCHT
    				&& consBrkMinutes >= pars.durationOfBreak;
    				//Store the current work detail's information
					wrkJobId = curWD.getJobId();
					wrkDeptId = curWD.getDeptId();
					wrkProjId = curWD.getProjId();
					wrkDockId = curWD.getDockId();
					wrkTeamId = curWD.getWbtId();
					curWD = wdl.getNextWorkDetail(curWD.getWrkdStartTime());
    			}
    			if (!brkFound) {
    				if (curWD != null) {
    					/* If the last work detail to be found is not null, use
    					 * its labour metric information*/
    					wrkJobId = curWD.getJobId();
    					wrkDeptId = curWD.getDeptId();
    					wrkProjId = curWD.getProjId();
    					wrkDockId = curWD.getDockId();
    					wrkTeamId = curWD.getWbtId();
    				}
    				/* If we're supposed to insert a premium and we haven't
    				 * reached the configured maximum */
    				if(pars.insertPremium && (pars.maxPremAllowed < 1
    						|| pars.totalPremiums < pars.maxPremAllowed) ) {
    					//Make a work detail containing the premium information
    					WorkDetailData wd =
    						wbData.getRuleData().getWorkPremiums().add(
    								DateHelper.DATE_1900,
    								DateHelper.DATE_1900,
    								wbData.getRuleData().getEmpDefaultLabor(0));
    					wd.setWrkdType(WorkDetailData.PREMIUM_TYPE);
    					wd.setCodeMapper(wbData.getCodeMapper());
    					//Required information
    					wd.setWrkdMinutes(pars.premiumMinutes);
    					wd.setWrkdTcodeName(pars.premiumTCodes);
    					wd.setWrkdHtypeName(pars.premiumHType);
    					//Optional defined rate (defaults to EMP_BASE_RATE)
    					if (pars.premiumRate > 0) {
    						wd.setWrkdRate(pars.premiumRate);
    					} else {
    						wd.setWrkdRate(wbData.getEmpBaseRate());
    					}
    					//Optional defined or worked job (defaults to EDL)
    					if (pars.useJobDef) {
    						wd.setWrkdJobName(pars.premJobDef);
    					} else if (pars.useJobWrk) {
    						wd.setJobId(wrkJobId);
    					}
    					//Optional defined or worked Dept (defaults to EDL)
    					if (pars.useDeptDef) {
    						wd.setWrkdDeptName(pars.premDeptDef);
    					} else if (pars.useDeptWrk) {
    						wd.setDeptId(wrkDeptId);
    					}
    					//Optional defined or worked Project (defaults to EDL)
    					if (pars.useProjDef) {
    						wd.setWrkdProjName(pars.premProjDef);
    					} else if (pars.useProjWrk) {
    						wd.setProjId(wrkProjId);
    					}
    					//Optional defined or worked Docket (defaults to EDL)
    					if (pars.useDockDef) {
    						wd.setWrkdDockName(pars.premDockDef);
    					} else if (pars.useDockWrk) {
    						wd.setDockId(wrkDockId);
    					}
    					//Optional defined or worked Team (defaults to EDL)
    					if (pars.useTeamDef) {
    						wd.setWrkdWbtName(pars.premTeamDef);
    					} else if (pars.useTeamWrk) {
    						wd.setWbtId(wrkTeamId);
    					}
    					pars.totalPremiums++;
    				} else {
    					throw new RuntimeException (
    							"Exception Meal Break Rule: NO Break has been " +
    							"scheduled in the following Work Period:" +
    							"\n- Work Period Start Time: " + prdWrkStart +
    							"\n- Work Period End Time" + prdWrkEnd +
    							"\n- Break Period Start Time: " + prdBrkStart +
    							"\n- Break Period End Time: " + prdBrkEnd);
    				}
    			}
    			//Find the work detail of the first break in the work period
    			int breakIndex = wdl.getFirstRecordIndex (
    					prdWrkStart, DateHelper.addMinutes(prdWrkEnd, 1),
    					pars.validBreakTimeCode, true, pars.validBreakHourType, true,
    					false);
    			if (breakIndex != -1) {
    				//If one exists, store its end time
    				breakEnd = wdl.getWorkDetail(breakIndex).getWrkdEndTime();
    			}
    		}
    		//Whether the last period was processed, look for the next period
    		if (pars.newPeriod && breakEnd != null) {
    			/* If we're using Work Period logic (JCPenny) use the end of the
    			 * first break of the work period if one exists */
    			retVal = breakEnd;
    		} else if (pars.minLengthOfShift != 0) {
    			/* If we're using Minimum Shift Length logic, find the next
    			 * work detail that would be the start of another shift */
    			int nextWDIndex = wdl.getFirstRecordIndex (
    					prdWrkEnd, DateHelper.addMinutes(prdWrkEnd, ONE_DAY),
    					pars.startTimeCodes, true, pars.startHourTypes, true,
    					false);
    			// If another shift isn't found, return null to exit loop
    			if (nextWDIndex == -1) return null;
    			retVal = wdl.getWorkDetail(nextWDIndex).getWrkdStartTime();
    		} else {
    			// Otherwise, start looking from the end of the last work period
    			retVal = prdWrkEnd;
    		}
    	}
    	return retVal;
    }

    /**
     * This method uses "minutes from midnight" to test if a time occurs between
     * a start and end time whether it's today, yesterday or tomorrow
     *
     * Example:
     * -300 = 19:00
     * 720 = 12:00
     * 1260 = 21:00
     *
     * Therefore -300 is between 720 and 1260 for the purposes of this method
     *
     * @param test
     * @param prdStart
     * @param prdEnd
     * @return
     */
    public boolean isMinuteValueBetween(int test, int prdStart, int prdEnd) {
    	return (test >= prdStart && test <= prdEnd)
    	|| (test - ONE_DAY >= prdStart && test - ONE_DAY <= prdEnd)
    	|| (test + ONE_DAY >= prdStart && test + ONE_DAY <= prdEnd);
    }


	public String getComponentName() {
		return "WBIAG: Meal Break Rule";
	}


	/**
	 * This method massages the intervals passed in as parameters for easier
	 * time interval processing
	 */
	private void massageIntervals(ParametersResolved pars) {
		if (pars.shiftStartFrom < 0)  {
			pars.shiftStartFrom = pars.shiftEndFrom > 0 ? pars.shiftEndFrom - ONE_DAY + 1: 0;
		}
		if (pars.shiftEndTo < 0) {
			pars.shiftEndTo = pars.shiftStartTo > 0 ? pars.shiftStartTo + ONE_DAY - 1 : ONE_DAY;
		}
		if (pars.shiftStartTo < 0) {
			pars.shiftStartTo = ONE_DAY;
		}
		if (pars.shiftEndFrom < 0) {
			pars.shiftEndFrom = 0;
		}
		if (pars.shiftStartFrom > pars.shiftStartTo) {
			pars.shiftStartFrom -= ONE_DAY;
		}
		if (pars.shiftEndFrom > pars.shiftEndTo) {
			pars.shiftEndTo += ONE_DAY;
		}
		if (pars.breakFrom < 0) {
			pars.brkFromND = true;
			pars.breakFrom = 0;
		}
		if (pars.breakTo < 0) {
			pars.brkToND = true;
			pars.breakTo = (pars.minLengthOfShift != 0 ? pars.minLengthOfShift : pars.consecutiveWorkedMinutes);
		}
		if (pars.breakFrom > pars.breakTo) {
			pars.breakTo += ONE_DAY;
		}
	}

	/**
	 * This class represents all information that needs to be communicated about
	 * a Work Period from the period creator (getWorkPeriodFrom) to the period
	 * analyzer (checkMealBreak) methods
	 * @author bhacko
	 *
	 */
	private class WorkPeriod {
		private Date brkPrdStart = null;
		private Date brkPrdEnd = null;
		private Date wrkPrdStart = null;
		private Date wrkPrdEnd = null;
		private boolean process = true;

		public Date getBreakPeriodStart() {
			return brkPrdStart;
		}
		public Date getBreakPeriodEnd() {
			return brkPrdEnd;
		}
		public Date getWorkPeriodStart() {
			return wrkPrdStart;
		}
		public Date getWorkPeriodEnd() {
			return wrkPrdEnd;
		}
		public void setBreakPeriodStart(Date val) {
			brkPrdStart = val;
		}
		public void setBreakPeriodEnd(Date val) {
			brkPrdEnd = val;
		}
		public void setWorkPeriodStart(Date val) {
			wrkPrdStart = val;
		}
		public void setWorkPeriodEnd(Date val) {
			wrkPrdEnd = val;
		}
		public boolean getProcess(WBData wbData) {
			/* log a warning if the work period is more than 24 hours */
			if (DateHelper.getMinutesBetween(wrkPrdEnd,wrkPrdStart) > ONE_DAY) {
				if (logger.isEnabledFor(Priority.WARN)) {
					logger.warn("Work period found with length greater than 24 hours; " +
							"EMP_ID: " + wbData.getWrksEmpId() +
							" WRKS_WORK_DATE: " + wbData.getWrksWorkDate());
				}
			}
			return process;
		}
		public void setProcess(boolean proc) {
			process = proc;
		}
		public void addProcessCondition(boolean cond) {
			process = process && cond;
		}
	}

    class ParametersResolved {
        // Rule Parameters.
        int minLengthOfShift = 0;
        int consecutiveWorkedMinutes = 0;
        boolean inclBrks = false;
        boolean newPeriod = false;
        String validWorkedTimeCodes;
        String validWorkedHourTypes;
        boolean relativetoActualTime;
        int shiftStartFrom;
        int shiftStartTo;
        int shiftEndFrom;
        int shiftEndTo;
        int breakFrom;
        int breakTo;
        int durationOfBreak;
        String validBreakTimeCode;
        String validBreakHourType;
        String shiftDivTimeCodes;
        boolean shiftDivInclusive;
        String premiumTCodes;
        String premiumHType;
        int premiumMinutes;
        double premiumRate;
        int maxPremAllowed;
        String premJobSource;
        String premJobDef;
        String premDeptSource;
        String premDeptDef;
        String premProjSource;
        String premProjDef;
        String premDockSource;
        String premDockDef;
        String premTeamSource;
        String premTeamDef;

        /* Values derived from parameters */
        boolean insertPremium;
        boolean useJobDef;
        boolean useDeptDef;
        boolean useProjDef;
        boolean useDockDef;
        boolean useTeamDef;
        boolean useJobWrk;
        boolean useDeptWrk;
        boolean useProjWrk;
        boolean useDockWrk;
        boolean useTeamWrk;
        boolean brkFromND = false;
        boolean brkToND = false;
        int totalPremiums = 0;
        String startTimeCodes;
        String startHourTypes;

    }
}

