package com.wbiag.app.ta.ruleengine;

import com.workbrain.app.ta.ruleengine.DataEvent;
import com.workbrain.app.ta.model.*;
import com.workbrain.sql.DBConnection;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.app.wbinterface.schedulein.*;
import com.workbrain.util.*;
import java.sql.SQLException;

/**
 *
 * This data event will convert an override to applied and recalc
 * the timesheet for the date range of the override.  This occurs once an
 * override has been created (i.e. from Timesheet w/o need to resubmit)
 *
 * @deprecated Core as of 4.1 FP14 with registry paramter TS_APPLY_OVR_ON_INSERT
 *
 */
public class AutoSubmitDataEvent extends DataEvent {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(AutoSubmitDataEvent.class);

    public AutoSubmitDataEvent() {
    }

    /**
     * This method determines if the inserted Override is of a certain type
     * (refer to the list in the code) and not created by interface processes
     * like Schedule Interface.  If so, the method will return true.  <p>
     * This method is used to help determine if a recalc is to be performed upon inserting an override.
     *
     * @param od
     * @return
     */
    protected boolean isRecalcRequired(OverrideData od ) {
        boolean ret = false;

        //*** add more checks here to make sure only overrides from timesheets are recalced
        ret = !ScheduleWorkDetailTransaction.ovrWbuName.equalsIgnoreCase(od.getWbuName())
            && !ScheduleTransaction.ovrWbuName.equalsIgnoreCase(od.getWbuName())
            && !WorkDetailTransaction.ovrWbuName.equalsIgnoreCase(od.getWbuName())
            && isEligibleOverride(od);
        return ret;
    }

    protected boolean isEligibleOverride(OverrideData od) {
        return
            (od.getOvrtypId() >= OverrideData.WORK_DETAIL_TYPE_START &&
             od.getOvrtypId() <= OverrideData.WORK_DETAIL_TYPE_END) ||
// run recalc on Work Premium Override
            (od.getOvrtypId() >= OverrideData.PRECALC_WORK_PREMIUM_TYPE_START &&
             od.getOvrtypId() <= OverrideData.PRECALC_WORK_PREMIUM_TYPE_END) ||
// run recalc on Schedule Override
            (od.getOvrtypId() >= OverrideData.SCHEDULE_TYPE_START &&
             od.getOvrtypId() <= OverrideData.SCHEDULE_TYPE_END) ||
// run LTA overrides
            (od.getOvrtypId() >= OverrideData.LTA_TYPE_START &&
             od.getOvrtypId() <= OverrideData.LTA_TYPE_END) ||
// run Holiday overrides
            (od.getOvrtypId() >= OverrideData.HOLIDAY_TYPE_START &&
             od.getOvrtypId() <= OverrideData.HOLIDAY_TYPE_END) ||
// run Postcalc Work Premiums overrides
            (od.getOvrtypId() >= OverrideData.POSTCALC_WORK_PREMIUM_TYPE_START &&
             od.getOvrtypId() <= OverrideData.POSTCALC_WORK_PREMIUM_TYPE_START) ||
// run Post calc Work Detail overrides
            (od.getOvrtypId() >= OverrideData.POSTCALC_WORKDETAIL_TYPE_START &&
             od.getOvrtypId() <= OverrideData.POSTCALC_WORKDETAIL_TYPE_END) ||
// run Manpower overrides
            (od.getOvrtypId() >= OverrideData.MANPOWER_TYPE_START &&
             od.getOvrtypId() <= OverrideData.MANPOWER_TYPE_END) ||
// run Timesheet overrides
            (od.getOvrtypId() >= OverrideData.TIMESHEET_TYPE_START &&
             od.getOvrtypId() <= OverrideData.TIMESHEET_TYPE_END) ||
// run Standby overrides
            (od.getOvrtypId() >= OverrideData.WORKDETAIL_STANDBY_TYPE_START &&
             od.getOvrtypId() <= OverrideData.WORKDETAIL_STANDBY_TYPE_END);
    }

    /**
     * Re-execute recalc on timesheet. Ignore if the override is an employee override
     *
     * @param od Incoming override record after it has been saved to the database
     * @param c Database connection
     */
    public void afterOverrideInsert(OverrideData od, DBConnection c) throws RuleEngineException{

        if (isRecalcRequired(od))
            try {
                RuleEngine.runCalcGroup(c, od.getEmpId(),
                                      od.getOvrStartDate(),
                                      od.getOvrStartDate(), false);
          }
          catch (SQLException se) {
              if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) logger.error("Could not recalc: ", se);
              throw new NestedRuntimeException("Could not recalc", se);
          }
    }
}