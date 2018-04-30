package com.wbiag.app.ta.quickrules;

import java.util.*;

import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;
/**
 * ChangeTcodeHtypeDurationRule
 * Changes time codes and hour types eligible by PARAM_TIME_CODES and PARAM_HOUR_TYPES
 * to PARAM_CHANGE_TO_TIME_CODE and PARAM_CHANGE_TO_HOUR_TYPE that are over
 * PARAM_CAP_DURATION.
 *@deprecated As of 5.0.2.0, use core classes
 */
public class ChangeTcodeHtypeDurationRule extends Rule{

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ChangeTcodeHtypeDurationRule.class);

    public static final String PARAM_TIME_CODES = "TimeCodes";
    public static final String PARAM_TIME_CODES_INCLUSIVE = "TimeCodesInclusive";
    public static final String PARAM_HOUR_TYPES = "HourTypes";
    public static final String PARAM_HOUR_TYPES_INCLUSIVE = "HourTypesInclusive";
    public static final String PARAM_CAP_DURATION = "CapDuration";
    public static final String PARAM_CAP_TIME = "CapTime";
    public static final String PARAM_VAL_OUTSIDE_SCHEDULE = "OUTSIDE_SCHEDULE";
    public static final String PARAM_VAL_INSIDE_SCHEDULE = "INSIDE_SCHEDULE";
    public static final String PARAM_VAL_CAPDURATION_FOR_EACH_DETAIL_PREFIX
        = "CAPDURATION_FOR_EACH_DETAIL";


    public static final String PARAM_CHANGE_TO_TIME_CODE = "ChangeToTimeCode";
    public static final String PARAM_CHANGE_TO_HOUR_TYPE = "ChangeToHourType";
    public final static String PARAM_PREMIUM_DETAIL = "PremiumDetail";
    public final static String PARAM_EXPRESSION_STRING = "ExpressionString";
    public static final String PARAM_APPLY_TO_DAY = "ApplyToDay";
    public static final String PARAM_VAL_APPLY_TO_DAY_CURRENT_DAY = "CurrentDay";
    public static final String PARAM_VAL_APPLY_TO_DAY_PREVIOUS_DAY = "PreviousDay";
    public static final String PARAM_VAL_APPLY_TO_DAY_NEXT_DAY = "NextDay";
    public static final String PARAM_CONDITIONSET_IS_MUTUALLY_EXCLUSIVE = "ConditionSetIsMutuallyExclusive";

    public final static String P24HOUR_DATE_FORMAT = "HH:mm";
    public final static String CAP_TIMES_DELIM = "~";

    public List getParameterInfo(DBConnection dBConnection) {
        List result = new ArrayList();

        result.add(new RuleParameterInfo(PARAM_TIME_CODES,
                                         RuleParameterInfo.STRING_TYPE, true));
        RuleParameterInfo timeCodesInclusive = new RuleParameterInfo(
            PARAM_TIME_CODES_INCLUSIVE, RuleParameterInfo.CHOICE_TYPE, true);
        timeCodesInclusive.addChoice("true");
        timeCodesInclusive.addChoice("false");
        result.add(timeCodesInclusive);
        result.add(new RuleParameterInfo(PARAM_HOUR_TYPES,
                                         RuleParameterInfo.STRING_TYPE, true));
        RuleParameterInfo hourTypesInclusive = new RuleParameterInfo(
            PARAM_HOUR_TYPES_INCLUSIVE, RuleParameterInfo.CHOICE_TYPE, true);
        hourTypesInclusive.addChoice("true");
        hourTypesInclusive.addChoice("false");
        result.add(hourTypesInclusive);
        RuleParameterInfo duration = new RuleParameterInfo(PARAM_CAP_DURATION, RuleParameterInfo.STRING_TYPE, true);
        result.add(duration);
        result.add(new RuleParameterInfo(PARAM_CAP_TIME, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_CHANGE_TO_TIME_CODE, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_CHANGE_TO_HOUR_TYPE, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_PREMIUM_DETAIL, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_EXPRESSION_STRING, RuleParameterInfo.STRING_TYPE, true));

        RuleParameterInfo applyToDay = new RuleParameterInfo(
            PARAM_APPLY_TO_DAY, RuleParameterInfo.CHOICE_TYPE, true);
        applyToDay.addChoice(PARAM_VAL_APPLY_TO_DAY_PREVIOUS_DAY);
        applyToDay.addChoice(PARAM_VAL_APPLY_TO_DAY_CURRENT_DAY);
        applyToDay.addChoice(PARAM_VAL_APPLY_TO_DAY_NEXT_DAY);
        result.add(applyToDay);

        RuleParameterInfo cset = new RuleParameterInfo(PARAM_CONDITIONSET_IS_MUTUALLY_EXCLUSIVE, RuleParameterInfo.CHOICE_TYPE, true);
        cset.addChoice("true");
        cset.addChoice("false");
        result.add(cset);

        return result;
    }


    public void execute(WBData wbData, Parameters parameters) throws Exception {
        ParametersResolved pars = getParametersResolved(parameters );
        if (pars == null) {
            if (logger.isDebugEnabled()) logger.debug("Parameters resolved to null, exiting the rule");
            return;
        }
        //Check if we should use work details or premium details
        if (WorkDetailData.DETAIL_TYPE.equals(pars.premiumDetail)) {
            processDetail(wbData , pars);
        }
        else if (WorkDetailData.PREMIUM_TYPE.equals(pars.premiumDetail)) {
            processPremium(wbData , pars);
        }
        else {
            throw new RuleEngineException("Premium details must be D or P");
        }

    }

    /**
     * Process details for parameters
     * @param wbData
     * @param pars
     * @throws Exception
     */
    protected void processDetail(WBData wbData, ParametersResolved pars) throws Exception {
        WorkDetailList wdl = wbData.getRuleData().getWorkDetails();
        if (wdl.size() == 0) {
            if (logger.isDebugEnabled()) logger.debug("No work details for ChangeTcodeHtypeDurationRule");
            return;
        }
        Date start = wbData.getRuleData().getWorkDetail(0).getWrkdStartTime();
        Date end = wbData.getRuleData().getWorkDetail(wdl.size() - 1).getWrkdEndTime();

        List durations = new ArrayList();
        if (!StringHelper.isEmpty(pars.capTime )) {
            if (PARAM_VAL_INSIDE_SCHEDULE.equals(pars.capTime)) {
                for (int k=0; k < WBData.MAXIMUM_SHIFT_COUNT ; k++) {
                    if (!wbData.getEmployeeScheduleData().retrieveShiftScheduled(k)) {
                        continue;
                    }
                    Date scheduledStart = wbData.getEmployeeScheduleData().retrieveShiftStartTime(k);
                    Date scheduledEnd = wbData.getEmployeeScheduleData().retrieveShiftEndTime(k);
                    StartEnd stEnd = new StartEnd();
                    stEnd.start = scheduledStart;
                    stEnd.end = scheduledEnd;
                    durations.add(stEnd);
                }

            }
            else if (PARAM_VAL_OUTSIDE_SCHEDULE.equals(pars.capTime)) {
                boolean firstDone = false;
                for (int k=0; k < wbData.getRuleData().getShiftsWithBreaks().size(); k++) {
                    ShiftWithBreaks thisShift = wbData.getShiftWithBreaks(k);
                    if (!thisShift.isScheduledActual()) {
                        continue;
                    }
                    if (!firstDone) {
                        ShiftWithBreaks previousShift = wbData.
                            getShiftWithBreaksBefore(k);
                        Date scheduledStart = previousShift == null ? null :
                            previousShift.getShftEndTime();
                        Date scheduledEnd = thisShift.getShftStartTime();
                        StartEnd stEnd = new StartEnd();
                        stEnd.start = scheduledStart;
                        stEnd.end = scheduledEnd;
                        durations.add(stEnd);
                        firstDone = true;
                    }
                    ShiftWithBreaks nextShift = wbData.
                        getShiftWithBreaksAfter(k);
                    Date scheduledStart = thisShift.getShftEndTime();
                    Date scheduledEnd = nextShift == null ? null :
                            nextShift.getShftStartTime();
                    StartEnd stEnd = new StartEnd();
                    stEnd.start = scheduledStart;
                    stEnd.end = scheduledEnd;
                    durations.add(stEnd);
                }
            }
            else if (pars.capTime.startsWith(PARAM_VAL_CAPDURATION_FOR_EACH_DETAIL_PREFIX)) {
                String[] items = StringHelper.detokenizeString(pars.capTime, ",") ;
                String operator = items.length > 1 ? items[1] : null;
                if (StringHelper.isEmpty(operator)) {
                    operator = RuleHelper.LESSEQ;
                }
                for (int i = 0, j = wbData.getRuleData().getWorkDetails().size(); i < j; i++) {
                    WorkDetailData wd = wbData.getRuleData().getWorkDetails().
                        getWorkDetail(i);
                    boolean isEligible = isWDEligible(wd, pars)
                        && RuleHelper.evaluate(new Integer(wd.getWrkdMinutes()),
                                               new Integer(pars.capDuration),
                                               operator);
                    if (isEligible) {
                        if (logger.isDebugEnabled()) logger.debug("Work Detail :" + wd.getWrkdStartTime() + " to " + wd.getWrkdEndTime() + " is eligible");
                        StartEnd stEnd = new StartEnd();
                        stEnd.start = wd.getWrkdStartTime() ;
                        stEnd.end = wd.getWrkdEndTime();
                        durations.add(stEnd);
                    }
                }
            }
            else {
                String[] capTimes = StringHelper.detokenizeString(pars.capTime, CAP_TIMES_DELIM);
                Date dCap = null;
                Date dCapEnd = null;
                try {
                    if (capTimes.length > 0 && !StringHelper.isEmpty(capTimes[0])) {
                        dCap = DateHelper.parseDate(capTimes[0], P24HOUR_DATE_FORMAT);
                    }
                    if (capTimes.length > 1 && !StringHelper.isEmpty(capTimes[1])) {
                        dCapEnd = DateHelper.parseDate(capTimes[1], P24HOUR_DATE_FORMAT);
                    }
                }
                catch (Exception ex) {
                    throw new RuleEngineException("Could not parse CAP_TIME:" + pars.capTime);
                }
                StartEnd stEnd = new StartEnd();
                stEnd.start = DateHelper.setTimeValues(wbData.getWrksWorkDate(), dCap);
                if (dCapEnd != null) {
                    stEnd.end = DateHelper.setTimeValues(wbData.getWrksWorkDate(),
                        dCapEnd);
                    if (stEnd.end.before(stEnd.start)) {
                        stEnd.end = DateHelper.addDays(stEnd.end , 1);
                    }
                    // *** if start/end, look at yest, tomorrow too
                    StartEnd oneDayBefore = stEnd.createWithDayOffset(-1);
                    StartEnd oneDayAfter = stEnd.createWithDayOffset(1);
                    durations.add(oneDayBefore);
                    durations.add(oneDayAfter);
                }
                durations.add(stEnd);
            }
        }
        else {
            int durMins = pars.capDuration;
            for (int i = 0, j = wbData.getRuleData().getWorkDetails().size(); i < j; i++) {
                WorkDetailData wd = wbData.getRuleData().getWorkDetails().
                    getWorkDetail(i);
                boolean isEligible = isWDEligible(wd, pars);
                if (isEligible) {
                    if (wd.getWrkdMinutes() > durMins) {
                        StartEnd stEnd = new StartEnd();
                        stEnd.start = DateHelper.addMinutes(wd.getWrkdStartTime() , durMins);
                        stEnd.end = wd.getWrkdEndTime();
                        durations.add(stEnd);
                    }
                    else {
                        durMins-= wd.getWrkdMinutes();
                    }
                }
            }
        }
        if (!StringHelper.isEmpty(pars.applyToDay)) {
            checkApplyTo(wbData, pars, durations);
        }

        Iterator iter = durations.iterator();
        while (iter.hasNext()) {
            StartEnd item = (StartEnd) iter.next();
            if (logger.isDebugEnabled())   logger.debug("Changing time code from :" + item.start + " to :" + item.end );
            setWorkDetailTcodeHtype(wbData, pars, item.start, item.end);
        }
    }

    /**
     * Assigns start/end based on ApplyToDay parameter
     * @param data
     * @param pars
     * @param durations
     */
    private void checkApplyTo(WBData data, ParametersResolved pars, List durations ) {
        Date applyToStart , applyToEnd = null;
        if (PARAM_VAL_APPLY_TO_DAY_CURRENT_DAY.equals(pars.applyToDay)) {
            applyToStart = data.getWrksWorkDate();
            applyToEnd = DateHelper.addDays(data.getWrksWorkDate() , 1);
        }
        else if (PARAM_VAL_APPLY_TO_DAY_PREVIOUS_DAY.equals(pars.applyToDay)) {
            applyToStart = DateHelper.addDays(data.getWrksWorkDate() , -1);
            applyToEnd = data.getWrksWorkDate();
        }
        else if (PARAM_VAL_APPLY_TO_DAY_NEXT_DAY.equals(pars.applyToDay)) {
            applyToStart = DateHelper.addDays(data.getWrksWorkDate() , 1);
            applyToEnd = DateHelper.addDays(data.getWrksWorkDate() , 2);
        }
        else {
            if (logger.isDebugEnabled()) logger.debug("Apply to parameter not resolved :" + pars.applyToDay + " , no action taken");
            return;
        }

        Iterator iter = durations.iterator();
        while (iter.hasNext()) {
            StartEnd item = (StartEnd) iter.next();
            if (logger.isDebugEnabled()) logger.debug("Changed duration start from : " + item);
            item.start = DateHelper.max(applyToStart , item.start);
            item.end = DateHelper.min(applyToEnd , item.end);
            if (logger.isDebugEnabled()) logger.debug("                        to  : " + item);
            if (item.end.getTime() <= item.start.getTime()) {
                iter.remove();
                if (logger.isDebugEnabled()) logger.debug("Start end removed due to ApplyToDay parameter");
            }
        }
    }

    /**
     * Process premiums for parameters
     * @param wbData
     * @param pars
     */
    protected void processPremium(WBData wbData, ParametersResolved pars) {
        WorkDetailList wdl = wbData.getRuleData().getWorkPremiums();
        for (int i = 0, j = wdl.size(); i < j; i++) {
            WorkDetailData wd = wdl.getWorkDetail(i);
            if (isWDEligible(wd, pars)
                    && pars.capDuration < wd.getWrkdMinutes()) {
                //insert new premium detail to replace the old one, if necessary
                if (pars.capDuration > 0) {
                    WorkDetailData wd2 = wd.duplicate();
                    wd2.setWrkdMinutes(pars.capDuration);
                    wdl.add(wd2);
                }
                //change old premium detail
                if (!StringHelper.isEmpty(pars.changeToTimeCode)) {
                    wd.setTcodeId(wbData.getRuleData().getCodeMapper().
                                  getTimeCodeByName(pars.changeToTimeCode).
                                  getTcodeId());
                    wd.setWrkdMinutes(wd.getWrkdMinutes() - pars.capDuration);
                }
                if (!StringHelper.isEmpty(pars.changeToHourType)) {
                    //change hour type
                    double oldRate = wd.getWrkdRate();
                    int oldHtypeId = wd.getHtypeId();
                    HourTypeData hd = wbData.getRuleData().getCodeMapper().getHourTypeByName(pars.changeToHourType);
                    HourTypeData hdOld = wbData.getRuleData().getCodeMapper().getHourTypeById(oldHtypeId);
                    wd.setHtypeId(hd.getHtypeId());
                    if (hdOld.getHtypeMultiple() != 0) {
                        wd.setWrkdRate(oldRate / hdOld.getHtypeMultiple() * hd.getHtypeMultiple());
                    }
                    else {
                        wd.setWrkdRate( wbData.getRuleData().getEmployeeData().getEmpBaseRate() * hd.getHtypeMultiple());
                    }
                }
                j = wdl.size(); //increase j since we submitted a new premium record
            }
        }//for

    }


    class StartEnd {
        Date start;
        Date end;

        public StartEnd createWithDayOffset(int offset) {
            StartEnd ret = new StartEnd ();
            if (start != null) {
                ret.start = DateHelper.addDays(start , offset);
            }
            if (end != null) {
                ret.end = DateHelper.addDays(end , offset);
            }
            return ret;
        }

        public String toString() {
            return "start=" + start + ", end=" + end;
        }
    }

    /**
     * Determines if detail is eligible
     * @param wd
     * @param pars
     * @return
     */
    protected boolean isWDEligible(WorkDetailData wd, ParametersResolved pars) {
        return RuleHelper.isCodeInList(pars.tcodes,
                                       wd.getWrkdTcodeName()) == pars.tcodesInclusive
                    && RuleHelper.isCodeInList(pars.htypes,
                                               wd.getWrkdHtypeName()) == pars.htypesInclusive
                    && wd.evaluateExpression(pars.expressionString);
    }

    private void setWorkDetailTcodeHtype(
            WBData wbData,
            ParametersResolved pars,
            Date startTime,
            Date endTime) {
        // ***** Defaults ****
        startTime = (startTime == null ? new Date(Long.MIN_VALUE) : startTime);
        endTime = (endTime == null ? new Date(Long.MAX_VALUE) : endTime);

        wbData.getRuleData().getWorkDetails().splitAt(startTime);
        wbData.getRuleData().getWorkDetails().splitAt(endTime);
        TimeCodeData tcd = wbData.getCodeMapper().getTimeCodeByName(pars.changeToTimeCode);
        int tcodeId = tcd.getTcodeId();
        for (int i = 0, j = wbData.getRuleData().getWorkDetails().size(); i < j; i++) {
            WorkDetailData wd = wbData.getRuleData().getWorkDetails().getWorkDetail(i);
            if (isWDEligible(wd, pars)
                && (!wd.getWrkdStartTime().before(startTime)) &&
                    (!wd.getWrkdEndTime().after(endTime))) {
                if (!StringHelper.isEmpty(pars.changeToTimeCode )) {
                    wd.setTcodeId(tcodeId);
                }
                if (!StringHelper.isEmpty(pars.changeToHourType )) {
                    double oldRate = wd.getWrkdRate();
                    int oldHtypeId = wd.getHtypeId();
                    HourTypeData hd = wbData.getRuleData().getCodeMapper().getHourTypeByName(pars.changeToHourType);
                    HourTypeData hdOld = wbData.getRuleData().getCodeMapper().getHourTypeById(oldHtypeId);
                    wd.setHtypeId(hd.getHtypeId());
                    if (hdOld.getHtypeMultiple() != 0) {
                        wd.setWrkdRate(oldRate / hdOld.getHtypeMultiple() * hd.getHtypeMultiple());
                    }
                    else {
                        wd.setWrkdRate( wbData.getRuleData().getEmployeeData().getEmpBaseRate() * hd.getHtypeMultiple());
                    }
                }
            }
        }
    }

    private ParametersResolved getParametersResolved(Parameters parameters) {
        ParametersResolved pars = new ParametersResolved();
        pars.tcodes =  parameters.getParameter(PARAM_TIME_CODES , null);
        pars.tcodesInclusive = Boolean.valueOf(parameters.getParameter(PARAM_TIME_CODES_INCLUSIVE, "true")).booleanValue();
        pars.htypes =  parameters.getParameter(PARAM_HOUR_TYPES , null);
        pars.htypesInclusive = Boolean.valueOf(parameters.getParameter(PARAM_HOUR_TYPES_INCLUSIVE, "true")).booleanValue();
        pars.capDuration = parameters.getIntegerParameter(PARAM_CAP_DURATION, 0);
        pars.changeToTimeCode = parameters.getParameter(PARAM_CHANGE_TO_TIME_CODE , null);
        pars.changeToHourType = parameters.getParameter(PARAM_CHANGE_TO_HOUR_TYPE , null);
        if (StringHelper.isEmpty(pars.changeToTimeCode)
            && StringHelper.isEmpty(pars.changeToHourType)) {
            throw new RuntimeException ("One of changeToTimeCode or  changeToHourType must be supplied");
        }
        pars.premiumDetail =  parameters.getParameter(PARAM_PREMIUM_DETAIL , WorkDetailData.DETAIL_TYPE);
        pars.capTime =  parameters.getParameter(PARAM_CAP_TIME , "");
        pars.expressionString =  parameters.getParameter(PARAM_EXPRESSION_STRING , null);
        pars.applyToDay = parameters.getParameter(PARAM_APPLY_TO_DAY , null);
        if (!StringHelper.isEmpty(pars.applyToDay)) {
            if (!PARAM_VAL_APPLY_TO_DAY_CURRENT_DAY.equals(pars.applyToDay)
                && !PARAM_VAL_APPLY_TO_DAY_PREVIOUS_DAY.equals(pars.applyToDay)
                && !PARAM_VAL_APPLY_TO_DAY_NEXT_DAY.equals(pars.applyToDay)) {
                throw new RuntimeException("applyToDay must be one of " + PARAM_VAL_APPLY_TO_DAY_CURRENT_DAY + ", " + PARAM_VAL_APPLY_TO_DAY_PREVIOUS_DAY + ", " + PARAM_VAL_APPLY_TO_DAY_NEXT_DAY + " when supplied");
            }
        }

        if (pars.capDuration < 0) {
            throw new RuntimeException("CAP_DURATION can't be < 0 for ChangeTcodeHtypeDurationRule");
        }

        return pars;
    }


    public class ParametersResolved {
        public String tcodes;
        public boolean tcodesInclusive;
        public String htypes;
        public boolean htypesInclusive;
        public int capDuration;
        public String changeToTimeCode;
        public String changeToHourType;
        public String premiumDetail;
        public String capTime;
        public String expressionString;
        public String applyToDay;
    }

    public String getComponentName() {
        return "WBIAG: Change Time Code Hour Type Duration Rule";
    }

}
