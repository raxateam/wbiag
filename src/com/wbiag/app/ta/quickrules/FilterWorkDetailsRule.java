package com.wbiag.app.ta.quickrules;

import java.util.*;

import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;
/**
 * FilterWorkDetailsRule
 *
 * Filters work details based before a rule/condition is executed so that rule only looks at certaing work details
 *     Filtering is done based on
 *    - given timecodes/hourtypes,
 *    - expression string (for work detail attributes i.e wrkdJobName=JANITOR)
 *    - and CapDuration/Time settings to filter to certain time period
 *          i.e capTime could be
 *         - INSIDE_SCHEDULE, OUTSIDE_SCHEDULE
 *         - CAPDURATION_FOR_EACH_DETAIL where capDuration determines eligible records
 *          CapDuration=240, CapTime=CAPDURATION_FOR_EACH_DETAIL,<= means records under 240 minutes are eligible
 *         - or between certain timeframes defined by capTime
 *          CapTime=17:00~19:00 means records only between 17 and 19 are eligible
 * Once this  FilterWorkDetailsRule is used, UnFilterWorkDetailsRule needs to be used to rollback filtering
 * or the results will be unpredictable. So the setup will be
 *    Conditions
 *        FilterWorkDetailsRule
 *        Some Rule/Condition
 *        UnFilterWorkDetailsRule
 *
 */
public class FilterWorkDetailsRule extends Rule{

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(FilterWorkDetailsRule.class);

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


    public final static String PARAM_EXPRESSION_STRING = "ExpressionString";

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
        result.add(new RuleParameterInfo(PARAM_EXPRESSION_STRING, RuleParameterInfo.STRING_TYPE, true));

        return result;
    }


    public void execute(WBData wbData, Parameters parameters) throws Exception {
        ParametersResolved pars = getParametersResolved(parameters);
        if (pars == null) {
            return;
        }
        Map wrk1 = getWorkDetailsPluckedMap(wbData);
        WorkDetailList plucked = getWorkDetailsPlucked(wbData);
        processDetail(wbData , pars, plucked);
    }

    private ParametersResolved getParametersResolved(Parameters parameters) {
        ParametersResolved pars = new ParametersResolved();
        pars.tcodes =  parameters.getParameter(PARAM_TIME_CODES , null);
        pars.tcodesInclusive = Boolean.valueOf(parameters.getParameter(PARAM_TIME_CODES_INCLUSIVE, "true")).booleanValue();
        pars.htypes =  parameters.getParameter(PARAM_HOUR_TYPES , null);
        pars.htypesInclusive = Boolean.valueOf(parameters.getParameter(PARAM_HOUR_TYPES_INCLUSIVE, "true")).booleanValue();
        pars.capDuration = parameters.getIntegerParameter(PARAM_CAP_DURATION, 0);
        pars.capTime =  parameters.getParameter(PARAM_CAP_TIME , "");
        pars.expressionString =  parameters.getParameter(PARAM_EXPRESSION_STRING , null);

        if (pars.capDuration < 0) {
            if (logger.isDebugEnabled()) logger.debug("CAP_DURATION was < 0 for FilterWorkDetailsRule");
            return null;
        }

        return pars;
    }

    public class ParametersResolved {
        public String tcodes;
        public boolean tcodesInclusive;
        public String htypes;
        public boolean htypesInclusive;
        public int capDuration;
        public String premiumDetail;
        public String capTime;
        public String expressionString;
    }

    protected void processDetail(WBData wbData, ParametersResolved pars, WorkDetailList plucked) throws Exception {
        WorkDetailList wdl = wbData.getRuleData().getWorkDetails();
        if (wdl.size() == 0) {
            if (logger.isDebugEnabled()) logger.debug("No work details for FilterWorkDetailsRule");
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
                    if (logger.isDebugEnabled()) logger.debug("Work Detail :" + wd.getWrkdStartTime() + " to " + wd.getWrkdEndTime() + " is eligible");
                    StartEnd stEnd = new StartEnd();
                    stEnd.start = wd.getWrkdStartTime() ;
                    stEnd.end = wd.getWrkdEndTime();
                    durations.add(stEnd);
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

        Iterator iter = durations.iterator();
        while (iter.hasNext()) {
            StartEnd item = (StartEnd) iter.next();
            if (logger.isDebugEnabled())   logger.debug("duration eligible :" + item.start + " to :" + item.end );
            wbData.getRuleData().getWorkDetails().splitAt(item.start);
            wbData.getRuleData().getWorkDetails().splitAt(item.end);
        }

        for (int i =  wbData.getRuleData().getWorkDetailCount() - 1; i >= 0; i--) {
            WorkDetailData wd = wbData.getRuleData().getWorkDetails().getWorkDetail(i);
            if (!(isWDEligible(wd, pars, durations))) {
                if (logger.isDebugEnabled())   logger.debug("plucking wd :" + wd.getWrkdStartTime() + " to :" + wd.getWrkdEndTime() );
                pluck(wbData , i, plucked);
            }
        }
    }

    public String getComponentName() {
        return "WBIAG: Filter Work Details Rule";
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
    }

    private boolean isWDEligible(WorkDetailData wd, ParametersResolved pars,
                                 List durations) {
        boolean ret = true;

        boolean eligibleDuration = false;
        Iterator iter = durations.iterator();
        while (iter.hasNext()) {
            StartEnd item = (StartEnd) iter.next();
            if (wd.getWrkdStartTime().compareTo(item.start) >= 0
                &&  (wd.getWrkdEndTime().compareTo(item.end) <= 0)) {
                eligibleDuration = true;
                break;
            }

        }

        return eligibleDuration
            && RuleHelper.isCodeInList(pars.tcodes,
                                       wd.getWrkdTcodeName()) == pars.tcodesInclusive
            && RuleHelper.isCodeInList(pars.htypes,
                                       wd.getWrkdHtypeName()) == pars.htypesInclusive
            && wd.evaluateExpression(pars.expressionString);
    }




    /**
     *  Removes a work detail record at the specified index from the work details
     *  list and shifts all the following records up by the amount of worked time
     *  that the removed record used to hold. The removed record is placed in the
     *  "plucked" list so it can be inserted back later.
     *
     *@param  in_Index  the index of the record in the work details list
     */
    private void pluck(WBData wbData , int in_Index, WorkDetailList plucked) {
//        WorkDetailList plucked = getWorkDetailsPlucked(wbData);

        if (wbData.getRuleData().getWorkDetail(in_Index) != null) {
            long shiftBy = wbData.getRuleData().getWorkDetail(in_Index).getWrkdEndTime().getTime() -
                    wbData.getRuleData().getWorkDetail(in_Index).getWrkdStartTime().getTime();

            plucked.add(wbData.getRuleData().getWorkDetail(in_Index));
            wbData.getRuleData().getWorkDetails().remove(in_Index);
            for (int i = in_Index, j = wbData.getRuleData().getWorkDetailCount(); i < j; i++) {
                wbData.getRuleData().getWorkDetail(i).setWrkdStartTime(new java.util.Date(
                        wbData.getRuleData().getWorkDetail(i).getWrkdStartTime().getTime() - shiftBy
                        ));
                wbData.getRuleData().getWorkDetail(i).setWrkdEndTime(new java.util.Date(
                        wbData.getRuleData().getWorkDetail(i).getWrkdEndTime().getTime() - shiftBy
                        ));
            }
        }
    }


    private WorkDetailList getWorkDetailsPlucked(WBData wbData) {

        EmployeeIdAndDate ed = new EmployeeIdAndDate(wbData.getEmpId() , wbData.getWrksWorkDate());
        WorkDetailList plucked = new WorkDetailList();

        Map wrk = getWorkDetailsPluckedMap(wbData);
        wrk.put(ed , plucked);


        return plucked;
    }

    private Map getWorkDetailsPluckedMap(WBData wbData ) {
        Hashtable entity = wbData.getRuleData().getCalcDataCache().getEntityCache();

        Map wrk = (Map)entity.get("WORK_DETAIL_PLUCKED");
        if (wrk == null) {
            wrk = new HashMap();
            entity.put("WORK_DETAIL_PLUCKED", wrk);
        }
        return wrk;
    }


}
