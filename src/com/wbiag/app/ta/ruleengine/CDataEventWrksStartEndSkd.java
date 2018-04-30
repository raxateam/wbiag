package com.wbiag.app.ta.ruleengine;

import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;
/**
 * Dataevent to set wrks times based on employee schedule
 */
public class CDataEventWrksStartEndSkd extends DataEvent {

    public void beforeSave(WBData data, DBConnection c) {
        if (data.getEmployeeScheduleData().isEmployeeScheduledActual()) {
            if (data.getScheduledShiftCount() == 1) {
                data.setWrksStartTime(data.getEmployeeScheduleData().getEmpskdActStartTime());
                data.setWrksEndTime(data.getEmployeeScheduleData().getEmpskdActEndTime());
            }
            else if (data.getScheduledShiftCount() > 1) {
                data.setWrksStartTime(retrieveMinActStartTime(data.getEmployeeScheduleData()));
                data.setWrksEndTime(retrieveMaxActEndTime(data.getEmployeeScheduleData()));
            }
        }
        else {
            data.setWrksStartTime(DateHelper.DATE_1900);
            data.setWrksEndTime(DateHelper.DATE_1900);
        }
    }

    /**
     * Retrieves the minumum actual start time of the schedule. The core one has bug with TT45355
     *
     * @return Datetime object
     */
    private java.util.Date retrieveMinActStartTime(EmployeeScheduleData esd) {
        java.util.Date minStart = new java.util.Date(esd.getEmpskdActStartTime().getTime());
        for (int i=1 ; i <=4 ; i++) {
            if (esd.retrieveShiftScheduled(i)) {
                java.util.Date date = esd.retrieveShiftStartTime(i);
                if (date != null && date.getTime() < minStart.getTime()) {
                    minStart.setTime(date.getTime());
                }
            }
        }
        return minStart;
    }

    /**
     * Retrieves the maximum actual end time of the schedule. . The core one has bug with TT45355
     *
     * @return Datetime object
     */
    public java.util.Date retrieveMaxActEndTime(EmployeeScheduleData esd) {
        java.util.Date maxEnd = new java.util.Date(esd.getEmpskdActEndTime().getTime());
        for (int i=1 ; i <=4 ; i++) {
            if (esd.retrieveShiftScheduled(i)) {
                java.util.Date date = esd.retrieveShiftEndTime(i);
                if (date != null && date.getTime() > maxEnd.getTime()) {
                    maxEnd.setTime(date.getTime());
                }
            }
        }
        return maxEnd;
    }

}

