package com.wbiag.app.ta.ruleengine;

import java.sql.SQLException;
import java.util.*;

import org.apache.log4j.Logger;

import com.wbiag.app.ta.ruleengine.CDataEventPayrollSplitAdj;
import com.workbrain.app.modules.retailSchedule.model.Activity;
import com.workbrain.app.ta.db.CodeMapper;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.DBConnection;
import com.workbrain.util.*;
/**
 * Changes an on clock work detail to scheduled time code
 * When finding a schedule for a clock, it will find the schedule where
 *   - the clock falss between skd start and end time
 *   or - the clock is between lask skd end time and this skd start time
 * This logic can be changed based on clocking behavior of emps
 *
 */
public class CDataEventChangeDetailsToSkdTcode extends DataEvent {
	private static Logger logger = Logger.getLogger(CDataEventChangeDetailsToSkdTcode.class);

	public void afterApplyClocks(WBData data, DBConnection c) {
		try {
			changeDetailsToSkdCode(data);
		} catch (Exception e) {
			if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) logger.error("Error in copySchedDetailToWrkd" , e);
            throw new NestedRuntimeException("Error in copySchedDetailToWrkd", e);
		}
	}

	private void changeDetailsToSkdCode(WBData wbData)	throws SQLException {

         WorkDetailList wdl = wbData.getRuleData().getWorkDetails();
         for (int i = 0, j = wdl.size(); i < j; i++) {
             WorkDetailData wd = wbData.getRuleData().getWorkDetails().
                 getWorkDetail(i);
             //*** if this is an WRK detail created by an on clock
             if ("WRK".equals(wd.getWrkdTcodeName())
                 && checkIfCreatedByOnClock(wbData, wd.getWrkdStartTime())) {
                 if (logger.isDebugEnabled()) logger.debug("Found work detail for an on clock " + wd.getWrkdStartTime());
                 // *** find corresponding skd detail which is not a WRK schedule
                 EmployeeSchedDtlData empSkdDet = getEmpSkdData(wbData,
                     wd.getWrkdStartTime());
                 if (empSkdDet != null &&
                     !"WRK".equals(empSkdDet.getEschdTcodeName())) {
                     if (logger.isDebugEnabled()) logger.debug("Changed tcode for work detail " + wd.getWrkdStartTime());
                     wd.setTcodeId(empSkdDet.getTcodeId());
                     // *** additionalyy, store actId in a Udf for reference
                     wd.setWrkdUdf1(String.valueOf(empSkdDet.getActId()));
                 }
             }
         }
	}

    private EmployeeSchedDtlData getEmpSkdData(WBData wbData, Date dat){
        EmployeeSchedDtlData ret = null;
        EmployeeSchedDtlList skdDets = wbData.getRuleData().getEmployeeScheduleDetails(); ;
        skdDets.sort();
        Date lastSkdEnd = DateHelper.DATE_1900;
        Iterator iter = skdDets.iterator();
        while (iter.hasNext()) {
            EmployeeSchedDtlData item = (EmployeeSchedDtlData) iter.
                next();
            if (DateHelper.isBetween(dat, lastSkdEnd, item.getEschdStartTime())
                ||
                DateHelper.isBetween(dat, item.getEschdStartTime(), item.getEschdEndTime())) {
                ret = item; break;
            }
            lastSkdEnd = item.getEschdEndTime();
        }
        return ret;
    }

    private boolean checkIfCreatedByOnClock(WBData wbData, Date wdTime){
        boolean ret = false;

        List clocks = wbData.getClocks();
        if (clocks.size() == 0) {
            return false;
        }
        Iterator iter = clocks.iterator();
        while (iter.hasNext()) {
            Clock item = (Clock)iter.next();
            if (Clock.TYPE_ON == item.getClockType()
                && DateHelper.equals(wdTime , item.getClockDate())) {
                ret = true; break;
            }
        }

        return ret;
    }

}
