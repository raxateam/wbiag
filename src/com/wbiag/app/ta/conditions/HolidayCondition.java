package com.wbiag.app.ta.conditions ;

import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;
import java.util.*;

/**
 * Very similar to the core HolidayCondition, except that it handles consecutive holidays.
*/
public class HolidayCondition extends com.workbrain.app.ta.ruleengine.Condition {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(HolidayCondition.class);

    public final static String PARAM_WORKED_TIME_CODES = "WorkedTimeCodes";
    public final static String PARAM_VIOLATION_TIME_CODES = "ViolationTimeCodes";
    public final static String PARAM_IGNORE_WEEKENDS = "IgnoreWeekends";

    public final static String IGNORE_WEEKENDS_TRUE = "TRUE";
    public final static String IGNORE_WEEKENDS_FALSE = "FALSE";

    public final static int MAX_LOOKBACK_DAYS = 365;

    /** @see Condition */
    public List getParameterInfo(DBConnection conn) {

        List result = new ArrayList();

        result.add(new RuleParameterInfo(PARAM_WORKED_TIME_CODES, RuleParameterInfo.STRING_TYPE, false));
        result.add(new RuleParameterInfo(PARAM_VIOLATION_TIME_CODES, RuleParameterInfo.STRING_TYPE, false));

        RuleParameterInfo ignoreWeekendsOptions = new RuleParameterInfo(
                PARAM_IGNORE_WEEKENDS, RuleParameterInfo.CHOICE_TYPE, false);
        ignoreWeekendsOptions.addChoice(IGNORE_WEEKENDS_TRUE);
        ignoreWeekendsOptions.addChoice(IGNORE_WEEKENDS_FALSE);
        result.add(ignoreWeekendsOptions);

        return result;
    }

    /** @see Condition */
    public String getComponentName() {
        return "WBIAG: Holiday Condition";
    }
    /** @see Condition */
    public String getDescription() {
        return "Applies if it is a holiday that day, none of the violation codes are present and the sheduled shift before and after was worked.";
    }
    /** @see Condition */
    public String getComponentUI() {
        return null;
    }

    /**
     * Uses scheduled start and end times to determine shift duration
     * and checks premium records as well as work detail records.
     * @throws RuleEngineException for invalid passed parameters
    */
    public boolean evaluate(WBData wbData, Parameters parameters) throws Exception{

        // parse and validate passed parameters
        String workedTimeCodes = validateParameter(parameters, PARAM_WORKED_TIME_CODES);
        String violationTimeCodes = validateParameter(parameters, PARAM_VIOLATION_TIME_CODES);
        String ignoreWeekendsParam = validateParameter(parameters, PARAM_IGNORE_WEEKENDS);
        boolean ignoreWeekends = ignoreWeekendsParam.equalsIgnoreCase(IGNORE_WEEKENDS_TRUE);

        Date evaluationDate = wbData.getWrksWorkDate();

        // only holidays should be evaluated
        if(!isHoliday(wbData, evaluationDate)) {
            if(logger.isDebugEnabled()) { logger.debug("Is not a holiday"); }
            return false;
        }

        if(   wbData.existsWorkDetailTCode(violationTimeCodes)
           || wbData.existsWorkPremiumTCode(violationTimeCodes)) {
            if(logger.isDebugEnabled()) { logger.debug("A violation code exists"); }
            return false;
        }

        // ignoreWeekends, when set to TRUE
        // changes the start date for the lookback period to the previous Friday
        // only if the holiday today is on a Monday and there is a holiday on the previous Friday
        if(ignoreWeekends) {
            if(DateHelper.dayOfWeek(evaluationDate) == 1) {
                Date fridayDate = DateHelper.addDays(evaluationDate, -3);
                if(logger.isDebugEnabled()) { logger.debug("fridayDate=[" + fridayDate + "]"); }
                if(isHoliday(wbData, fridayDate)) {
                    evaluationDate = fridayDate;
                    if(logger.isDebugEnabled()) { logger.debug("adjusted evaluationDate=[" + evaluationDate + "]"); }
                }
            }
        }

        // iterate through the previous days to see which was scheduled and not a holiday
        // and determine if they worked all of the minutes they were scheduled for
        // i.e. there are no scheduled breaks
        for(int dayIndex = 1 ; dayIndex < MAX_LOOKBACK_DAYS ; dayIndex++) {
            evaluationDate = DateHelper.addDays(evaluationDate, -1);
            if(logger.isDebugEnabled()) { logger.debug("checking evaluationDate=[" + evaluationDate + "]"); }

            // skip holidays
            if(isHoliday(wbData, evaluationDate)) {
                if(logger.isDebugEnabled()) { logger.debug("skipping holiday"); }
                continue;
            }

            // check only scheduled days
            Date shiftStartTime = wbData.getEmpskdActStartTime(evaluationDate);
            Date shiftEndTime = wbData.getEmpskdActEndTime(evaluationDate);
            int shiftMinutes = (int)DateHelper.getMinutesBetween(shiftEndTime, shiftStartTime);
            if(logger.isDebugEnabled()) { logger.debug("shiftMinutes=[" + shiftMinutes + "]"); }
            if( shiftMinutes == 0) {
                if(logger.isDebugEnabled()) { logger.debug("skipping unscheduled day"); }
                continue;
            } else {
                // check work detail minutes
                int wrkdMinutes = wbData.getMinutesWorkDetailRange(evaluationDate, evaluationDate, null, null, workedTimeCodes, true, "", true);
                if(logger.isDebugEnabled()) { logger.debug("wrkdMinutes=[" + wrkdMinutes + "]"); }

                // check work premium minutes
                int wrkpMinutes = wbData.getMinutesWorkPremiumRange(evaluationDate, evaluationDate, workedTimeCodes, true, "", true);
                if(logger.isDebugEnabled()) { logger.debug("wrkpMinutes=[" + wrkpMinutes + "]"); }

                int totalMinutes = wrkdMinutes + wrkpMinutes;
                if(logger.isDebugEnabled()) { logger.debug("totalMinutes=[" + totalMinutes + "]"); }

                return (totalMinutes >= shiftMinutes);
            }
        }
        if(logger.isDebugEnabled()) { logger.debug("falling through - no match found"); }
        return false;
    }

    /** Validates the passed parameters (cannot be empty or null), then trims them for return evaluation. */
    protected String validateParameter(Parameters parameters, String paramName) throws Exception {
        String paramValue = parameters.getParameter(paramName);
        if(paramValue == null || paramValue.trim().length() == 0) {
            throw new RuleEngineException("Empty " + paramName + " field");
        } else {
           return paramValue.trim();
        }
    }

    /** @return true iff there are any holiday overrides on the date specified. */
    protected boolean isHoliday(WBData wbData, Date evaluationDate) throws Exception {
        OverrideList holidayOvrList = wbData.getOverridesAppliedRange(evaluationDate
                , evaluationDate
                , OverrideData.HOLIDAY_TYPE_START
                , OverrideData.HOLIDAY_TYPE_END);
        if(logger.isDebugEnabled()) { logger.debug("holidayOvrList.size()=[" + holidayOvrList.size() + "]"); }
        return (holidayOvrList.size() > 0);
    }
}
