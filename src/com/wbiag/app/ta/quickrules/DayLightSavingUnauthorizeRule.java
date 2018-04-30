package com.wbiag.app.ta.quickrules;

import java.util.*;

import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.DBConnection;
import com.workbrain.util.*;

/**
 * Unuthorization rule for Days where Daylight Saving Time changes
 */
public class DayLightSavingUnauthorizeRule extends Rule {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(DayLightSavingUnauthorizeRule.class);

    public final static String UNAUTH_TYPE_SCHEDULE_OVERLAPS_DST_CHANGE = "ScheduleOverlapsDstChange";
    public final static String UNAUTH_TYPE_NO_OF_CLOCKS_OVERLAPPING_DST_CHANGE = "NoOfClocksOverlappingDstChange";
    public final static String PARAM_UNAUTHORIZE_IF_SCHEDULE_OVERLAPS_DST_CHANGE
        = "unauthorizeIfScheduleOverlapsDstChange";
    public final static String PARAM_UNAUTHORIZE_IF_NO_OF_CLOCKS_OVERLAPPING_DST_CHANGE
        = "unauthorizeIfNoOfClocksOverlappingDstChange";

    //private WBData wbData;

    public List getParameterInfo(DBConnection conn) {
        List result = new ArrayList();
        result.add(new RuleParameterInfo(PARAM_UNAUTHORIZE_IF_SCHEDULE_OVERLAPS_DST_CHANGE, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_UNAUTHORIZE_IF_NO_OF_CLOCKS_OVERLAPPING_DST_CHANGE, RuleParameterInfo.INT_TYPE, true));
        return result;
    }


    public void execute(WBData wbData, Parameters parameters) throws Exception {

        boolean unauthorizeIfScheduleOverlapsDstChange =
            Boolean.valueOf(parameters.getParameter(PARAM_UNAUTHORIZE_IF_SCHEDULE_OVERLAPS_DST_CHANGE , "false")).booleanValue();
        int unauthorizeIfNoOfClocksOverlappingDstChange =
            parameters.getIntegerParameter(PARAM_UNAUTHORIZE_IF_NO_OF_CLOCKS_OVERLAPPING_DST_CHANGE , 0);

        // *** no need to do it if not Spring forward ot fall back
        if (!isSpringForwardDate(wbData.getWrksWorkDate())
            && !isFallBackDate(wbData.getWrksWorkDate())) {
            return;
        }

        if (unauthorizeIfScheduleOverlapsDstChange) {
            unauthorizeForSchedule(wbData);
        }

        if (unauthorizeIfNoOfClocksOverlappingDstChange > 0) {
            unauthorizeForClocks(wbData, unauthorizeIfNoOfClocksOverlappingDstChange);
        }

    }

    protected void unauthorizeForSchedule(WBData wbData) {
        EmployeeScheduleData esd = wbData.getEmployeeScheduleData();
        if (!esd.isEmployeeScheduledActual()) {
            return;
        }
        for (int i = 0 ; i < wbData.MAXIMUM_SHIFT_COUNT   ; i++ ) {
            if (!esd.retrieveShiftScheduled(i)) continue;
            boolean overlaps =
                (isInDaylightSavingTime(esd.retrieveShiftStartTime(i))
                 && !isInDaylightSavingTime(esd.retrieveShiftEndTime(i)))
                ||
                (!isInDaylightSavingTime(esd.retrieveShiftStartTime(i))
                 && isInDaylightSavingTime(esd.retrieveShiftEndTime(i)));

            if (overlaps) {
                wbData.getRuleData().getWorkSummary().setWrksAuthorized("N");
                wbData.getRuleData().getWorkSummary().setWrksMessages(UNAUTH_TYPE_SCHEDULE_OVERLAPS_DST_CHANGE);
                if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) logger.debug("Uanuthorized for DST schedule overlapping");
            }
        }
    }

    protected void unauthorizeForClocks(WBData wbData, int clkCount) {
        if (wbData.getClocks().size() == 0) {
            return;
        }
        boolean existsDST = false , existsST = false;
        Iterator it = wbData.getClocks().iterator();
        int cnt = 0;
        while (it.hasNext()) {
            Clock clk = (Clock) it.next();
            if (isInDaylightSavingTime(clk.getClockDate())) {
                existsDST = true;
            }
            else if (!isInDaylightSavingTime(clk.getClockDate())) {
                existsST = true;
            }
        }

        if (existsDST && existsST
            && wbData.getClocks().size() > clkCount) {
            wbData.getRuleData().getWorkSummary().setWrksAuthorized("N");
            wbData.getRuleData().getWorkSummary().setWrksMessages(UNAUTH_TYPE_NO_OF_CLOCKS_OVERLAPPING_DST_CHANGE);
            if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) logger.debug("Uanuthorized for clocks overlapping");
        }
    }

    public String getComponentName() {
        return "WBIAG: Unauthorize Day Light Saving";
    }

    public String getComponentUI() {
        return null;
    }

    public boolean conditionSetExecutionIsMutuallyExclusive() {
        return true;
    }

    public List getSuitableConditions() {
        List list = new ArrayList();
        list.add(new com.workbrain.app.ta.conditions.AlwaysTrueCondition());
        return list;
    }

    /**
     * Returns whether given date is in DaylightSavingTime.
     * If the timezone does not observe daylight saving, returns false.
     *
     * @param d Date
     * @return boolean
     */
    private boolean isInDaylightSavingTime(Date d) {
        TimeZone dateTz = TimeZone.getDefault();
        if (!dateTz.useDaylightTime()) {
            return false;
        }
        if (dateTz != null) {
            return dateTz.inDaylightTime(d);
        }
        return false;
    }

    /**
     * Returns whether given date is the date when spring forward adjustment is made.
     * Looking at 3 days ahead because work details of a DST work summary can be observed in -1,+1 days
     *
     * @param d Date
     * @return
     */
    private boolean isSpringForwardDate(Date d) {
        TimeZone dateTz = TimeZone.getDefault();
        if (!dateTz.useDaylightTime()) {
            return false;
        }
        return isInDaylightSavingTime(DateHelper.addDays(d , 3));
    }

    /**
     * Returns whether given date is the date when fall back adjustment is made.
     * Looking at 3 days ahead because work details of a DST work summary can be observed in -1,+1 days
     *
     * @param d Date
     * @return
     */
    private boolean isFallBackDate(Date d) {
        TimeZone dateTz = TimeZone.getDefault();
        if (!dateTz.useDaylightTime()) {
            return false;
        }
        return !isInDaylightSavingTime(DateHelper.addDays(d , 3));
    }


}



