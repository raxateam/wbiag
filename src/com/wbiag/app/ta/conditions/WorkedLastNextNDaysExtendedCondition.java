package com.wbiag.app.ta.conditions;

 import com.workbrain.sql.DBConnection;
import com.workbrain.app.ta.ruleengine.*;
import java.util.*;

import org.apache.log4j.Logger;

import com.workbrain.util.DateHelper;
import com.workbrain.app.ta.model.*;


/**
 *  Title:        Has Worked Last/Next N Days Condition
 *  Description:  Checks if the emp has worked at least X minutes
 *                within given timecode and hourtype constraints
 *                for the next/last N "Scheduled" "ScheduleType" days
 *  Copyright:    Copyright (c) 2002
 *  Company:      Workbrain Inc
 *
 *@author     Brian Viveiros
 */

public class WorkedLastNextNDaysExtendedCondition extends Condition {

	private static Logger logger = Logger.getLogger(WorkedLastNextNDaysExtendedCondition.class);

    public final static String PARAM_DAYSTOLOOK = "DaysToLook";
    public final static String PARAM_LAST_NEXT = "LastNext";
    public final static String PARAM_MIN_MINUTES = "MinMinutes";
    public final static String PARAM_SCHEDULED = "Scheduled";
    public final static String PARAM_TCODENAME_LIST = "TcodeNameList";
    public final static String PARAM_TCODE_INCLUSIVE = "TcodeInclusive";
    public final static String PARAM_HTYPENAME_LIST = "HtypeNameList";
    public final static String PARAM_HTYPE_INCLUSIVE = "HtypeInclusive";
    public final static String PARAM_VIOLATION_TIMECODES = "ViolationTimeCodes";
    public final static String PARAM_SCHEDULE_TYPE = "ScheduleType";
    public final static String PARAM_WORK_DETAIL_TYPES_TO_CHECK = "WorkDetailTypesToCheck";
    public final static String PARAM_ONE_OCCURANCE = "OneOccuranceOnly";


    public final static String PARAM_VAL_WD_WORK_DETAILS = "WORK DETAILS";
    public final static String PARAM_VAL_WD_WORK_PREMIUMS = "WORK PREMIUMS";
    public final static String PARAM_VAL_WD_ALL = "ALL";

    public final static String PARAM_VAL_LAST = WBData.WORKED_LAST;
    public final static String PARAM_VAL_NEXT = WBData.WORKED_NEXT;

    public final static String PARAM_VAL_SCHEDULED = WBData.WORKED_SCHEDULED;
    public final static String PARAM_VAL_UNSCHEDULED = WBData.WORKED_UNSCHEDULED;
    public final static String PARAM_VAL_SCHEDULED_ALL = WBData.WORKED_SCHEDULED_ALL;

    public final static String PARAM_VAL_SCHEDULED_ACTUAL = WBData.WORKED_SCHEDULED_ACTUAL;
    public final static String PARAM_VAL_SCHEDULED_DEFAULT = WBData.WORKED_SCHEDULED_DEFAULT;

    public static final int MAX_SEARCH_DAYS = 14;



    public List getParameterInfo( DBConnection conn ) {

    	List result = new ArrayList();

    	result.add(new RuleParameterInfo(PARAM_DAYSTOLOOK, RuleParameterInfo.INT_TYPE, false));

        // **** LAST/NEXT
        RuleParameterInfo rpi = new RuleParameterInfo(PARAM_LAST_NEXT, RuleParameterInfo.CHOICE_TYPE, false);
        rpi.addChoice(PARAM_VAL_LAST);
        rpi.addChoice(PARAM_VAL_NEXT);
        result.add(rpi);
        result.add(new RuleParameterInfo(PARAM_MIN_MINUTES, RuleParameterInfo.INT_TYPE, false));

        // **** SCHEDULED/UNSCHEDULED/ALL
        rpi = new RuleParameterInfo(PARAM_SCHEDULED, RuleParameterInfo.CHOICE_TYPE, false);
        rpi.addChoice(PARAM_VAL_SCHEDULED);
        rpi.addChoice(PARAM_VAL_UNSCHEDULED);
        rpi.addChoice(PARAM_VAL_SCHEDULED_ALL);
        result.add(rpi);

        result.add(new RuleParameterInfo(PARAM_TCODENAME_LIST, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_TCODE_INCLUSIVE, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_HTYPENAME_LIST, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_HTYPE_INCLUSIVE, RuleParameterInfo.STRING_TYPE, true));

        result.add(new RuleParameterInfo(PARAM_VIOLATION_TIMECODES, RuleParameterInfo.STRING_TYPE, true));

        // **** ACTUAL/DEFAULT
        rpi = new RuleParameterInfo(PARAM_SCHEDULE_TYPE, RuleParameterInfo.CHOICE_TYPE, false);
        rpi.addChoice(PARAM_VAL_SCHEDULED_ACTUAL);
        rpi.addChoice(PARAM_VAL_SCHEDULED_DEFAULT);
        result.add(rpi);

        // *** Work Detail Types To Check
        rpi = new RuleParameterInfo(PARAM_WORK_DETAIL_TYPES_TO_CHECK, RuleParameterInfo.CHOICE_TYPE, false);
        rpi.addChoice(PARAM_VAL_WD_WORK_DETAILS);
        rpi.addChoice(PARAM_VAL_WD_WORK_PREMIUMS);
        rpi.addChoice(PARAM_VAL_WD_ALL);
        result.add(rpi);

        result.add(new RuleParameterInfo(PARAM_ONE_OCCURANCE, RuleParameterInfo.STRING_TYPE, true));

        return result;
    }


    public boolean evaluate(WBData wbData, Parameters parameters) throws Exception {

    	// Get the config parameters.
    	ParametersResolved pars = getParameters(wbData, parameters);

    	boolean workedLastNext = false;
        Date retDate = null;
        int intFoundDay = 0;
        int multiple = PARAM_VAL_LAST.equals(pars.lastNext) ? -1 : 1;
        Date datWrkLoop = null;
        int intWrkdMinutes = 0;
        EmployeeScheduleData esd = null;
        boolean chkSchedule = false;

        // This code is taken from wbData.getWorkedNthDayDate(), with a few modifications
        // to account for the additional parameters.

        for (int i = 1; i <= MAX_SEARCH_DAYS; i++) {

        	datWrkLoop = DateHelper.addDays(wbData.getWrksWorkDate(), i * multiple);

        	if (pars.minMinutes == -1){
                pars.minMinutes = wbData.getScheduleDuration(datWrkLoop);
            }

            esd = wbData.getEmployeeScheduleData(datWrkLoop);

            // Check the schedule acc. to scheduled type
            if (pars.scheduled.equals(PARAM_VAL_SCHEDULED)) {
                chkSchedule = pars.scheduleType.equals(PARAM_VAL_SCHEDULED_ACTUAL) ?
                								esd.isEmployeeScheduledActual() :
                								esd.isEmployeeScheduledDefault();

            } else if (pars.scheduled.equals(PARAM_VAL_UNSCHEDULED)) {
                chkSchedule = pars.scheduleType.equals(PARAM_VAL_SCHEDULED_ACTUAL) ?
							                    (!esd.isEmployeeScheduledActual()) :
							                    (!esd.isEmployeeScheduledDefault());
            } else {
                chkSchedule = true;
            }

            // If eligible day
            if (chkSchedule) {

            	// If any time was found with a violation time code,
            	// the condition is false.
            	if (pars.violationTimeCodes != null
            			&& wbData.getMinutesWorkDetailPremiumRange(
								                    datWrkLoop, datWrkLoop , null , null,
								                    pars.violationTimeCodes, true ,
													null , true , pars.detailPremium) > 0) {

            		if (logger.isDebugEnabled()) {
            			logger.debug("violationTimeCodes found on date: " + datWrkLoop);
            		}
            		break;
            	}

            	// Get the number of minutes worked for that date.
	        	intWrkdMinutes = wbData.getMinutesWorkDetailPremiumRange(
								                    datWrkLoop, datWrkLoop , null , null,
								                    pars.tcodeNameList, pars.tcodeInclusive ,
                                                    pars.htypeNameList ,
								                    pars.htypeInclusive , pars.detailPremium);

	        	// Check if the number of minutes worked meets the criteria.
                if (intWrkdMinutes >= pars.minMinutes) {

                	// If we are only looking for one occurance, then
                	// we have found it.
                	if (pars.oneOccuranceOnly) {
                        retDate = datWrkLoop;
                        break;
                	}

                	// Sum the number of occurances.  Once we've found the daysToLook,
                	// the condition is met.
                    intFoundDay = intFoundDay + 1;
                    if (intFoundDay == pars.daysToLook) {
                        retDate = datWrkLoop;
                        break;
                    }

                // Found a day that does not match the criteria.
                } else if (intWrkdMinutes < pars.minMinutes) {
                	if (logger.isDebugEnabled()) {
                		logger.debug("On date: " + datWrkLoop
                					+ ", worked minutes: " + intWrkdMinutes
                					+ ", is less than minMinutes: " + pars.minMinutes);
                	}

                	// If we are looking for one occurance, then keep looking.
                	// Otherwise, we are looking for consecutive occurances.  Therefore,
                	// the condition failed.
                	if (!pars.oneOccuranceOnly) {
                		break;
                	}
                }
            }
        }

        // Output the date for debugging.  If a date was not found, then the
        // condition was not met.
        if (logger.isDebugEnabled()) {
        	logger.debug("Found Date: " + retDate);
        }

        // If the date is null, then the condition was not met.
        workedLastNext = retDate != null;

        return workedLastNext;
    }


    protected ParametersResolved getParameters(WBData wbData,
                                               Parameters parameters) {
    	ParametersResolved ret = new ParametersResolved();
        ret.daysToLook = parameters.getIntegerParameter(PARAM_DAYSTOLOOK);
        ret.lastNext = parameters.getParameter(PARAM_LAST_NEXT);
        ret.minMinutes = parameters.getIntegerParameter(PARAM_MIN_MINUTES,0);
        ret.scheduled = parameters.getParameter(PARAM_SCHEDULED , PARAM_VAL_SCHEDULED);
        ret.tcodeNameList = parameters.getParameter(PARAM_TCODENAME_LIST,null);
        ret.tcodeInclusive = Boolean.valueOf(parameters.getParameter(PARAM_TCODE_INCLUSIVE,"true")).booleanValue();
        ret.htypeNameList = parameters.getParameter(PARAM_HTYPENAME_LIST,null);
        ret.htypeInclusive = Boolean.valueOf(parameters.getParameter(PARAM_HTYPE_INCLUSIVE,"true")).booleanValue();
        ret.violationTimeCodes = parameters.getParameter(PARAM_VIOLATION_TIMECODES, null);
        ret.scheduleType = parameters.getParameter(PARAM_SCHEDULE_TYPE , PARAM_VAL_SCHEDULED_ACTUAL);
        ret.workDetailTypesToCheck = parameters.getParameter(PARAM_WORK_DETAIL_TYPES_TO_CHECK , WorkDetailData.DETAIL_TYPE);
        ret.oneOccuranceOnly = Boolean.valueOf(parameters.getParameter(PARAM_ONE_OCCURANCE,"false")).booleanValue();
        if (ret.workDetailTypesToCheck.equals(PARAM_VAL_WD_WORK_PREMIUMS)) {
            ret.detailPremium = WorkDetailData.PREMIUM_TYPE ;
        }
        else if (ret.workDetailTypesToCheck.equals(PARAM_VAL_WD_ALL)) {
            ret.detailPremium = null;
        }
        else {
            ret.detailPremium = WorkDetailData.DETAIL_TYPE;
        }

        return ret;


    }

    public String getComponentName() {
        return "WBIAG: Worked Last Next N Days Condition";
    }

    public class ParametersResolved {
        // Member variables.
        public int daysToLook = 0;
        public String lastNext = null;
        public int minMinutes = 0;
        public String scheduled = null;
        public String tcodeNameList = null;
        public boolean tcodeInclusive = true;
        public String htypeNameList = null;
        public boolean htypeInclusive = true;
        public String violationTimeCodes = null;
        public String scheduleType = null;
        public String workDetailTypesToCheck = null;
        public boolean oneOccuranceOnly = false;
        public String detailPremium = null;

    }
}
