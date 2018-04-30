package com.wbiag.app.ta.ruleengine;

import java.util.*;

import org.apache.log4j.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.app.wbinterface.db.ImportData;
import com.workbrain.sql.*;
import com.workbrain.util.*;
/**
 * Custom event for CDataEventMoveClockDataSkd
 * Will move the ON clock to the closest schedule work date
 */
public class CDataEventMoveClockDateSkd extends DataEvent {

    private static Logger logger = org.apache.log4j.Logger.getLogger(CDataEventMoveClockDateSkd.class);

    public int afterProcessOneClockAlterWrksId(Clock clock , int empId, 
            DBConnection conn , ImportData data, int wrksId){
        int ret = wrksId;
        try {
            ret = findClosestSchedule(clock, empId, conn, wrksId);
        }
        catch (Exception e){
            if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) logger.error("Error in CDataEventMoveClockDataSkd" , e);
            throw new NestedRuntimeException("Error in CDataEventMoveClockDataSkd", e);            
        }
        
        return ret; 
    }
    
    protected int findClosestSchedule(Clock clock , int empId, 
            DBConnection conn , int wrksId)  throws Exception {
        int ret = wrksId;
        if (!clock.isClockOn()) {
            return ret;
        }
        Date clkDate = DateHelper.truncateToDays(clock.getClockDate());
        Date dayBefore = DateHelper.addDays(clkDate, -1);
        Date dayAfter = DateHelper.addDays(clkDate, 1);        
        EmployeeScheduleAccess esa = new EmployeeScheduleAccess(conn ,
                CodeMapper.createCodeMapper(conn));
        List skds = esa.loadByDateRange(empId, dayBefore, dayAfter);
        long diff = Long.MAX_VALUE;
        EmployeeScheduleData dayOfClockSkd = null; 
        Iterator iter = skds.iterator();
        while (iter.hasNext()) {
            EmployeeScheduleData item = (EmployeeScheduleData)iter.next();
            if (!item.isEmployeeScheduledActual()) {
                if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG))  {logger.debug("EmpId :" + empId + " is not scheduled for :" + item.getWorkDate()); }
            }
            long diffItem = Math.abs(DateHelper.getMinutesBetween(clock.getClockDate(), 
                    item.getEmpskdActStartTime()));
            if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG))  {logger.debug("Difference in minutes :" + diffItem + " for clock :" + clock.getClockDate() + " and schedule on :" + item.getEmpskdActStartTime()); }            
            if (diffItem < diff) {
                dayOfClockSkd = item;
                diff = diffItem;
            }
        }
        if (dayOfClockSkd != null) {
            WorkSummaryData wsd = new WorkSummaryAccess(conn).loadByEmpIdAndDate(empId, 
                    new java.sql.Date(dayOfClockSkd.getWorkDate().getTime()));
            if (wsd != null) {
                if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG))  {logger.debug("Returning wrksId :" + wsd.getWrksId() + " for date :" + wsd.getWrksWorkDate()); }                
                ret = wsd.getWrksId(); 
            }
        }
        return ret;
    }

}
